package com.commitunlock.prototype

data class SetupChecklistState(
    val usageAccessGranted: Boolean,
    val overlayGranted: Boolean,
    val notificationGranted: Boolean,
    val blockedTargetCount: Int,
    val monitorRunning: Boolean
)

data class SetupChecklistResult(
    val canStartMonitor: Boolean,
    val readyForBlocking: Boolean,
    val missingItems: List<SetupChecklistItem>,
    val nextAction: String
)

enum class SetupChecklistItem(val label: String) {
    USAGE_ACCESS("Grant Usage Access"),
    OVERLAY_PERMISSION("Allow Display over other apps"),
    BLOCKED_TARGET("Add at least one blocked package"),
    MONITOR_STOPPED("Start the monitor service")
}

object SetupChecklist {
    fun evaluate(state: SetupChecklistState): SetupChecklistResult {
        val missingItems = buildList {
            if (!state.usageAccessGranted) add(SetupChecklistItem.USAGE_ACCESS)
            if (!state.overlayGranted) add(SetupChecklistItem.OVERLAY_PERMISSION)
            if (state.blockedTargetCount <= 0) add(SetupChecklistItem.BLOCKED_TARGET)
            if (
                state.usageAccessGranted &&
                state.overlayGranted &&
                state.blockedTargetCount > 0 &&
                !state.monitorRunning
            ) {
                add(SetupChecklistItem.MONITOR_STOPPED)
            }
        }

        val canStartMonitor = state.usageAccessGranted &&
            state.overlayGranted &&
            state.blockedTargetCount > 0

        val nextAction = when {
            !state.usageAccessGranted -> "Grant Usage Access first. Without it Android will not report the foreground app."
            !state.overlayGranted -> "Allow Display over other apps so the blocker can show the pause screen."
            state.blockedTargetCount <= 0 -> "Add a target package, or tap the Chrome demo setup."
            !state.monitorRunning -> "Start or restart the monitor when you are ready to test blocking."
            else -> "Ready. Open a selected target with zero credit to verify the overlay."
        }

        return SetupChecklistResult(
            canStartMonitor = canStartMonitor,
            readyForBlocking = canStartMonitor && state.monitorRunning,
            missingItems = missingItems,
            nextAction = nextAction
        )
    }
}
