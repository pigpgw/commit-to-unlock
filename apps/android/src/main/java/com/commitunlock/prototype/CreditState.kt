package com.commitunlock.prototype

data class CreditState(
    val remainingMinutes: Int,
    val blockedTargets: List<String>,
    val freeUntil: String?,
    val strictMode: Boolean,
    val lastUpdatedAt: String
)
