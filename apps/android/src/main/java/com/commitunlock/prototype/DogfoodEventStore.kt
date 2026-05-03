package com.commitunlock.prototype

import android.content.Context
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

data class DogfoodEvent(
    val timestamp: Instant,
    val type: String,
    val detail: String
)

data class DogfoodSummary(
    val monitorEnabledDays: Int,
    val blockedAttempts: Int,
    val permissionFailures: Int,
    val overlayOpens: Int,
    val overlayCreditAdds: Int,
    val automaticCreditSpends: Int,
    val manualCreditChanges: Int,
    val eventCount: Int
)

class DogfoodEventStore(context: Context) {
    private val prefs = context.getSharedPreferences("dogfood_events", Context.MODE_PRIVATE)

    fun record(type: String, detail: String = "") {
        val cleanType = sanitize(type)
        val cleanDetail = sanitize(detail)
        if (cleanType.isEmpty()) return

        val current = readRaw()
        val nextLine = "${Instant.now()}\t$cleanType\t$cleanDetail"
        if (current.firstOrNull()?.substringAfter("\t", "") == nextLine.substringAfter("\t", "")) {
            return
        }

        val next = listOf(nextLine)
            .plus(current)
            .take(MAX_EVENTS)

        prefs.edit()
            .putString(KEY_EVENTS, next.joinToString("\n"))
            .apply()
    }

    fun read(): List<DogfoodEvent> {
        return readRaw().mapNotNull { parse(it) }
    }

    fun summary(now: Instant = Instant.now()): DogfoodSummary {
        val since = now.minus(Duration.ofDays(SUMMARY_DAYS))
        val recent = read().filter { it.timestamp >= since }
        val zoneId = ZoneId.systemDefault()
        val enabledDays = recent
            .filter { it.type == "monitor_started" || it.type == "monitor_heartbeat" }
            .map { it.timestamp.atZone(zoneId).toLocalDate() }
            .distinct()
            .size

        return DogfoodSummary(
            monitorEnabledDays = enabledDays,
            blockedAttempts = recent.count { it.type == "blocked_attempt" },
            permissionFailures = recent.count { it.type == "permission_missing" },
            overlayOpens = recent.count { it.type == "overlay_open_app" },
            overlayCreditAdds = recent.count { it.type == "overlay_add_credit" },
            automaticCreditSpends = recent.count { it.type == "credit_auto_spent" },
            manualCreditChanges = recent.count {
                it.type == "credit_added" || it.type == "credit_spent" || it.type == "credit_reset"
            },
            eventCount = recent.size
        )
    }

    fun exportTsv(): String {
        val header = "timestamp\ttype\tdetail"
        val rows = read()
            .sortedBy { it.timestamp }
            .joinToString("\n") { "${it.timestamp}\t${it.type}\t${it.detail}" }
        return listOf(header, rows)
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    fun clear() {
        prefs.edit().remove(KEY_EVENTS).apply()
    }

    private fun readRaw(): List<String> {
        return prefs.getString(KEY_EVENTS, "")
            .orEmpty()
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun parse(line: String): DogfoodEvent? {
        val parts = line.split('\t', limit = 3)
        if (parts.size < 2) return null

        val timestamp = runCatching { Instant.parse(parts[0]) }.getOrNull() ?: return null
        val type = parts[1]
        val detail = parts.getOrElse(2) { "" }

        return DogfoodEvent(timestamp, type, detail)
    }

    private fun sanitize(value: String): String {
        return value
            .replace("\t", " ")
            .replace("\n", " ")
            .trim()
    }

    companion object {
        private const val KEY_EVENTS = "events"
        private const val MAX_EVENTS = 1_000
        private const val SUMMARY_DAYS = 14L
    }
}
