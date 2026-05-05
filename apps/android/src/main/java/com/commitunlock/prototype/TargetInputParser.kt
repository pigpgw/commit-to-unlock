package com.commitunlock.prototype

object TargetInputParser {
    fun parse(rawInput: String): List<String> {
        return rawInput
            .split(Regex("[,\\s]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
