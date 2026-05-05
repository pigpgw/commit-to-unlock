package com.commitunlock.prototype

import kotlin.test.Test
import kotlin.test.assertEquals

class MonitorPollCadenceTest {
    @Test
    fun usesActiveCadenceWhenMonitorCanEnforceTargets() {
        val delay = MonitorPollCadence.nextDelayMillis(
            permissionsReady = true,
            hasTargets = true,
            deviceInteractive = true
        )

        assertEquals(1_000L, delay)
    }

    @Test
    fun backsOffWhenPermissionsAreMissing() {
        val delay = MonitorPollCadence.nextDelayMillis(
            permissionsReady = false,
            hasTargets = true,
            deviceInteractive = true
        )

        assertEquals(5_000L, delay)
    }

    @Test
    fun backsOffWhenNoTargetsAreConfigured() {
        val delay = MonitorPollCadence.nextDelayMillis(
            permissionsReady = true,
            hasTargets = false,
            deviceInteractive = true
        )

        assertEquals(5_000L, delay)
    }

    @Test
    fun backsOffWhenDeviceIsNotInteractive() {
        val delay = MonitorPollCadence.nextDelayMillis(
            permissionsReady = true,
            hasTargets = true,
            deviceInteractive = false
        )

        assertEquals(5_000L, delay)
    }
}
