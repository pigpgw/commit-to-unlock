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
            "Usage Access (${status(state.usageAccessGranted)}): reads foreground package names so selected targets can be matched. It does not read screen content.",
            "Overlay (${status(state.overlayGranted)}): shows the local blocking screen over selected target apps when policy blocks access. It does not control other apps.",
            "Notifications (${status(state.notificationGranted)}): keeps the Android monitor visible while it is running, especially on Android 13+.",
            "Dogfood data: stores the latest ${state.dogfoodEventCount} local events shown in the app. Events may include timestamps, package names, policy reasons, credit values, quest titles, and emergency reasons.",
            "Export and clear: Share dogfood export only sends TSV when you choose it. Clear dogfood events removes the local event log and export file.",
            "Limit: this is a local prototype, not tamper-proof. Permissions can be revoked, device time can be changed, and the app can be uninstalled."
        ).joinToString("\n")
    }

    private fun status(granted: Boolean): String = if (granted) "granted" else "missing"
}
