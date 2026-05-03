package com.commitunlock.prototype

import android.content.Context
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

class EmergencyUnlockStore(context: Context) {
    private val prefs = context.getSharedPreferences("emergency_unlocks", Context.MODE_PRIVATE)

    fun read(now: Instant = Instant.now()): List<EmergencyUnlock> {
        val cutoff = now.minus(Duration.ofDays(8))
        return parseUnlocks()
            .filter { unlock ->
                runCatching { Instant.parse(unlock.startedAt) >= cutoff }.getOrDefault(false)
            }
            .also { save(it) }
    }

    fun active(now: Instant = Instant.now()): List<EmergencyUnlock> {
        return read(now).filter { unlock ->
            runCatching {
                !Instant.parse(unlock.startedAt).isAfter(now) && Instant.parse(unlock.expiresAt).isAfter(now)
            }.getOrDefault(false)
        }
    }

    fun start(durationMinutes: Int, reason: String, now: Instant = Instant.now()): EmergencyUnlock {
        val unlock = EmergencyUnlock(
            id = "unlock-${now.toEpochMilli()}",
            durationMinutes = durationMinutes,
            reason = reason.trim(),
            startedAt = now.toString(),
            expiresAt = now.plus(Duration.ofMinutes(durationMinutes.toLong())).toString()
        )
        save(listOf(unlock).plus(read(now)))
        return unlock
    }

    fun countStartedToday(timezone: String, now: Instant = Instant.now()): Int {
        val zoneId = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
        val today = now.atZone(zoneId).toLocalDate()
        return read(now).count { unlock ->
            runCatching { Instant.parse(unlock.startedAt).atZone(zoneId).toLocalDate() == today }
                .getOrDefault(false)
        }
    }

    fun countStartedSince(since: Instant, now: Instant = Instant.now()): Int {
        return read(now).count { unlock ->
            runCatching { Instant.parse(unlock.startedAt) >= since }.getOrDefault(false)
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_UNLOCKS).apply()
    }

    private fun parseUnlocks(): List<EmergencyUnlock> {
        val raw = prefs.getString(KEY_UNLOCKS, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                EmergencyUnlock(
                    id = item.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null,
                    durationMinutes = item.optInt("durationMinutes"),
                    reason = item.optString("reason"),
                    startedAt = item.optString("startedAt"),
                    expiresAt = item.optString("expiresAt")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun save(unlocks: List<EmergencyUnlock>) {
        val array = JSONArray()
        unlocks.distinctBy { it.id }.take(MAX_UNLOCKS).forEach { unlock ->
            array.put(JSONObject().apply {
                put("id", unlock.id)
                put("durationMinutes", unlock.durationMinutes)
                put("reason", unlock.reason)
                put("startedAt", unlock.startedAt)
                put("expiresAt", unlock.expiresAt)
            })
        }
        prefs.edit().putString(KEY_UNLOCKS, array.toString()).apply()
    }

    companion object {
        private const val KEY_UNLOCKS = "unlocks"
        private const val MAX_UNLOCKS = 50
    }
}
