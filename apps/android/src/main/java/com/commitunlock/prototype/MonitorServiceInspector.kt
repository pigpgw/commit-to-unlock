package com.commitunlock.prototype

import android.app.ActivityManager
import android.content.Context

object MonitorServiceInspector {
    fun isMonitorServiceRunning(context: Context): Boolean {
        return runCatching {
            val manager = context.getSystemService(ActivityManager::class.java)
                ?: return@runCatching false
            @Suppress("DEPRECATION")
            manager.getRunningServices(Int.MAX_VALUE).any { service ->
                service.service.className == MonitorService::class.java.name
            }
        }.getOrDefault(false)
    }
}
