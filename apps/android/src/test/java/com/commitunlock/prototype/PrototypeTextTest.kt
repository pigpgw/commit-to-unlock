package com.commitunlock.prototype

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class PrototypeTextTest {
    @Test
    fun formatsDogfoodEventWithoutEmptyFields() {
        val event = DogfoodEvent(
            timestamp = Instant.parse("2026-05-04T00:00:00Z"),
            type = "overlay_shown",
            target = "com.example.video",
            policyReason = "credit_empty",
            creditRemaining = 0,
            detail = ""
        )

        assertEquals(
            "2026-05-04T00:00:00Z overlay_shown target=com.example.video reason=credit_empty credit=0",
            PrototypeText.dogfoodEvent(event)
        )
    }

    @Test
    fun buildsQuestSummaryFromProofBackedStatus() {
        val state = CreditState(
            remainingMinutes = 0,
            blockedTargets = listOf("com.example.video"),
            freeUntil = "2026-05-04T14:59:59Z",
            strictMode = false,
            lastUpdatedAt = "2026-05-04T00:00:00Z"
        )
        val quests = listOf(
            DailyQuest(
                id = "quest-1",
                title = "Refactor policy UI",
                required = true,
                proofType = "mock",
                proofRef = "local",
                status = DailyQuestStatus.COMPLETED,
                createdAt = "2026-05-04T00:00:00Z",
                completedAt = "2026-05-04T01:00:00Z"
            )
        )

        val summary = PrototypeText.questSummary(quests, state)

        assertContains(summary, "Required: 1 / 1 completed")
        assertContains(summary, "Free day: ready")
        assertContains(summary, "[completed] Refactor policy UI (required, mock)")
    }

    @Test
    fun foregroundUnavailableReasonNamesMissingUsageAccess() {
        assertEquals("unknown", PrototypeText.foregroundUnavailableReason(hasUsageAccess = true))
        assertEquals(
            "unknown (usage access missing)",
            PrototypeText.foregroundUnavailableReason(hasUsageAccess = false)
        )
    }

    @Test
    fun formatsMonitorHeartbeat() {
        assertEquals(
            "3s ago",
            PrototypeText.monitorHeartbeat(
                MonitorRuntimeSnapshot(
                    desiredRunning = true,
                    state = MonitorRuntimeState.RUNNING,
                    lastHeartbeatAtMillis = 1_000L,
                    heartbeatAgeMillis = 3_500L
                )
            )
        )
        assertEquals(
            "none",
            PrototypeText.monitorHeartbeat(
                MonitorRuntimeSnapshot(
                    desiredRunning = true,
                    state = MonitorRuntimeState.STALE,
                    lastHeartbeatAtMillis = null,
                    heartbeatAgeMillis = null
                )
            )
        )
    }
}
