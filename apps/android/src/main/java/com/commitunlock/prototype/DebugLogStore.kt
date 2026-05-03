package com.commitunlock.prototype

import android.content.Context
import java.time.Instant

class DebugLogStore(context: Context) {
    private val prefs = context.getSharedPreferences("debug_log", Context.MODE_PRIVATE)

    fun record(event: String) {
        val cleanEvent = event.replace("\n", " ").trim()
        if (cleanEvent.isEmpty()) return

        val current = read()
        if (current.firstOrNull()?.endsWith(" $cleanEvent") == true) return

        val next = listOf("${Instant.now()} $cleanEvent")
            .plus(current)
            .take(MAX_EVENTS)

        prefs.edit()
            .putString(KEY_EVENTS, next.joinToString("\n"))
            .apply()
    }

    fun read(): List<String> {
        return prefs.getString(KEY_EVENTS, "")
            .orEmpty()
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun clear() {
        prefs.edit().remove(KEY_EVENTS).apply()
    }

    companion object {
        private const val KEY_EVENTS = "events"
        private const val MAX_EVENTS = 50
    }
}
