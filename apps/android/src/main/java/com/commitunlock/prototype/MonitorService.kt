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
    private lateinit var foregroundReader: ForegroundAppReader
    private lateinit var overlay: BlockOverlay
    private val handler = Handler(Looper.getMainLooper())

    private val pollRunnable = object : Runnable {
        override fun run() {
            poll()
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        creditStore = CreditStore(this)
        foregroundReader = ForegroundAppReader(this)
        overlay = BlockOverlay(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification("Monitoring selected apps"))
        handler.post(pollRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        overlay.hide()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun poll() {
        if (!PermissionChecks.hasUsageAccess(this) || !PermissionChecks.canDrawOverlays(this)) {
            overlay.hide()
            return
        }

        val state = creditStore.read()
        val foregroundPackage = foregroundReader.currentForegroundPackage()
        val shouldBlock = foregroundPackage != null &&
            foregroundPackage != packageName &&
            state.blockedTargets.contains(foregroundPackage) &&
            state.remainingMinutes <= 0

        if (shouldBlock) {
            overlay.show(
                packageName = foregroundPackage,
                onOpenApp = { openMainActivity() },
                onAddCredit = {
                    creditStore.addMinutes(5)
                    overlay.hide()
                }
            )
        } else {
            overlay.hide()
        }
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
