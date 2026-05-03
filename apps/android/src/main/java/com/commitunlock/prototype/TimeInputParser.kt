package com.commitunlock.prototype

sealed interface TimeInputValue {
    data object Blank : TimeInputValue
    data object Invalid : TimeInputValue
    data class Valid(val value: String) : TimeInputValue

    fun valueOrNull(): String? {
        return when (this) {
            Blank -> null
            Invalid -> null
            is Valid -> value
        }
    }
}

object TimeInputParser {
    private val timePattern = Regex("""^\d{1,2}:\d{2}$""")

    fun normalize(value: String): TimeInputValue {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return TimeInputValue.Blank
        if (!timePattern.matches(trimmed)) return TimeInputValue.Invalid

        val hour = trimmed.substringBefore(":").toInt()
        val minute = trimmed.substringAfter(":").toInt()
        if (hour !in 0..23 || minute !in 0..59) return TimeInputValue.Invalid

        return TimeInputValue.Valid("%02d:%02d".format(hour, minute))
    }
}
