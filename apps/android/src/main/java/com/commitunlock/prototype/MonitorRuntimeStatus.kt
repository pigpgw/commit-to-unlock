package com.commitunlock.prototype

enum class MonitorRuntimeState(val code: String) {
    STOPPED("stopped"),
    RUNNING("running"),
    STALE("stale")
}

data class MonitorRuntimeSnapshot(
    val desiredRunning: Boolean,
    val state: MonitorRuntimeState,
    val lastHeartbeatAtMillis: Long?,
    val heartbeatAgeMillis: Long?
)

object MonitorRuntimeStatus {
    const val DEFAULT_STALE_AFTER_MS = 15_000L

    fun evaluate(
        desiredRunning: Boolean,
        lastHeartbeatAtMillis: Long?,
        nowMillis: Long,
        staleAfterMillis: Long = DEFAULT_STALE_AFTER_MS,
        serviceRunning: Boolean? = null
    ): MonitorRuntimeSnapshot {
        if (!desiredRunning) {
            return MonitorRuntimeSnapshot(
                desiredRunning = false,
                state = MonitorRuntimeState.STOPPED,
                lastHeartbeatAtMillis = lastHeartbeatAtMillis,
                heartbeatAgeMillis = null
            )
        }

        val heartbeatAgeMillis = lastHeartbeatAtMillis
            ?.let { (nowMillis - it).coerceAtLeast(0L) }

        if (serviceRunning == false) {
            return MonitorRuntimeSnapshot(
                desiredRunning = true,
                state = MonitorRuntimeState.STALE,
                lastHeartbeatAtMillis = lastHeartbeatAtMillis,
                heartbeatAgeMillis = heartbeatAgeMillis
            )
        }

        if (lastHeartbeatAtMillis == null) {
            return MonitorRuntimeSnapshot(
                desiredRunning = true,
                state = MonitorRuntimeState.STALE,
                lastHeartbeatAtMillis = null,
                heartbeatAgeMillis = null
            )
        }

        val state = if (heartbeatAgeMillis != null && heartbeatAgeMillis <= staleAfterMillis) {
            MonitorRuntimeState.RUNNING
        } else {
            MonitorRuntimeState.STALE
        }

        return MonitorRuntimeSnapshot(
            desiredRunning = true,
            state = state,
            lastHeartbeatAtMillis = lastHeartbeatAtMillis,
            heartbeatAgeMillis = heartbeatAgeMillis
        )
    }
}
