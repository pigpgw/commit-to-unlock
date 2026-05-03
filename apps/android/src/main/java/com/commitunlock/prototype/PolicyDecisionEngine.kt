package com.commitunlock.prototype

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

object PolicyDecisionEngine {
    fun evaluate(input: PolicyDecisionInput): PolicyDecision {
        val currentPackage = input.currentPackage

        if (currentPackage == null || currentPackage == input.ownPackage) {
            return allow(PolicyDecisionReason.OWN_APP)
        }

        if (!input.creditState.blockedTargets.contains(currentPackage)) {
            return allow(PolicyDecisionReason.TARGET_NOT_BLOCKED)
        }

        val localNow = localNow(input.now, input.policyState.timezone)

        if (!input.policyState.activeWeekdays.contains(localNow.dayOfWeek.value)) {
            return allow(PolicyDecisionReason.INACTIVE_WEEKDAY, currentPackage)
        }

        if (!isWithinActiveTime(localNow.hour * 60 + localNow.minute, input.policyState)) {
            return allow(PolicyDecisionReason.OUTSIDE_ACTIVE_TIME, currentPackage)
        }

        if (input.policyState.isManualHolidayActive(input.now)) {
            return allow(PolicyDecisionReason.MANUAL_HOLIDAY, currentPackage)
        }

        if (input.isPublicHoliday && !input.policyState.applyOnPublicHolidays) {
            return allow(PolicyDecisionReason.PUBLIC_HOLIDAY, currentPackage)
        }

        if (isFutureIso(input.creditState.freeUntil, input.now)) {
            return allow(PolicyDecisionReason.FREE_DAY, currentPackage)
        }

        val activeUnlock = input.activeEmergencyUnlocks.firstOrNull {
            isEmergencyUnlockActive(it, input.now)
        }
        if (activeUnlock != null) {
            return PolicyDecision(
                allowed = true,
                reason = PolicyDecisionReason.EMERGENCY_UNLOCK,
                shouldSpendCredit = false,
                matchedTarget = currentPackage,
                activeEmergencyUnlockId = activeUnlock.id
            )
        }

        if (input.creditState.remainingMinutes > 0) {
            return PolicyDecision(
                allowed = true,
                reason = PolicyDecisionReason.CREDIT_AVAILABLE,
                shouldSpendCredit = true,
                matchedTarget = currentPackage
            )
        }

        return PolicyDecision(
            allowed = false,
            reason = PolicyDecisionReason.CREDIT_EMPTY,
            shouldSpendCredit = false,
            matchedTarget = currentPackage
        )
    }

    private fun allow(reason: PolicyDecisionReason, matchedTarget: String? = null): PolicyDecision {
        return PolicyDecision(
            allowed = true,
            reason = reason,
            shouldSpendCredit = false,
            matchedTarget = matchedTarget
        )
    }

    private fun localNow(now: Instant, timezone: String) =
        now.atZone(runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneOffset.UTC))

    private fun isWithinActiveTime(currentMinutes: Int, policy: PolicyState): Boolean {
        if (policy.activeFrom == null && policy.activeUntil == null) return true

        val fromMinutes = policy.activeFrom?.let { parseTimeToMinutesOrNull(it) } ?: 0
        val untilMinutes = policy.activeUntil?.let { parseTimeToMinutesOrNull(it) } ?: 24 * 60
        if (fromMinutes == untilMinutes) return true

        return if (fromMinutes < untilMinutes) {
            currentMinutes >= fromMinutes && currentMinutes < untilMinutes
        } else {
            currentMinutes >= fromMinutes || currentMinutes < untilMinutes
        }
    }

    private fun parseTimeToMinutesOrNull(value: String): Int? {
        val parts = value.split(":", limit = 2)
        val hour = parts.getOrNull(0)?.toIntOrNull()
        val minute = parts.getOrNull(1)?.toIntOrNull()
        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    private fun isFutureIso(value: String?, now: Instant): Boolean {
        if (value.isNullOrBlank()) return false
        return try {
            Instant.parse(value).isAfter(now)
        } catch (_: DateTimeParseException) {
            false
        }
    }

    private fun isEmergencyUnlockActive(unlock: EmergencyUnlock, now: Instant): Boolean {
        return try {
            !Instant.parse(unlock.startedAt).isAfter(now) && Instant.parse(unlock.expiresAt).isAfter(now)
        } catch (_: DateTimeParseException) {
            false
        }
    }
}
