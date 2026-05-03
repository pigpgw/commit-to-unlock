package com.commitunlock.prototype

import android.content.Context
import java.time.Instant

class CreditStore(context: Context) {
    private val prefs = context.getSharedPreferences("credit_state", Context.MODE_PRIVATE)

    fun read(): CreditState {
        val targets = prefs.getString(KEY_TARGETS, "")
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return CreditState(
            remainingMinutes = prefs.getInt(KEY_REMAINING_MINUTES, 0),
            blockedTargets = targets,
            freeUntil = prefs.getString(KEY_FREE_UNTIL, null),
            strictMode = prefs.getBoolean(KEY_STRICT_MODE, false),
            lastUpdatedAt = prefs.getString(KEY_LAST_UPDATED_AT, Instant.now().toString())
                ?: Instant.now().toString()
        )
    }

    fun save(state: CreditState) {
        prefs.edit()
            .putInt(KEY_REMAINING_MINUTES, state.remainingMinutes.coerceAtLeast(0))
            .putString(KEY_TARGETS, state.blockedTargets.joinToString(","))
            .putString(KEY_FREE_UNTIL, state.freeUntil)
            .putBoolean(KEY_STRICT_MODE, state.strictMode)
            .putString(KEY_LAST_UPDATED_AT, state.lastUpdatedAt)
            .apply()
    }

    fun addMinutes(minutes: Int) {
        val current = read()
        save(current.copy(
            remainingMinutes = (current.remainingMinutes + minutes).coerceAtLeast(0),
            lastUpdatedAt = Instant.now().toString()
        ))
    }

    fun spendMinute() {
        val current = read()
        save(current.copy(
            remainingMinutes = (current.remainingMinutes - 1).coerceAtLeast(0),
            lastUpdatedAt = Instant.now().toString()
        ))
    }

    fun resetCredit() {
        val current = read()
        save(current.copy(
            remainingMinutes = 0,
            lastUpdatedAt = Instant.now().toString()
        ))
    }

    fun setFreeUntil(freeUntil: String?) {
        val current = read()
        save(current.copy(
            freeUntil = freeUntil,
            lastUpdatedAt = Instant.now().toString()
        ))
    }

    companion object {
        private const val KEY_REMAINING_MINUTES = "remaining_minutes"
        private const val KEY_TARGETS = "blocked_targets"
        private const val KEY_FREE_UNTIL = "free_until"
        private const val KEY_STRICT_MODE = "strict_mode"
        private const val KEY_LAST_UPDATED_AT = "last_updated_at"
    }
}
