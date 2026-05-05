package com.commitunlock.prototype

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class PermissionDisclosureCopyTest {
    @Test
    fun includesPermissionStatusesAndDataBoundaries() {
        val copy = PermissionDisclosureCopy.build(
            PermissionDisclosureState(
                usageAccessGranted = true,
                overlayGranted = false,
                notificationGranted = true,
                dogfoodEventCount = 42
            )
        )

        assertContains(copy, "Usage Access (granted)")
        assertContains(copy, "Overlay (missing)")
        assertContains(copy, "Notifications (granted)")
        assertContains(copy, "latest 42 local events")
        assertContains(copy, "package names")
        assertContains(copy, "quest titles")
        assertContains(copy, "emergency reasons")
        assertContains(copy, "sharing sends TSV only when you choose it")
        assertContains(copy, "Clearing removes local events")
        assertContains(copy, "not tamper-proof")
    }

    @Test
    fun doesNotClaimServerSyncOrUninstallProtection() {
        val copy = PermissionDisclosureCopy.build(
            PermissionDisclosureState(
                usageAccessGranted = false,
                overlayGranted = false,
                notificationGranted = false,
                dogfoodEventCount = 0
            )
        )

        assertContains(copy, "local prototype")
        assertContains(copy, "Permissions can be revoked")
        assertContains(copy, "app can be uninstalled")
        assertFalse(copy.contains("server sync", ignoreCase = true))
        assertFalse(copy.contains("cannot be uninstalled", ignoreCase = true))
    }
}
