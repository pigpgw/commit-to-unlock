package com.commitunlock.prototype

import kotlin.test.Test
import kotlin.test.assertEquals

class TargetGuardrailsTest {
    @Test
    fun acceptsValidUniqueTargetsInInputOrder() {
        val result = TargetGuardrails.normalizeTargets(
            listOf(" com.google.android.youtube ", "com.reddit.frontpage"),
            OWN_PACKAGE
        )

        assertEquals(listOf("com.google.android.youtube", "com.reddit.frontpage"), result.accepted)
        assertEquals(emptyList(), result.rejected)
    }

    @Test
    fun rejectsEmptyInvalidOwnSystemAndDuplicateTargets() {
        val result = TargetGuardrails.normalizeTargets(
            listOf(
                "",
                "not a package",
                OWN_PACKAGE,
                "com.android.settings",
                "com.android.settings.overlay",
                "com.video.app",
                "com.video.app"
            ),
            OWN_PACKAGE
        )

        assertEquals(listOf("com.video.app"), result.accepted)
        assertEquals(
            listOf(
                TargetRejectionReason.EMPTY,
                TargetRejectionReason.INVALID_PACKAGE,
                TargetRejectionReason.OWN_PACKAGE,
                TargetRejectionReason.SYSTEM_CRITICAL,
                TargetRejectionReason.SYSTEM_CRITICAL,
                TargetRejectionReason.DUPLICATE
            ),
            result.rejected.map { it.reason }
        )
    }

    @Test
    fun rejectsLauncherPermissionControllerAndCoreGoogleServices() {
        val result = TargetGuardrails.normalizeTargets(
            listOf(
                "com.google.android.permissioncontroller",
                "com.google.android.gms",
                "com.google.android.apps.nexuslauncher",
                "com.sec.android.app.launcher",
                "com.miui.home"
            ),
            OWN_PACKAGE
        )

        assertEquals(emptyList(), result.accepted)
        assertEquals(
            List(5) { TargetRejectionReason.SYSTEM_CRITICAL },
            result.rejected.map { it.reason }
        )
    }

    private companion object {
        const val OWN_PACKAGE = "com.commitunlock.prototype"
    }
}
