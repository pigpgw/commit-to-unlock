package com.commitunlock.prototype

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SetupChecklistTest {
    @Test
    fun blocksMonitorStartUntilUsageAccessOverlayAndTargetsAreReady() {
        val result = SetupChecklist.evaluate(
            SetupChecklistState(
                usageAccessGranted = false,
                overlayGranted = false,
                notificationGranted = false,
                blockedTargetCount = 0,
                monitorRunning = false
            )
        )

        assertFalse(result.canStartMonitor)
        assertFalse(result.readyForBlocking)
        assertEquals(
            listOf(
                SetupChecklistItem.USAGE_ACCESS,
                SetupChecklistItem.OVERLAY_PERMISSION,
                SetupChecklistItem.BLOCKED_TARGET
            ),
            result.missingItems
        )
    }

    @Test
    fun allowsMonitorStartWhenCorePrerequisitesAreReady() {
        val result = SetupChecklist.evaluate(
            SetupChecklistState(
                usageAccessGranted = true,
                overlayGranted = true,
                notificationGranted = false,
                blockedTargetCount = 1,
                monitorRunning = false
            )
        )

        assertTrue(result.canStartMonitor)
        assertFalse(result.readyForBlocking)
        assertEquals(listOf(SetupChecklistItem.MONITOR_STOPPED), result.missingItems)
    }

    @Test
    fun reportsReadyWhenMonitorIsRunningWithCorePrerequisites() {
        val result = SetupChecklist.evaluate(
            SetupChecklistState(
                usageAccessGranted = true,
                overlayGranted = true,
                notificationGranted = true,
                blockedTargetCount = 2,
                monitorRunning = true
            )
        )

        assertTrue(result.canStartMonitor)
        assertTrue(result.readyForBlocking)
        assertTrue(result.missingItems.isEmpty())
    }
}
