package com.commitunlock.prototype

import android.content.Context

class MonitorStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("monitor_state", Context.MODE_PRIVATE)

    fun isDesiredRunning(): Boolean {
        return prefs.getBoolean(
            KEY_DESIRED_RUNNING,
            prefs.getBoolean(LEGACY_KEY_RUNNING, false)
        )
    }

    fun setDesiredRunning(running: Boolean) {
        prefs.edit()
            .putBoolean(KEY_DESIRED_RUNNING, running)
            .putBoolean(LEGACY_KEY_RUNNING, running)
            .apply()
    }

    fun recordHeartbeat(nowMillis: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_HEARTBEAT_AT_MILLIS, nowMillis).apply()
    }

    fun clearHeartbeat() {
        prefs.edit().remove(KEY_LAST_HEARTBEAT_AT_MILLIS).apply()
    }

    fun lastHeartbeatAtMillis(): Long? {
        val value = prefs.getLong(KEY_LAST_HEARTBEAT_AT_MILLIS, 0L)
        return value.takeIf { it > 0L }
    }

    fun runtimeStatus(
        nowMillis: Long = System.currentTimeMillis(),
        serviceRunning: Boolean? = null
    ): MonitorRuntimeSnapshot {
        return MonitorRuntimeStatus.evaluate(
            desiredRunning = isDesiredRunning(),
            lastHeartbeatAtMillis = lastHeartbeatAtMillis(),
            nowMillis = nowMillis,
            serviceRunning = serviceRunning
        )
    }

    companion object {
        private const val KEY_DESIRED_RUNNING = "desired_running"
        private const val KEY_LAST_HEARTBEAT_AT_MILLIS = "last_heartbeat_at_millis"
        private const val LEGACY_KEY_RUNNING = "running"
    }
}
