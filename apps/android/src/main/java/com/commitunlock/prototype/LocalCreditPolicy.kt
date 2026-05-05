package com.commitunlock.prototype

object LocalCreditPolicy {
    const val MAX_LOCAL_TEST_MINUTES = 240

    fun normalizeRemainingMinutes(minutes: Int): Int {
        return minutes.coerceIn(0, MAX_LOCAL_TEST_MINUTES)
    }

    fun addMinutes(currentMinutes: Int, deltaMinutes: Int): Int {
        return normalizeRemainingMinutes(currentMinutes + deltaMinutes)
    }

    fun spendMinute(currentMinutes: Int): Int {
        return normalizeRemainingMinutes(currentMinutes - 1)
    }
}
