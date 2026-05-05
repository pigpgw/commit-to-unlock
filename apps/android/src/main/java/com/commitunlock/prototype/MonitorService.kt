package com.commitunlock.prototype

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import java.time.Instant
import java.time.LocalDate

class MonitorService : Service() {
    private lateinit var creditStore: CreditStore
    private lateinit var dogfoodEventStore: DogfoodEventStore
    private lateinit var emergencyUnlockStore: EmergencyUnlockStore
    private lateinit var foregroundReader: ForegroundAppReader
    private lateinit var monitorStateStore: MonitorStateStore
    private lateinit var policyStore: PolicyStore
    private lateinit var overlay: BlockOverlay
    private val handler = Handler(Looper.getMainLooper())
    private var lastForegroundPackage: String? = null
    private var lastResolvedForegroundPackage: String? = null
    private var lastPolicyDecisionKey: String? = null
    private var showingBlockedPackage: String? = null
    private var showingStrictMode: Boolean? = null
    private var showingReason: PolicyDecisionReason? = null
    private var lastHeartbeatDay: String? = null
    private var activeSpendPackage: String? = null
    private var lastSpendTickMs: Long? = null
    private var spendAccumulatorMs: Long = 0L
    private var lastHeartbeatWriteMs: Long = 0L
    private var lastPermissionMissingKey: String? = null
    private var lastTargetMatchedKey: String? = null

