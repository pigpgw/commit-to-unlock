package com.commitunlock.prototype

data class PermissionDisclosureState(
    val usageAccessGranted: Boolean,
    val overlayGranted: Boolean,
    val notificationGranted: Boolean,
    val dogfoodEventCount: Int
)

object PermissionDisclosureCopy {
    fun build(state: PermissionDisclosureState): String {
        return listOf(
            "Permission and privacy disclosure",
            "Usage Access (${status(state.usageAccessGranted)}): foreground package names only, never screen content.",
            "Overlay (${status(state.overlayGranted)}): block screen for selected targets only.",
            "Notifications (${status(state.notificationGranted)}): visible monitor status on Android 13+.",
            "Dogfood data: latest ${state.dogfoodEventCount} local events may include package names, quest titles, emergency reasons, policy reasons, and credit values.",
            "Export and clear: sharing sends TSV only when you choose it; use redacted export to hide targets, quest titles, and emergency reasons before sharing.",
            "Clearing removes local events and the export file.",
            "Limit: this is a local prototype, not tamper-proof. Permissions can be revoked and the app can be uninstalled."
        ).joinToString("\n")
    }

    private fun status(granted: Boolean): String = if (granted) "granted" else "missing"
}
