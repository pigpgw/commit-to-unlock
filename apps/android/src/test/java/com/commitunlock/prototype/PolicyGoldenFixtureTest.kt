package com.commitunlock.prototype

import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import org.json.JSONArray
import org.json.JSONObject

class PolicyGoldenFixtureTest {
    @Test
    fun evaluatesSharedGoldenFixtures() {
        val fixtures = JSONArray(policyGoldenFile().readText())

        for (index in 0 until fixtures.length()) {
            val fixture = fixtures.getJSONObject(index)
            val id = fixture.getString("id")
            val decision = PolicyDecisionEngine.evaluate(fixture.toPolicyDecisionInput())
            val expected = fixture.getJSONObject("expected")

            assertEquals(expected.getBoolean("allowed"), decision.allowed, "$id allowed")
            assertEquals(expected.getString("reason"), decision.reason.code, "$id reason")
            assertEquals(expected.getBoolean("shouldSpendCredit"), decision.shouldSpendCredit, "$id shouldSpendCredit")
            assertEquals(expected.nullableString("matchedTarget"), decision.matchedTarget, "$id matchedTarget")
            assertEquals(
                expected.nullableString("activeEmergencyUnlockId"),
                decision.activeEmergencyUnlockId,
                "$id activeEmergencyUnlockId"
            )
        }
    }

    private fun JSONObject.toPolicyDecisionInput(): PolicyDecisionInput {
        val input = getJSONObject("input")
        val creditState = input.getJSONObject("creditState")
        val policyState = input.getJSONObject("policyState")

        return PolicyDecisionInput(
            currentPackage = input.nullableString("currentPackage"),
            ownPackage = input.getString("ownPackage"),
            now = Instant.parse(input.getString("now")),
            creditState = CreditState(
                remainingMinutes = creditState.getInt("remainingMinutes"),
                blockedTargets = policyState.stringList("blockedTargets"),
                freeUntil = creditState.nullableString("freeUntil"),
                strictMode = creditState.getBoolean("strictMode"),
                lastUpdatedAt = creditState.getString("lastUpdatedAt")
            ),
            policyState = PolicyState(
                activeWeekdays = policyState.intList("activeWeekdays"),
                activeFrom = policyState.nullableString("activeFrom"),
                activeUntil = policyState.nullableString("activeUntil"),
                applyOnPublicHolidays = policyState.getBoolean("applyOnPublicHolidays"),
                manualHolidayDate = policyState.nullableString("manualHolidayDate"),
                timezone = policyState.getString("timezone")
            ),
            activeEmergencyUnlocks = input.emergencyUnlocks("activeEmergencyUnlocks"),
            isPublicHoliday = input.getBoolean("isPublicHoliday")
        )
    }

    private fun policyGoldenFile(): File {
        var directory = File(System.getProperty("user.dir") ?: error("user.dir not set")).canonicalFile
        while (true) {
            val candidate = File(directory, "fixtures/policy-golden.json")
            if (candidate.isFile) return candidate
            directory = directory.parentFile ?: error("fixtures/policy-golden.json not found")
        }
    }

    private fun JSONObject.nullableString(name: String): String? {
        if (!has(name) || isNull(name)) return null
        return getString(name)
    }

    private fun JSONObject.stringList(name: String): List<String> {
        val array = getJSONArray(name)
        return (0 until array.length()).map { index -> array.getString(index) }
    }

    private fun JSONObject.intList(name: String): List<Int> {
        val array = getJSONArray(name)
        return (0 until array.length()).map { index -> array.getInt(index) }
    }

    private fun JSONObject.emergencyUnlocks(name: String): List<EmergencyUnlock> {
        val array = getJSONArray(name)
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            EmergencyUnlock(
                id = item.getString("id"),
                durationMinutes = item.getInt("durationMinutes"),
                reason = item.getString("reason"),
                startedAt = item.getString("startedAt"),
                expiresAt = item.getString("expiresAt")
            )
        }
    }
}