    private val pollRunnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, poll())
        }
    }

    override fun onCreate() {
        super.onCreate()
        creditStore = CreditStore(this)
        dogfoodEventStore = DogfoodEventStore(this)
        emergencyUnlockStore = EmergencyUnlockStore(this)
        foregroundReader = ForegroundAppReader(this)
        monitorStateStore = MonitorStateStore(this)
        policyStore = PolicyStore(this)
        overlay = BlockOverlay(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification("Monitoring selected apps"))
        monitorStateStore.setDesiredRunning(true)
        recordHeartbeat(force = true)
        dogfoodEventStore.record("monitor_started")
        handler.post(pollRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        hideOverlay("monitor_stopped")
        monitorStateStore.clearHeartbeat()
        dogfoodEventStore.record("monitor_stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun poll(): Long {
        recordHeartbeat()

        val usageAccessGranted = PermissionChecks.hasUsageAccess(this)
        if (!usageAccessGranted) {
            recordPermissionMissing("usage_access")
            stopSpendSession("usage_access_missing")
            hideOverlay("usage_access_missing")
            return MonitorPollCadence.nextDelayMillis(
                permissionsReady = false,
                hasTargets = true,
                deviceInteractive = true
            )
        }

        val overlayGranted = PermissionChecks.canDrawOverlays(this)
        if (!overlayGranted) {
            recordPermissionMissing("overlay")
            stopSpendSession("overlay_permission_missing")
            hideOverlay("overlay_permission_missing")
            return MonitorPollCadence.nextDelayMillis(
                permissionsReady = false,
                hasTargets = true,
                deviceInteractive = true
            )
        }
        lastPermissionMissingKey = null

        var state = creditStore.read()
        if (state.blockedTargets.isEmpty()) {
            stopSpendSession(
                reason = "no_targets",
                clearAccumulator = state.remainingMinutes <= 0
            )
            hideOverlay("no_targets")
            lastTargetMatchedKey = null
            return MonitorPollCadence.nextDelayMillis(
                permissionsReady = true,
                hasTargets = false,
                deviceInteractive = true
            )
        }

        val deviceInteractive = isDeviceInteractive()
        if (!deviceInteractive) {
            stopSpendSession(
                reason = "device_not_interactive",
                clearAccumulator = state.remainingMinutes <= 0
            )
            hideOverlay("device_not_interactive")
            lastTargetMatchedKey = null
            return MonitorPollCadence.nextDelayMillis(
                permissionsReady = true,
                hasTargets = true,
                deviceInteractive = false
            )
        }

        val policyState = policyStore.read()
        val activeUnlocks = emergencyUnlockStore.active()
        val rawForegroundPackage = foregroundReader.currentForegroundPackage()
        val foregroundPackage = ForegroundPackageResolver.resolveForPolicy(
            rawForegroundPackage = rawForegroundPackage,
            ownPackage = packageName,
            showingBlockedPackage = showingBlockedPackage,
            lastResolvedPackage = lastResolvedForegroundPackage
        )
        lastResolvedForegroundPackage = foregroundPackage

        if (foregroundPackage != null && foregroundPackage != lastForegroundPackage) {
            lastForegroundPackage = foregroundPackage
            dogfoodEventStore.recordStructured(
                type = "foreground_changed",
                target = foregroundPackage
            )
        }

        var decision = evaluatePolicy(foregroundPackage, state, policyState, activeUnlocks)
        recordPolicyDecision(decision, state.remainingMinutes)

        if (decision.shouldSpendCredit && foregroundPackage != null) {
            state = accrueSpend(foregroundPackage, state)
            decision = evaluatePolicy(foregroundPackage, state, policyState, activeUnlocks)
            recordPolicyDecision(decision, state.remainingMinutes)
        } else {
            stopSpendSession(
                reason = decision.reason.code,
                clearAccumulator = state.remainingMinutes <= 0
            )
        }

        if (!decision.allowed) {
            val blockedPackage = decision.matchedTarget ?: foregroundPackage
            if (blockedPackage == null) {
                hideOverlay("missing_target")
                return MonitorPollCadence.nextDelayMillis(
                    permissionsReady = true,
                    hasTargets = state.blockedTargets.isNotEmpty(),
                    deviceInteractive = true
                )
            }
            recordTargetMatched(blockedPackage, decision.reason, state.remainingMinutes)
            showOverlay(blockedPackage, state.strictMode, decision.reason, state.remainingMinutes)
        } else {
            lastTargetMatchedKey = null
            hideOverlay(decision.reason.code)
        }

        return MonitorPollCadence.nextDelayMillis(
            permissionsReady = true,
            hasTargets = state.blockedTargets.isNotEmpty(),
            deviceInteractive = true
        )
    }

    private fun recordHeartbeat(force: Boolean = false) {
        val nowMs = System.currentTimeMillis()
        if (!force && nowMs - lastHeartbeatWriteMs < HEARTBEAT_WRITE_INTERVAL_MS) return

        lastHeartbeatWriteMs = nowMs
        monitorStateStore.recordHeartbeat(nowMs)
        val today = LocalDate.now().toString()
        if (lastHeartbeatDay == today) return

        lastHeartbeatDay = today
        dogfoodEventStore.record("monitor_heartbeat", "date=$today")
    }

    private fun recordPermissionMissing(permissionKey: String) {
        if (permissionKey == lastPermissionMissingKey) return

        lastPermissionMissingKey = permissionKey
        dogfoodEventStore.record("permission_missing", permissionKey)
    }

    private fun accrueSpend(foregroundPackage: String, state: CreditState): CreditState {
        val nowMs = SystemClock.elapsedRealtime()
        if (activeSpendPackage != foregroundPackage) {
            stopSpendSession("target_changed")
            activeSpendPackage = foregroundPackage
            lastSpendTickMs = nowMs
            dogfoodEventStore.recordStructured(
                type = "target_use_started",
                target = foregroundPackage,
                policyReason = PolicyDecisionReason.CREDIT_AVAILABLE.code,
                creditRemaining = state.remainingMinutes
            )
            return state
        }

        val lastTickMs = lastSpendTickMs ?: nowMs
        val elapsedMs = (nowMs - lastTickMs).coerceAtLeast(0L)
        lastSpendTickMs = nowMs
        spendAccumulatorMs += elapsedMs

        var updatedState = state
        while (spendAccumulatorMs >= CREDIT_SPEND_INTERVAL_MS && updatedState.remainingMinutes > 0) {
            spendAccumulatorMs -= CREDIT_SPEND_INTERVAL_MS
            creditStore.spendMinute()
            updatedState = creditStore.read()
            dogfoodEventStore.recordStructured(
                type = "credit_auto_spent",
                target = foregroundPackage,
                policyReason = PolicyDecisionReason.CREDIT_AVAILABLE.code,
                creditRemaining = updatedState.remainingMinutes,
                detail = "minutes=1"
            )
        }

        if (updatedState.remainingMinutes <= 0) {
            stopSpendSession("credit_depleted", clearAccumulator = true)
        }

        return updatedState
    }

    private fun stopSpendSession(reason: String, clearAccumulator: Boolean = false) {
        val packageName = activeSpendPackage ?: return
        dogfoodEventStore.recordStructured(
            type = "target_use_stopped",
            target = packageName,
            policyReason = reason,
            detail = "pending_ms=$spendAccumulatorMs"
        )
        activeSpendPackage = null
        lastSpendTickMs = null
        if (clearAccumulator) {
            spendAccumulatorMs = 0L
        }
    }

    private fun isDeviceInteractive(): Boolean {
        val powerManager = getSystemService(PowerManager::class.java)
        return powerManager.isInteractive
    }

    private fun evaluatePolicy(
        foregroundPackage: String?,
        state: CreditState,
        policyState: PolicyState,
        activeUnlocks: List<EmergencyUnlock>
    ): PolicyDecision {
        return PolicyDecisionEngine.evaluate(
            PolicyDecisionInput(
                currentPackage = foregroundPackage,
                ownPackage = packageName,
                now = Instant.now(),
                creditState = state,
                policyState = policyState,
                activeEmergencyUnlocks = activeUnlocks,
                isPublicHoliday = false
            )
        )
    }

    private fun recordPolicyDecision(decision: PolicyDecision, creditRemaining: Int) {
        val key = listOf(
            decision.matchedTarget.orEmpty(),
            decision.reason.code,
            decision.allowed.toString(),
            decision.shouldSpendCredit.toString(),
            decision.activeEmergencyUnlockId.orEmpty()
        ).joinToString("|")

        if (key == lastPolicyDecisionKey) return
        lastPolicyDecisionKey = key
        dogfoodEventStore.recordStructured(
            type = if (decision.allowed) "policy_allowed" else "policy_blocked",
            target = decision.matchedTarget,
            policyReason = decision.reason.code,
            creditRemaining = creditRemaining,
            detail = "spend=${decision.shouldSpendCredit}"
        )
    }

    private fun recordTargetMatched(
        blockedPackage: String,
        reason: PolicyDecisionReason,
        creditRemaining: Int
    ) {
        val key = "$blockedPackage|${reason.code}|$creditRemaining"
        if (key == lastTargetMatchedKey) return

        lastTargetMatchedKey = key
        dogfoodEventStore.recordStructured(
            type = "target_matched",
            target = blockedPackage,
            policyReason = reason.code,
            creditRemaining = creditRemaining
        )
    }

    private fun showOverlay(
        foregroundPackage: String,
        strictMode: Boolean,
        reason: PolicyDecisionReason,
        creditRemaining: Int
    ) {
        if (
            showingBlockedPackage == foregroundPackage &&
            showingStrictMode == strictMode &&
            showingReason == reason
        ) return

        dogfoodEventStore.recordStructured(
            type = "blocked_attempt",
            target = foregroundPackage,
            policyReason = reason.code,
            creditRemaining = creditRemaining
        )
        overlay.hide()
        val shown = overlay.show(
            packageName = foregroundPackage,
            strictMode = strictMode,
            reasonCode = reason.code,
            onOpenApp = {
                dogfoodEventStore.recordStructured(
                    type = "overlay_open_app",
                    target = foregroundPackage,
                    policyReason = reason.code,
                    creditRemaining = creditStore.read().remainingMinutes
                )
                hideOverlay("open_app")
                openMainActivity()
            },
            onAddCredit = {
                creditStore.addMinutes(5)
                dogfoodEventStore.recordStructured(
                    type = "overlay_add_credit",
                    target = foregroundPackage,
                    policyReason = reason.code,
                    creditRemaining = creditStore.read().remainingMinutes,
                    detail = "minutes=5"
                )
                hideOverlay("credit_added")
            }
        )
        if (!shown) {
            dogfoodEventStore.recordStructured(
                type = "overlay_show_failed",
                target = foregroundPackage,
                policyReason = reason.code,
                creditRemaining = creditRemaining,
                detail = "add_view_failed"
            )
            showingBlockedPackage = null
            showingStrictMode = null
            showingReason = null
            return
        }
        showingBlockedPackage = foregroundPackage
        showingStrictMode = strictMode
        showingReason = reason
        dogfoodEventStore.recordStructured(
            type = "overlay_shown",
            target = foregroundPackage,
            policyReason = reason.code,
            creditRemaining = creditRemaining
        )
    }

    private fun hideOverlay(reason: String) {
        if (showingBlockedPackage != null) {
            dogfoodEventStore.recordStructured(
                type = "overlay_hidden",
                target = showingBlockedPackage,
                policyReason = reason,
                creditRemaining = creditStore.read().remainingMinutes
            )
        }
        overlay.hide()
        showingBlockedPackage = null
        showingStrictMode = null
        showingReason = null
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun notification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Commit Unlock")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Commit Unlock Monitor",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "commit_unlock_monitor"
        private const val NOTIFICATION_ID = 1001
        private const val HEARTBEAT_WRITE_INTERVAL_MS = 5_000L
        private const val CREDIT_SPEND_INTERVAL_MS = 60_000L
    }
}
