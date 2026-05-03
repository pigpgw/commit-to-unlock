package com.commitunlock.prototype

import android.content.Context

class MonitorStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("monitor_state", Context.MODE_PRIVATE)

    fun isRunning(): Boolean = prefs.getBoolean(KEY_RUNNING, false)

    fun setRunning(running: Boolean) {
        prefs.edit().putBoolean(KEY_RUNNING, running).apply()
    }

    companion object {
        private const val KEY_RUNNING = "running"
    }
}
