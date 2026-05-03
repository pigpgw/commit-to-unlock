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

class MonitorService : Service() {
    private lateinit var creditStore: CreditStore
    private lateinit var debugLogStore: DebugLogStore
    private lateinit var foregroundReader: ForegroundAppReader
    private lateinit var monitorStateStore: MonitorStateStore
    private lateinit var overlay: BlockOverlay
    private val handler = Handler(Looper.getMainLooper())
    private var lastForegroundPackage: String? = null
    private var showingBlockedPackage: String? = null

    private val pollRunnable = object : Runnable {
        override fun run() {
            poll()
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        creditStore = CreditStore(this)
        debugLogStore = DebugLogStore(this)
        foregroundReader = ForegroundAppReader(this)
        monitorStateStore = MonitorStateStore(this)
        overlay = BlockOverlay(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification("Monitoring selected apps"))
        monitorStateStore.setRunning(true)
        debugLogStore.record("monitor_started")
        handler.post(pollRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        hideOverlay("monitor_stopped")
        monitorStateStore.setRunning(false)
        debugLogStore.record("monitor_stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun poll() {
        if (!PermissionChecks.hasUsageAccess(this)) {
            debugLogStore.record("usage_access_missing")
            hideOverlay("usage_access_missing")
            return
        }

        if (!PermissionChecks.canDrawOverlays(this)) {
            debugLogStore.record("overlay_permission_missing")
            hideOverlay("overlay_permission_missing")
            return
        }

        val state = creditStore.read()
        val foregroundPackage = foregroundReader.currentForegroundPackage()

        if (foregroundPackage != null && foregroundPackage != lastForegroundPackage) {
            lastForegroundPackage = foregroundPackage
            debugLogStore.record("foreground_changed:$foregroundPackage")
        }

        val shouldBlock = foregroundPackage != null &&
            foregroundPackage != packageName &&
            state.blockedTargets.contains(foregroundPackage) &&
            state.remainingMinutes <= 0

        if (shouldBlock) {
            debugLogStore.record("target_matched:$foregroundPackage")
            showOverlay(foregroundPackage, state.strictMode)
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

    private fun showOverlay(foregroundPackage: String, strictMode: Boolean) {
        if (showingBlockedPackage == foregroundPackage) return

        overlay.hide()
        overlay.show(
            packageName = foregroundPackage,
            canAddCredit = !strictMode,
            onOpenApp = { openMainActivity() },
            onAddCredit = {
                creditStore.addMinutes(5)
                debugLogStore.record("credit_added:5")
                hideOverlay("credit_added")
            }
        )
        showingBlockedPackage = foregroundPackage
        debugLogStore.record("overlay_shown:$foregroundPackage")
    }

    private fun hideOverlay(reason: String) {
        if (showingBlockedPackage != null) {
            debugLogStore.record("overlay_hidden:$reason")
        }
        overlay.hide()
        showingBlockedPackage = null
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
    }
}
