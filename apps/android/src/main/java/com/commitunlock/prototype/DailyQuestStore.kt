package com.commitunlock.prototype

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

class DailyQuestStore(context: Context) {
    private val prefs = context.getSharedPreferences("daily_quests", Context.MODE_PRIVATE)

    fun read(timezone: String, now: Instant = Instant.now()): List<DailyQuest> {
        val today = localDateString(timezone, now)
        val storedDate = prefs.getString(KEY_DATE, null)
        if (storedDate != today) {
            save(today, emptyList())
            return emptyList()
        }
        return parseQuests()
    }

    fun add(
        title: String,
        required: Boolean,
        timezone: String,
        now: Instant = Instant.now()
    ): DailyQuest {
        val today = localDateString(timezone, now)
        val quest = DailyQuest(
            id = "quest-${now.toEpochMilli()}",
            title = title.trim(),
            required = required,
            proofType = null,
            proofRef = null,
            status = DailyQuestStatus.PLANNED,
            createdAt = now.toString(),
            completedAt = null
        )
        save(today, listOf(quest).plus(read(timezone, now)).take(MAX_QUESTS))
        return quest
    }

    fun completeNextWithMockProof(
        timezone: String,
        now: Instant = Instant.now()
    ): DailyQuest? {
        val current = read(timezone, now)
        val nextQuest = DailyQuestPolicy.nextQuestForMockProof(current) ?: return null
        val completed = nextQuest.copy(
            proofType = "mock",
            proofRef = "local-mock-${now.toEpochMilli()}",
            status = DailyQuestStatus.COMPLETED,
            completedAt = now.toString()
        )
        val updated = current.map { quest ->
            if (quest.id == completed.id) completed else quest
        }
        save(localDateString(timezone, now), updated)
        return completed
    }

    fun clearToday(timezone: String, now: Instant = Instant.now()) {
        save(localDateString(timezone, now), emptyList())
    }

    private fun parseQuests(): List<DailyQuest> {
        val raw = prefs.getString(KEY_QUESTS, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                DailyQuest(
                    id = item.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null,
                    title = item.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null,
                    required = item.optBoolean("required", true),
                    proofType = item.optString("proofType").takeIf { it.isNotBlank() },
                    proofRef = item.optString("proofRef").takeIf { it.isNotBlank() },
                    status = DailyQuestStatus.fromCode(item.optString("status")),
                    createdAt = item.optString("createdAt"),
                    completedAt = item.optString("completedAt").takeIf { it.isNotBlank() }
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun save(date: String, quests: List<DailyQuest>) {
        val array = JSONArray()
        quests.take(MAX_QUESTS).forEach { quest ->
            array.put(JSONObject().apply {
                put("id", quest.id)
                put("title", quest.title)
                put("required", quest.required)
                put("proofType", quest.proofType.orEmpty())
                put("proofRef", quest.proofRef.orEmpty())
                put("status", quest.status.code)
                put("createdAt", quest.createdAt)
                put("completedAt", quest.completedAt.orEmpty())
            })
        }
        prefs.edit()
            .putString(KEY_DATE, date)
            .putString(KEY_QUESTS, array.toString())
            .apply()
    }

    private fun localDateString(timezone: String, now: Instant): String {
        val zoneId = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
        return now.atZone(zoneId).toLocalDate().toString()
    }

    companion object {
        private const val KEY_DATE = "date"
        private const val KEY_QUESTS = "quests"
        private const val MAX_QUESTS = 20
    }
}
