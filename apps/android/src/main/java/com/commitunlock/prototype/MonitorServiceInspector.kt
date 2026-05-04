package com.commitunlock.prototype

import android.app.ActivityManager
import android.content.Context

object MonitorServiceInspector {
    fun isMonitorServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(ActivityManager::class.java)
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE).any { service ->
            service.service.className == MonitorService::class.java.name
        }
    }
}
