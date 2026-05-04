package com.commitunlock.prototype

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MonitorRuntimeStatusTest {
    @Test
    fun reportsStoppedWhenMonitorIsNotDesired() {
        val snapshot = MonitorRuntimeStatus.evaluate(
            desiredRunning = false,
            lastHeartbeatAtMillis = 1_000L,
            nowMillis = 2_000L,
            staleAfterMillis = 5_000L
        )

        assertEquals(MonitorRuntimeState.STOPPED, snapshot.state)
        assertEquals(false, snapshot.desiredRunning)
        assertNull(snapshot.heartbeatAgeMillis)
    }

    @Test
    fun reportsRunningWhenHeartbeatIsFresh() {
        val snapshot = MonitorRuntimeStatus.evaluate(
            desiredRunning = true,
            lastHeartbeatAtMillis = 10_000L,
            nowMillis = 13_000L,
            staleAfterMillis = 5_000L
        )

        assertEquals(MonitorRuntimeState.RUNNING, snapshot.state)
        assertEquals(3_000L, snapshot.heartbeatAgeMillis)
    }

    @Test
    fun reportsStaleWhenServiceIsNotActuallyRunning() {
        val snapshot = MonitorRuntimeStatus.evaluate(
            desiredRunning = true,
            lastHeartbeatAtMillis = 10_000L,
            nowMillis = 13_000L,
            staleAfterMillis = 5_000L,
            serviceRunning = false
        )

        assertEquals(MonitorRuntimeState.STALE, snapshot.state)
        assertEquals(3_000L, snapshot.heartbeatAgeMillis)
    }

    @Test
    fun reportsStaleWhenHeartbeatIsMissing() {
        val snapshot = MonitorRuntimeStatus.evaluate(
            desiredRunning = true,
            lastHeartbeatAtMillis = null,
            nowMillis = 13_000L,
            staleAfterMillis = 5_000L
        )

        assertEquals(MonitorRuntimeState.STALE, snapshot.state)
        assertNull(snapshot.heartbeatAgeMillis)
    }

    @Test
    fun reportsStaleWhenHeartbeatExpires() {
        val snapshot = MonitorRuntimeStatus.evaluate(
            desiredRunning = true,
            lastHeartbeatAtMillis = 10_000L,
            nowMillis = 16_001L,
            staleAfterMillis = 5_000L
        )

        assertEquals(MonitorRuntimeState.STALE, snapshot.state)
        assertEquals(6_001L, snapshot.heartbeatAgeMillis)
    }

    @Test
    fun clockRollbackDoesNotCreateNegativeAge() {
        val snapshot = MonitorRuntimeStatus.evaluate(
            desiredRunning = true,
            lastHeartbeatAtMillis = 20_000L,
            nowMillis = 19_000L,
            staleAfterMillis = 5_000L
        )

        assertEquals(MonitorRuntimeState.RUNNING, snapshot.state)
        assertEquals(0L, snapshot.heartbeatAgeMillis)
    }
}
