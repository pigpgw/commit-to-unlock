package com.commitunlock.prototype

import android.content.Context

class DeveloperGateStore(context: Context) {
    private val prefs = context.getSharedPreferences("developer_gate", Context.MODE_PRIVATE)

    fun isAccepted(): Boolean = prefs.getBoolean(KEY_ACCEPTED, false)

    fun accept() {
        prefs.edit().putBoolean(KEY_ACCEPTED, true).apply()
    }

    companion object {
        private const val KEY_ACCEPTED = "accepted"
    }
}
