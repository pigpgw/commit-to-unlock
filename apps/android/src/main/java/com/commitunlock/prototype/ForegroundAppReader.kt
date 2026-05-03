package com.commitunlock.prototype

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class ForegroundAppReader(context: Context) {
    private val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun currentForegroundPackage(): String? {
        val now = System.currentTimeMillis()
        val events = usageStats.queryEvents(now - LOOKBACK_MS, now)
        val event = UsageEvents.Event()
        var latestPackage: String? = null
        var latestTimestamp = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND &&
                event.timeStamp >= latestTimestamp
            ) {
                latestPackage = event.packageName
                latestTimestamp = event.timeStamp
            }
        }

        return latestPackage
    }

    companion object {
        private const val LOOKBACK_MS = 10_000L
    }
}
