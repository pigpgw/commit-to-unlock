package com.commitunlock.prototype

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ForegroundPackageResolverTest {
    @Test
    fun keepsBlockedPackageWhenOverlayMakesOwnPackageLookForeground() {
        assertEquals(
            "com.android.chrome",
            ForegroundPackageResolver.resolveForPolicy(
                rawForegroundPackage = "com.commitunlock.prototype",
                ownPackage = "com.commitunlock.prototype",
                showingBlockedPackage = "com.android.chrome",
                lastResolvedPackage = "com.android.chrome"
            )
        )
    }

    @Test
    fun preservesRealForegroundPackageWhenNoOverlayIsShowing() {
        assertEquals(
            "com.android.chrome",
            ForegroundPackageResolver.resolveForPolicy(
                rawForegroundPackage = "com.android.chrome",
                ownPackage = "com.commitunlock.prototype",
                showingBlockedPackage = null,
                lastResolvedPackage = null
            )
        )
    }

    @Test
    fun preservesOwnPackageWhenNoOverlayIsShowing() {
        assertEquals(
            "com.commitunlock.prototype",
            ForegroundPackageResolver.resolveForPolicy(
                rawForegroundPackage = "com.commitunlock.prototype",
                ownPackage = "com.commitunlock.prototype",
                showingBlockedPackage = null,
                lastResolvedPackage = "com.android.chrome"
            )
        )
    }

    @Test
    fun fallsBackToLastResolvedPackageWhenUsageStatsLookbackExpires() {
        assertEquals(
            "com.android.chrome",
            ForegroundPackageResolver.resolveForPolicy(
                rawForegroundPackage = null,
                ownPackage = "com.commitunlock.prototype",
                showingBlockedPackage = null,
                lastResolvedPackage = "com.android.chrome"
            )
        )
    }

    @Test
    fun preservesNullForegroundWhenNoPreviousPackageExists() {
        assertNull(
            ForegroundPackageResolver.resolveForPolicy(
                rawForegroundPackage = null,
                ownPackage = "com.commitunlock.prototype",
                showingBlockedPackage = null,
                lastResolvedPackage = null
            )
        )
    }

    @Test
    fun keepsBlockedPackageWhenUsageStatsLookbackExpiresUnderOverlay() {
        assertEquals(
            "com.android.chrome",
            ForegroundPackageResolver.resolveForPolicy(
                rawForegroundPackage = null,
                ownPackage = "com.commitunlock.prototype",
                showingBlockedPackage = "com.android.chrome",
                lastResolvedPackage = null
            )
        )
    }
}
