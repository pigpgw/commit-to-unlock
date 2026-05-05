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
            "Usage Access (${status(state.usageAccessGranted)}): reads foreground package names, not screen content.",
            "Overlay (${status(state.overlayGranted)}): draws the block screen only over selected targets.",
            "Notifications (${status(state.notificationGranted)}): keeps the monitor visible on Android 13+.",
            "Dogfood data: latest ${state.dogfoodEventCount} local events; may include package names, quest titles, emergency reasons, policy reasons, and credit values.",
            "Export and clear: Share dogfood export only sends TSV when you choose it. Clear dogfood events removes the local event log and export file.",
            "Limit: this is a local prototype, not tamper-proof. Permissions can be revoked and the app can be uninstalled."
        ).joinToString("\n")
    }

    private fun status(granted: Boolean): String = if (granted) "granted" else "missing"
}
