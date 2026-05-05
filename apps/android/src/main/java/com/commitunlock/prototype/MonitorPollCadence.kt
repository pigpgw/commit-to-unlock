package com.commitunlock.prototype

object MonitorPollCadence {
    const val ACTIVE_MS = 1_000L
    const val DEGRADED_MS = 5_000L

    fun nextDelayMillis(
        permissionsReady: Boolean,
        hasTargets: Boolean,
        deviceInteractive: Boolean
    ): Long {
        return if (permissionsReady && hasTargets && deviceInteractive) {
            ACTIVE_MS
        } else {
            DEGRADED_MS
        }
    }
}
