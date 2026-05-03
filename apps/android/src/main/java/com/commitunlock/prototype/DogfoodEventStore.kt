package com.commitunlock.prototype

import android.content.Context
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

const val DOGFOOD_EXPORT_FILE_NAME = "dogfood-export.tsv"

data class DogfoodEvent(
    val timestamp: Instant,
    val type: String,
    val target: String?,
    val policyReason: String?,
    val creditRemaining: Int?,
    val detail: String
)

data class DogfoodSummary(
    val monitorEnabledDays: Int,
    val blockedAttempts: Int,
    val policyBlocks: Int,
    val emergencyUnlocks: Int,
    val freeDays: Int,
    val dailyQuestsAdded: Int,
    val dailyQuestMockCompletions: Int,
    val permissionFailures: Int,
    val overlayOpens: Int,
    val overlayCreditAdds: Int,
    val automaticCreditSpends: Int,
    val manualCreditChanges: Int,
    val eventCount: Int
)

class DogfoodEventStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("dogfood_events", Context.MODE_PRIVATE)

    init {
        writeExportFile()
    }

    fun record(type: String, detail: String = "") {
        recordStructured(type = type, detail = detail)
    }

    fun recordStructured(
        type: String,
        target: String? = null,
        policyReason: String? = null,
        creditRemaining: Int? = null,
        detail: String = ""
    ) {
        val cleanType = sanitize(type)
        val cleanTarget = sanitize(target.orEmpty())
        val cleanPolicyReason = sanitize(policyReason.orEmpty())
        val cleanCreditRemaining = creditRemaining?.coerceAtLeast(0)?.toString().orEmpty()
        val cleanDetail = sanitize(detail)
        if (cleanType.isEmpty()) return

        val current = readRaw()
        val nextLine = listOf(
            Instant.now().toString(),
            cleanType,
            cleanTarget,
            cleanPolicyReason,
            cleanCreditRemaining,
            cleanDetail
        ).joinToString("\t")
        if (current.firstOrNull()?.substringAfter("\t", "") == nextLine.substringAfter("\t", "")) {
            return
        }

        val next = listOf(nextLine)
            .plus(current)
            .take(MAX_EVENTS)

        val saved = prefs.edit()
            .putString(KEY_EVENTS, next.joinToString("\n"))
            .commit()

        if (saved) {
            writeExportFile(next)
        }
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
            policyBlocks = recent.count { it.type == "policy_blocked" },
            emergencyUnlocks = recent.count { it.type == "emergency_unlock_started" },
            freeDays = recent.count { it.type == "free_day_set" },
            dailyQuestsAdded = recent.count { it.type == "daily_quest_added" },
            dailyQuestMockCompletions = recent.count { it.type == "daily_quest_mock_completed" },
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
        val header = EXPORT_HEADER
        val rows = read()
            .sortedBy { it.timestamp }
            .joinToString("\n") { serialize(it) }
        return listOf(header, rows)
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    fun clear() {
        val cleared = prefs.edit().remove(KEY_EVENTS).commit()
        if (cleared) {
            writeExportFile(emptyList())
        }
    }

    private fun readRaw(): List<String> {
        return prefs.getString(KEY_EVENTS, "")
            .orEmpty()
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun parse(line: String): DogfoodEvent? {
        val parts = line.split('\t', limit = 6)
        if (parts.size < 2) return null

        val timestamp = runCatching { Instant.parse(parts[0]) }.getOrNull() ?: return null
        val type = parts[1]
        if (parts.size >= 6) {
            val creditRemaining = parts[4].takeIf { it.isNotBlank() }?.toIntOrNull()
            return DogfoodEvent(
                timestamp = timestamp,
                type = type,
                target = parts[2].takeIf { it.isNotBlank() },
                policyReason = parts[3].takeIf { it.isNotBlank() },
                creditRemaining = creditRemaining,
                detail = parts[5]
            )
        }

        return DogfoodEvent(
            timestamp = timestamp,
            type = type,
            target = null,
            policyReason = null,
            creditRemaining = null,
            detail = parts.getOrElse(2) { "" }
        )
    }

    private fun writeExportFile(rawEvents: List<String> = readRaw()) {
        val rows = rawEvents
            .mapNotNull { parse(it) }
            .sortedBy { it.timestamp }
            .joinToString("\n") { serialize(it) }
        val export = listOf(EXPORT_HEADER, rows)
            .filter { it.isNotEmpty() }
            .joinToString("\n")

        appContext.openFileOutput(DOGFOOD_EXPORT_FILE_NAME, Context.MODE_PRIVATE).use { output ->
            output.write(export.toByteArray(Charsets.UTF_8))
        }
    }

    private fun sanitize(value: String): String {
        return value
            .replace("\t", " ")
            .replace("\n", " ")
            .trim()
    }

    private fun serialize(event: DogfoodEvent): String {
        return listOf(
            event.timestamp.toString(),
            sanitize(event.type),
            sanitize(event.target.orEmpty()),
            sanitize(event.policyReason.orEmpty()),
            event.creditRemaining?.coerceAtLeast(0)?.toString().orEmpty(),
            sanitize(event.detail)
        ).joinToString("\t")
    }

    companion object {
        private const val KEY_EVENTS = "events"
        private const val MAX_EVENTS = 1_000
        private const val SUMMARY_DAYS = 14L
        private const val EXPORT_HEADER = "timestamp\ttype\ttarget\tpolicy_reason\tcredit_remaining\tdetail"
    }
}
