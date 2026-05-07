package com.commitunlock.prototype

object PrototypeText {
    fun dogfoodEvent(event: DogfoodEvent): String {
        return listOf(
            event.timestamp.toString(),
            event.type,
            event.target?.let { "target=$it" }.orEmpty(),
            event.policyReason?.let { "reason=$it" }.orEmpty(),
            event.creditRemaining?.let { "credit=$it" }.orEmpty(),
            event.detail
        )
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }

    fun questSummary(quests: List<DailyQuest>, state: CreditState): String {
        val requiredCount = quests.count { it.required }
        val completedRequiredCount = quests.count {
            it.required && it.status == DailyQuestStatus.COMPLETED
        }

        return buildString {
            append("Proof quest status\n")
            append("Required: $completedRequiredCount / $requiredCount completed\n")
            append("Free day: ${if (DailyQuestPolicy.shouldGrantFreeDay(quests)) "ready" else "locked"}\n")
            append("Free until: ${state.freeUntil ?: "none"}\n")
            if (quests.isEmpty()) {
                append("No quest planned today")
            } else {
                append(quests.joinToString("\n") { quest ->
                    val requiredLabel = if (quest.required) "required" else "optional"
                    val proofLabel = quest.proofType ?: "no-proof"
                    "[${quest.status.code}] ${quest.title} ($requiredLabel, $proofLabel)"
                })
            }
        }
    }

    fun foregroundUnavailableReason(hasUsageAccess: Boolean): String {
        return if (hasUsageAccess) {
            "unknown"
        } else {
            "unknown (usage access missing)"
        }
    }

    fun monitorHeartbeat(snapshot: MonitorRuntimeSnapshot): String {
        val heartbeatAgeMillis = snapshot.heartbeatAgeMillis ?: return "none"
        return "${heartbeatAgeMillis / 1_000L}s ago"
    }
}
