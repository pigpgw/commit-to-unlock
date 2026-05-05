package com.commitunlock.prototype

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

class ForegroundAppReader(context: Context) {
    private val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    fun currentForegroundPackage(): String? {
        val manager = usageStats ?: return null
        return runCatching {
            val now = System.currentTimeMillis()
            val events = manager.queryEvents(now - LOOKBACK_MS, now)
            val event = UsageEvents.Event()
            var latestPackage: String? = null
            var latestTimestamp = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (
                    isForegroundEvent(event.eventType) &&
                    event.timeStamp >= latestTimestamp
                ) {
                    latestPackage = event.packageName
                    latestTimestamp = event.timeStamp
                }
            }

            latestPackage
        }.getOrNull()
    }

    fun recentForegroundPackages(limit: Int = DEFAULT_RECENT_LIMIT): List<String> {
        val manager = usageStats ?: return emptyList()
        return runCatching {
            val now = System.currentTimeMillis()
            val events = manager.queryEvents(now - RECENT_LOOKBACK_MS, now)
            val event = UsageEvents.Event()
            val latestByPackage = mutableMapOf<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (isForegroundEvent(event.eventType)) {
                    latestByPackage[event.packageName] = event.timeStamp
                }
            }

            latestByPackage.entries
                .sortedByDescending { it.value }
                .map { it.key }
                .take(limit)
        }.getOrDefault(emptyList())
    }

    private fun isForegroundEvent(eventType: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return eventType == UsageEvents.Event.ACTIVITY_RESUMED
        }

        @Suppress("DEPRECATION")
        return eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
    }

    companion object {
        private const val LOOKBACK_MS = 10_000L
        private const val RECENT_LOOKBACK_MS = 6 * 60 * 60 * 1_000L
        private const val DEFAULT_RECENT_LIMIT = 8
    }
}
