package com.commitunlock.prototype

enum class DailyQuestStatus(val code: String) {
    PLANNED("planned"),
    PROOF_SEEN("proof_seen"),
    COMPLETED("completed"),
    REJECTED("rejected");

    companion object {
        fun fromCode(code: String): DailyQuestStatus {
            return entries.firstOrNull { it.code == code } ?: PLANNED
        }
    }
}

data class DailyQuest(
    val id: String,
    val title: String,
    val required: Boolean,
    val proofType: String?,
    val proofRef: String?,
    val status: DailyQuestStatus,
    val createdAt: String,
    val completedAt: String?
)

object DailyQuestPolicy {
    fun shouldGrantFreeDay(quests: List<DailyQuest>): Boolean {
        val requiredQuests = quests.filter { it.required }
        return requiredQuests.isNotEmpty() &&
            requiredQuests.all { it.status == DailyQuestStatus.COMPLETED }
    }

    fun nextQuestForMockProof(quests: List<DailyQuest>): DailyQuest? {
        return quests
            .filter { it.status != DailyQuestStatus.COMPLETED && it.status != DailyQuestStatus.REJECTED }
            .sortedWith(compareByDescending<DailyQuest> { it.required }.thenBy { it.createdAt })
            .firstOrNull()
    }
}
