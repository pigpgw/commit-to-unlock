package com.commitunlock.prototype

object DogfoodExportRedactor {
    private const val REDACTED_TARGET = "<target:redacted>"
    private const val REDACTED_VALUE = "<redacted>"
    private val sensitiveDetailPattern = Regex("""\b(title|reason)=.*""")

    fun target(value: String?): String {
        return if (value.isNullOrBlank()) "" else REDACTED_TARGET
    }

    fun detail(value: String): String {
        return value.replace(sensitiveDetailPattern) { match ->
            "${match.groupValues[1]}=$REDACTED_VALUE"
        }
    }
}
