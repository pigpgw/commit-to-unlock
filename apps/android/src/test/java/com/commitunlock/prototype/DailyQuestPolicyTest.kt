package com.commitunlock.prototype

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyQuestPolicyTest {
    @Test
    fun doesNotGrantFreeDayWhenThereAreNoRequiredQuests() {
        val quests = listOf(
            quest(required = false, status = DailyQuestStatus.COMPLETED)
        )

        assertFalse(DailyQuestPolicy.shouldGrantFreeDay(quests))
    }

    @Test
    fun doesNotGrantFreeDayForPlannedRequiredQuest() {
        val quests = listOf(
            quest(required = true, status = DailyQuestStatus.PLANNED)
        )

        assertFalse(DailyQuestPolicy.shouldGrantFreeDay(quests))
    }

    @Test
    fun grantsFreeDayWhenAllRequiredQuestsAreCompleted() {
        val quests = listOf(
            quest(id = "required-1", required = true, status = DailyQuestStatus.COMPLETED),
            quest(id = "optional-1", required = false, status = DailyQuestStatus.PLANNED)
        )

        assertTrue(DailyQuestPolicy.shouldGrantFreeDay(quests))
    }

    @Test
    fun picksRequiredQuestBeforeOptionalQuestForMockProof() {
        val optional = quest(id = "optional-1", required = false, createdAt = "2026-05-04T09:00:00Z")
        val required = quest(id = "required-1", required = true, createdAt = "2026-05-04T10:00:00Z")

        val nextQuest = DailyQuestPolicy.nextQuestForMockProof(listOf(optional, required))

        assertEquals("required-1", nextQuest?.id)
    }

    @Test
    fun skipsCompletedAndRejectedQuestsForMockProof() {
        val completed = quest(id = "done", status = DailyQuestStatus.COMPLETED)
        val rejected = quest(id = "rejected", status = DailyQuestStatus.REJECTED)
        val planned = quest(id = "planned", status = DailyQuestStatus.PLANNED)

        val nextQuest = DailyQuestPolicy.nextQuestForMockProof(listOf(completed, rejected, planned))

        assertEquals("planned", nextQuest?.id)
    }

    private fun quest(
        id: String = "quest-1",
        required: Boolean = true,
        status: DailyQuestStatus = DailyQuestStatus.PLANNED,
        createdAt: String = "2026-05-04T09:00:00Z"
    ): DailyQuest {
        return DailyQuest(
            id = id,
            title = id,
            required = required,
            proofType = if (status == DailyQuestStatus.COMPLETED) "mock" else null,
            proofRef = if (status == DailyQuestStatus.COMPLETED) "local-mock" else null,
            status = status,
            createdAt = createdAt,
            completedAt = if (status == DailyQuestStatus.COMPLETED) "2026-05-04T10:00:00Z" else null
        )
    }
}
