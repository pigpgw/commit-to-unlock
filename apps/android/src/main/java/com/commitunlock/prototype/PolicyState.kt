package com.commitunlock.prototype

import java.time.Instant
import java.time.ZoneId

data class PolicyState(
    val activeWeekdays: List<Int>,
    val activeFrom: String?,
    val activeUntil: String?,
    val applyOnPublicHolidays: Boolean,
    val manualHolidayDate: String?,
    val timezone: String
) {
    fun isManualHolidayActive(now: Instant = Instant.now()): Boolean {
        val zoneId = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
        return manualHolidayDate == now.atZone(zoneId).toLocalDate().toString()
    }
}

data class EmergencyUnlock(
    val id: String,
    val durationMinutes: Int,
    val reason: String,
    val startedAt: String,
    val expiresAt: String
)

enum class PolicyDecisionReason(val code: String) {
    OWN_APP("own_app"),
    TARGET_NOT_BLOCKED("target_not_blocked"),
    INACTIVE_WEEKDAY("inactive_weekday"),
    OUTSIDE_ACTIVE_TIME("outside_active_time"),
    MANUAL_HOLIDAY("manual_holiday"),
    PUBLIC_HOLIDAY("public_holiday"),
    FREE_DAY("free_day"),
    EMERGENCY_UNLOCK("emergency_unlock"),
    CREDIT_AVAILABLE("credit_available"),
    CREDIT_EMPTY("credit_empty")
}

data class PolicyDecision(
    val allowed: Boolean,
    val reason: PolicyDecisionReason,
    val shouldSpendCredit: Boolean,
    val matchedTarget: String? = null,
    val activeEmergencyUnlockId: String? = null
)

data class PolicyDecisionInput(
    val currentPackage: String?,
    val ownPackage: String,
    val now: Instant,
    val creditState: CreditState,
    val policyState: PolicyState,
    val activeEmergencyUnlocks: List<EmergencyUnlock> = emptyList(),
    val isPublicHoliday: Boolean = false
)
