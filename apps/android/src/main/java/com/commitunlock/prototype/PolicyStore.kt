package com.commitunlock.prototype

import android.content.Context
import java.time.Instant
import java.time.ZoneId

class PolicyStore(context: Context) {
    private val prefs = context.getSharedPreferences("policy_state", Context.MODE_PRIVATE)

    fun read(): PolicyState {
        return PolicyState(
            activeWeekdays = readActiveWeekdays(),
            activeFrom = prefs.getString(KEY_ACTIVE_FROM, null).emptyToNull(),
            activeUntil = prefs.getString(KEY_ACTIVE_UNTIL, null).emptyToNull(),
            applyOnPublicHolidays = prefs.getBoolean(KEY_APPLY_ON_PUBLIC_HOLIDAYS, false),
            manualHolidayDate = prefs.getString(KEY_MANUAL_HOLIDAY_DATE, null).emptyToNull(),
            timezone = prefs.getString(KEY_TIMEZONE, ZoneId.systemDefault().id)
                ?: ZoneId.systemDefault().id
        )
    }

    fun save(state: PolicyState) {
        prefs.edit()
            .putString(KEY_ACTIVE_WEEKDAYS, state.activeWeekdays.sorted().joinToString(","))
            .putString(KEY_ACTIVE_FROM, state.activeFrom.orEmpty())
            .putString(KEY_ACTIVE_UNTIL, state.activeUntil.orEmpty())
            .putBoolean(KEY_APPLY_ON_PUBLIC_HOLIDAYS, state.applyOnPublicHolidays)
            .putString(KEY_MANUAL_HOLIDAY_DATE, state.manualHolidayDate.orEmpty())
            .putString(KEY_TIMEZONE, state.timezone.ifBlank { ZoneId.systemDefault().id })
            .apply()
    }

    fun setManualHolidayToday(enabled: Boolean, now: Instant = Instant.now()) {
        val current = read()
        val zoneId = runCatching { ZoneId.of(current.timezone) }.getOrDefault(ZoneId.systemDefault())
        val today = now.atZone(zoneId).toLocalDate().toString()
        save(current.copy(manualHolidayDate = if (enabled) today else null))
    }

    private fun readActiveWeekdays(): List<Int> {
        val raw = prefs.getString(KEY_ACTIVE_WEEKDAYS, null)
        if (raw.isNullOrBlank()) return DEFAULT_ACTIVE_WEEKDAYS
        return raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .distinct()
            .sorted()
    }

    private fun String?.emptyToNull(): String? {
        return this?.trim()?.takeIf { it.isNotEmpty() }
    }

    companion object {
        private val DEFAULT_ACTIVE_WEEKDAYS = listOf(1, 2, 3, 4, 5)
        private const val KEY_ACTIVE_WEEKDAYS = "active_weekdays"
        private const val KEY_ACTIVE_FROM = "active_from"
        private const val KEY_ACTIVE_UNTIL = "active_until"
        private const val KEY_APPLY_ON_PUBLIC_HOLIDAYS = "apply_on_public_holidays"
        private const val KEY_MANUAL_HOLIDAY_DATE = "manual_holiday_date"
        private const val KEY_TIMEZONE = "timezone"
    }
}
