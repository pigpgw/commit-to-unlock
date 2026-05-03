package com.commitunlock.prototype

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolicyDecisionEngineTest {
    private val mondayAtNoon = Instant.parse("2026-05-04T12:00:00.000Z")

    private fun baseInput(): PolicyDecisionInput {
        return PolicyDecisionInput(
            currentPackage = "com.video.app",
            ownPackage = "com.commitunlock.prototype",
            now = mondayAtNoon,
            creditState = CreditState(
                remainingMinutes = 0,
                blockedTargets = listOf("com.video.app"),
                freeUntil = null,
                strictMode = false,
                lastUpdatedAt = mondayAtNoon.toString()
            ),
            policyState = PolicyState(
                activeWeekdays = listOf(1, 2, 3, 4, 5),
                activeFrom = null,
                activeUntil = null,
                applyOnPublicHolidays = false,
                manualHolidayDate = null,
                timezone = "UTC"
            )
        )
    }

    @Test
    fun allowsOwnAppBeforePolicy() {
        val decision = PolicyDecisionEngine.evaluate(
            baseInput().copy(currentPackage = "com.commitunlock.prototype")
        )

        assertTrue(decision.allowed)
        assertEquals(PolicyDecisionReason.OWN_APP, decision.reason)
        assertFalse(decision.shouldSpendCredit)
    }

    @Test
    fun allowsTargetOutsideBlockedList() {
        val decision = PolicyDecisionEngine.evaluate(
            baseInput().copy(currentPackage = "com.editor.app")
        )

        assertTrue(decision.allowed)
        assertEquals(PolicyDecisionReason.TARGET_NOT_BLOCKED, decision.reason)
    }

    @Test
    fun allowsInactiveWeekday() {
        val decision = PolicyDecisionEngine.evaluate(
            baseInput().copy(now = Instant.parse("2026-05-03T12:00:00.000Z"))
        )

        assertTrue(decision.allowed)
        assertEquals(PolicyDecisionReason.INACTIVE_WEEKDAY, decision.reason)
    }

    @Test
    fun allowsManualHolidayWithoutCreditSpend() {
        val decision = PolicyDecisionEngine.evaluate(
            baseInput().copy(
                policyState = baseInput().policyState.copy(manualHolidayDate = "2026-05-04")
            )
        )

        assertTrue(decision.allowed)
        assertEquals(PolicyDecisionReason.MANUAL_HOLIDAY, decision.reason)
        assertFalse(decision.shouldSpendCredit)
    }

    @Test
    fun allowsEmergencyUnlockBeforeCredit() {
        val decision = PolicyDecisionEngine.evaluate(
            baseInput().copy(
                activeEmergencyUnlocks = listOf(
                    EmergencyUnlock(
                        id = "unlock-1",
                        durationMinutes = 15,
                        reason = "Production incident",
                        startedAt = "2026-05-04T11:50:00Z",
                        expiresAt = "2026-05-04T12:05:00Z"
                    )
                )
            )
        )

        assertTrue(decision.allowed)
        assertEquals(PolicyDecisionReason.EMERGENCY_UNLOCK, decision.reason)
        assertEquals("unlock-1", decision.activeEmergencyUnlockId)
        assertFalse(decision.shouldSpendCredit)
    }

    @Test
    fun allowsFreeDayBeforeEmergencyUnlockOrCredit() {
        val decision = PolicyDecisionEngine.evaluate(
            baseInput().copy(
                creditState = baseInput().creditState.copy(
                    freeUntil = "2026-05-04T23:59:59Z"
                ),
                activeEmergencyUnlocks = listOf(
                    EmergencyUnlock(
                        id = "unlock-1",
                        durationMinutes = 15,
                        reason = "Check something",
                        startedAt = "2026-05-04T11:50:00Z",
                        expiresAt = "2026-05-04T12:05:00Z"
                    )
                )
            )
        )

        assertTrue(decision.allowed)
        assertEquals(PolicyDecisionReason.FREE_DAY, decision.reason)
        assertFalse(decision.shouldSpendCredit)
    }

    @Test
    fun allowsOutsideActiveTimeWindow() {
        val decision = PolicyDecisionEngine.evaluate(
            baseInput().copy(
                now = Instant.parse("2026-05-04T20:30:00Z"),
                policyState = baseInput().policyState.copy(
                    activeFrom = "22:00",
                    activeUntil = "02:00"
                )
            )
        )

        assertTrue(decision.allowed)
        assertEquals(PolicyDecisionReason.OUTSIDE_ACTIVE_TIME, decision.reason)
    }

    @Test
    fun spendsCreditWhenAvailable() {
        val decision = PolicyDecisionEngine.evaluate(
            baseInput().copy(
                creditState = baseInput().creditState.copy(remainingMinutes = 5)
            )
        )

        assertTrue(decision.allowed)
        assertEquals(PolicyDecisionReason.CREDIT_AVAILABLE, decision.reason)
        assertTrue(decision.shouldSpendCredit)
    }

    @Test
    fun blocksMatchedTargetWhenCreditIsEmpty() {
        val decision = PolicyDecisionEngine.evaluate(baseInput())

        assertFalse(decision.allowed)
        assertEquals(PolicyDecisionReason.CREDIT_EMPTY, decision.reason)
    }

    @Test
    fun supportsOvernightActiveWindow() {
        val decision = PolicyDecisionEngine.evaluate(
            baseInput().copy(
                now = Instant.parse("2026-05-04T23:30:00Z"),
                policyState = baseInput().policyState.copy(
                    activeFrom = "22:00",
                    activeUntil = "02:00"
                )
            )
        )

        assertFalse(decision.allowed)
        assertEquals(PolicyDecisionReason.CREDIT_EMPTY, decision.reason)
    }

    @Test
    fun evaluatesWeekdayAndTimeInPolicyTimezone() {
        val decision = PolicyDecisionEngine.evaluate(
            baseInput().copy(
                now = Instant.parse("2026-05-03T15:30:00Z"),
                policyState = baseInput().policyState.copy(
                    activeWeekdays = listOf(1),
                    activeFrom = "00:00",
                    activeUntil = "01:00",
                    timezone = "Asia/Seoul"
                )
            )
        )

        assertFalse(decision.allowed)
        assertEquals(PolicyDecisionReason.CREDIT_EMPTY, decision.reason)
    }
}
