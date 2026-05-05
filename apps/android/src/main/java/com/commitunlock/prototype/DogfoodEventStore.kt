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
    val overlayFailures: Int,
    val automaticCreditSpends: Int,
    val manualCreditChanges: Int,
    val eventCount: Int
)

class DogfoodEventStore internal constructor(
    private val storage: DogfoodEventStorage,
    private val now: () -> Instant = { Instant.now() }
) {
    constructor(context: Context) : this(AndroidDogfoodEventStorage(context))

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
            now().toString(),
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

        val saved = storage.writeRaw(next)

        if (saved) {
            writeExportFile(next)
        }
    }

    fun read(): List<DogfoodEvent> {
        return readRaw().mapNotNull { parse(it) }
    }

    fun summary(now: Instant = Instant.now()): DogfoodSummary {
        return summary(read(), now)
    }

    fun summary(events: List<DogfoodEvent>, now: Instant = Instant.now()): DogfoodSummary {
        val since = now.minus(Duration.ofDays(SUMMARY_DAYS))
        val recent = events.filter { it.timestamp >= since }
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
            overlayFailures = recent.count { it.type == "overlay_show_failed" },
            automaticCreditSpends = recent.count { it.type == "credit_auto_spent" },
            manualCreditChanges = recent.count {
                it.type == "credit_added" || it.type == "credit_spent" || it.type == "credit_reset"
            },
            eventCount = recent.size
        )
    }

    fun exportTsv(redactSensitive: Boolean = false): String {
        val header = EXPORT_HEADER
        val rows = read()
            .sortedBy { it.timestamp }
            .joinToString("\n") { serialize(it, redactSensitive) }
        return listOf(header, rows)
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    fun clear() {
        val cleared = storage.writeRaw(emptyList())
        if (cleared) {
            writeExportFile(emptyList())
        }
    }

    private fun readRaw(): List<String> {
        return storage.readRaw()
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
            .joinToString("\n") { serialize(it, redactSensitive = false) }
        val export = listOf(EXPORT_HEADER, rows)
            .filter { it.isNotEmpty() }
            .joinToString("\n")

        storage.writeExport(export)
    }

    private fun sanitize(value: String): String {
        return value
            .replace("\t", " ")
            .replace("\n", " ")
            .trim()
    }

    private fun serialize(event: DogfoodEvent, redactSensitive: Boolean): String {
        val target = if (redactSensitive) {
            DogfoodExportRedactor.target(event.target)
        } else {
            event.target.orEmpty()
        }
        val detail = if (redactSensitive) {
            DogfoodExportRedactor.detail(event.detail)
        } else {
            event.detail
        }

        return listOf(
            event.timestamp.toString(),
            sanitize(event.type),
            sanitize(target),
            sanitize(event.policyReason.orEmpty()),
            event.creditRemaining?.coerceAtLeast(0)?.toString().orEmpty(),
            sanitize(detail)
        ).joinToString("\t")
    }

    companion object {
        private const val MAX_EVENTS = 1_000
        private const val SUMMARY_DAYS = 14L
        private const val EXPORT_HEADER = "timestamp\ttype\ttarget\tpolicy_reason\tcredit_remaining\tdetail"
    }
}

internal interface DogfoodEventStorage {
    fun readRaw(): List<String>
    fun writeRaw(events: List<String>): Boolean
    fun writeExport(export: String)
}

private class AndroidDogfoodEventStorage(context: Context) : DogfoodEventStorage {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("dogfood_events", Context.MODE_PRIVATE)

    override fun readRaw(): List<String> {
        return prefs.getString(KEY_EVENTS, "")
            .orEmpty()
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    override fun writeRaw(events: List<String>): Boolean {
        return if (events.isEmpty()) {
            prefs.edit().remove(KEY_EVENTS).commit()
        } else {
            prefs.edit().putString(KEY_EVENTS, events.joinToString("\n")).commit()
        }
    }

    override fun writeExport(export: String) {
        appContext.openFileOutput(DOGFOOD_EXPORT_FILE_NAME, Context.MODE_PRIVATE).use { output ->
            output.write(export.toByteArray(Charsets.UTF_8))
        }
    }

    private companion object {
        private const val KEY_EVENTS = "events"
    }
}
