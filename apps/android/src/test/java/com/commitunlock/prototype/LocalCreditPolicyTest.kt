package com.commitunlock.prototype

import kotlin.test.Test
import kotlin.test.assertEquals

class LocalCreditPolicyTest {
    @Test
    fun clampsLocalCreditBetweenZeroAndThePrototypeCap() {
        assertEquals(0, LocalCreditPolicy.normalizeRemainingMinutes(-10))
        assertEquals(60, LocalCreditPolicy.normalizeRemainingMinutes(60))
        assertEquals(
            LocalCreditPolicy.MAX_LOCAL_TEST_MINUTES,
            LocalCreditPolicy.normalizeRemainingMinutes(999)
        )
    }

    @Test
    fun addMinutesCannotExceedTheLocalPrototypeCap() {
        assertEquals(
            LocalCreditPolicy.MAX_LOCAL_TEST_MINUTES,
            LocalCreditPolicy.addMinutes(LocalCreditPolicy.MAX_LOCAL_TEST_MINUTES - 2, 5)
        )
    }

    @Test
    fun spendMinuteCannotGoBelowZero() {
        assertEquals(0, LocalCreditPolicy.spendMinute(0))
        assertEquals(2, LocalCreditPolicy.spendMinute(3))
    }
}
