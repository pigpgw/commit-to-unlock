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
import java.time.LocalDate

class MonitorService : Service() {
    private lateinit var creditStore: CreditStore
    private lateinit var dogfoodEventStore: DogfoodEventStore
    private lateinit var foregroundReader: ForegroundAppReader
    private lateinit var monitorStateStore: MonitorStateStore
    private lateinit var overlay: BlockOverlay
    private val handler = Handler(Looper.getMainLooper())
    private var lastForegroundPackage: String? = null
    private var showingBlockedPackage: String? = null
    private var showingStrictMode: Boolean? = null
    private var lastHeartbeatDay: String? = null
    private var activeSpendPackage: String? = null
    private var lastSpendTickMs: Long? = null
    private var spendAccumulatorMs: Long = 0L

    private val pollRunnable = object : Runnable {
        override fun run() {
            poll()
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        creditStore = CreditStore(this)
        dogfoodEventStore = DogfoodEventStore(this)
        foregroundReader = ForegroundAppReader(this)
        monitorStateStore = MonitorStateStore(this)
        overlay = BlockOverlay(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification("Monitoring selected apps"))
        monitorStateStore.setRunning(true)
        dogfoodEventStore.record("monitor_started")
        handler.post(pollRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        hideOverlay("monitor_stopped")
        monitorStateStore.setRunning(false)
        dogfoodEventStore.record("monitor_stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun poll() {
        recordHeartbeat()

        if (!PermissionChecks.hasUsageAccess(this)) {
            dogfoodEventStore.record("permission_missing", "usage_access")
            stopSpendSession("usage_access_missing")
            hideOverlay("usage_access_missing")
            return
        }

        if (!PermissionChecks.canDrawOverlays(this)) {
            dogfoodEventStore.record("permission_missing", "overlay")
            stopSpendSession("overlay_permission_missing")
            hideOverlay("overlay_permission_missing")
            return
        }

        var state = creditStore.read()
        val foregroundPackage = foregroundReader.currentForegroundPackage()
        val deviceInteractive = isDeviceInteractive()

        if (foregroundPackage != null && foregroundPackage != lastForegroundPackage) {
            lastForegroundPackage = foregroundPackage
            dogfoodEventStore.record("foreground_changed", foregroundPackage)
        }

        val isTrackedTarget = foregroundPackage != null &&
            foregroundPackage != packageName &&
            state.blockedTargets.contains(foregroundPackage)

        if (foregroundPackage != null && isTrackedTarget && state.remainingMinutes > 0 && deviceInteractive) {
            state = accrueSpend(foregroundPackage, state)
        } else {
            stopSpendSession(
                when {
                    !deviceInteractive -> "device_not_interactive"
                    foregroundPackage == null -> "foreground_unknown"
                    !isTrackedTarget -> "target_mismatch"
                    else -> "no_credit"
                },
                clearAccumulator = state.remainingMinutes <= 0
            )
        }

        val shouldBlock = isTrackedTarget && state.remainingMinutes <= 0

        if (shouldBlock) {
            val blockedPackage = foregroundPackage
            dogfoodEventStore.record("target_matched", blockedPackage)
            showOverlay(blockedPackage, state.strictMode)
        } else {
            hideOverlay(
                when {
                    foregroundPackage == null -> "foreground_unknown"
                    state.remainingMinutes > 0 -> "credit_available"
                    else -> "target_mismatch"
                }
            )
        }
    }

    private fun recordHeartbeat() {
        val today = LocalDate.now().toString()
        if (lastHeartbeatDay == today) return

        lastHeartbeatDay = today
        dogfoodEventStore.record("monitor_heartbeat", "date=$today")
    }

    private fun accrueSpend(foregroundPackage: String, state: CreditState): CreditState {
        val nowMs = SystemClock.elapsedRealtime()
        if (activeSpendPackage != foregroundPackage) {
            stopSpendSession("target_changed")
            activeSpendPackage = foregroundPackage
            lastSpendTickMs = nowMs
            dogfoodEventStore.record("target_use_started", foregroundPackage)
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
            dogfoodEventStore.record(
                "credit_auto_spent",
                "package=$foregroundPackage minutes=1 remaining=${updatedState.remainingMinutes}"
            )
        }

        if (updatedState.remainingMinutes <= 0) {
            stopSpendSession("credit_depleted", clearAccumulator = true)
        }

        return updatedState
    }

    private fun stopSpendSession(reason: String, clearAccumulator: Boolean = false) {
        val packageName = activeSpendPackage ?: return
        dogfoodEventStore.record(
            "target_use_stopped",
            "package=$packageName reason=$reason pending_ms=$spendAccumulatorMs"
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

    private fun showOverlay(foregroundPackage: String, strictMode: Boolean) {
        if (showingBlockedPackage == foregroundPackage && showingStrictMode == strictMode) return

        dogfoodEventStore.record("blocked_attempt", foregroundPackage)
        overlay.hide()
        overlay.show(
            packageName = foregroundPackage,
            strictMode = strictMode,
            onOpenApp = {
                dogfoodEventStore.record("overlay_open_app", foregroundPackage)
                openMainActivity()
            },
            onAddCredit = {
                creditStore.addMinutes(5)
                dogfoodEventStore.record("overlay_add_credit", "package=$foregroundPackage minutes=5")
                hideOverlay("credit_added")
            }
        )
        showingBlockedPackage = foregroundPackage
        showingStrictMode = strictMode
        dogfoodEventStore.record("overlay_shown", foregroundPackage)
    }

    private fun hideOverlay(reason: String) {
        if (showingBlockedPackage != null) {
            dogfoodEventStore.record("overlay_hidden", "package=$showingBlockedPackage reason=$reason")
        }
        overlay.hide()
        showingBlockedPackage = null
        showingStrictMode = null
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
        private const val POLL_MS = 1_000L
        private const val CREDIT_SPEND_INTERVAL_MS = 60_000L
    }
}
