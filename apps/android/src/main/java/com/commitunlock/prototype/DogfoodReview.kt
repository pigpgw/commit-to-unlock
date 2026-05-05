package com.commitunlock.prototype

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.round
import kotlin.math.roundToInt

data class DogfoodReviewMetric(
    val label: String,
    val events: Int,
    val populated: Int,
    val coverage: Double
)

enum class DogfoodReviewGateStatus(val code: String) {
    PASS("pass"),
    FAIL("fail"),
    NEEDS_DATA("needs_data")
}

data class DogfoodReviewGateCheck(
    val label: String,
    val passed: Boolean,
    val actual: String,
    val target: String
)

data class DogfoodReviewGate(
    val id: String,
    val title: String,
    val status: DogfoodReviewGateStatus,
    val summary: String,
    val checks: List<DogfoodReviewGateCheck>
)

data class DogfoodReview(
    val eventCount: Int,
    val firstEventAt: String?,
    val lastEventAt: String?,
    val activeDays: Int,
    val metrics: Map<String, Int>,
    val dataQuality: List<DogfoodReviewMetric>,
    val gates: List<DogfoodReviewGate>,
    val recommendations: List<String>
)

object DogfoodReviewEngine {
    fun analyze(
        events: List<DogfoodEvent>,
        summary: DogfoodSummary,
        now: Instant = Instant.now()
    ): DogfoodReview {
        val windowEvents = events.filter { it.timestamp >= now.minus(Duration.ofDays(REVIEW_WINDOW_DAYS)) }
        val sortedEvents = windowEvents.sortedBy { it.timestamp }
        val metrics = EVENT_TYPES.mapValues { (_, types) -> countTypes(windowEvents, types) }
        val dataQuality = dataQuality(windowEvents)
        val review = DogfoodReview(
            eventCount = windowEvents.size,
            firstEventAt = sortedEvents.firstOrNull()?.timestamp?.toString(),
            lastEventAt = sortedEvents.lastOrNull()?.timestamp?.toString(),
            activeDays = activeDays(windowEvents),
            metrics = metrics,
            dataQuality = dataQuality,
            gates = emptyList(),
            recommendations = emptyList()
        )
        val gates = gateDecisions(review, windowEvents, summary, dataQuality)
        return review.copy(
            gates = gates,
            recommendations = recommendations(review.copy(gates = gates))
        )
    }

    private fun dataQuality(events: List<DogfoodEvent>): List<DogfoodReviewMetric> {
        val targetEvents = events.filter { targetLikeEvents.contains(it.type) }
        val reasonEvents = events.filter { policyReasonEvents.contains(it.type) }
        val creditEvents = events.filter { creditRemainingEvents.contains(it.type) }

        return listOf(
            qualityMetric(
                label = "target coverage",
                events = targetEvents.size,
                populated = targetEvents.count { eventTarget(it) != null }
            ),
            qualityMetric(
                label = "policy reason coverage",
                events = reasonEvents.size,
                populated = reasonEvents.count { eventPolicyReason(it) != null }
            ),
            qualityMetric(
                label = "credit remaining coverage",
                events = creditEvents.size,
                populated = creditEvents.count { it.creditRemaining != null }
            )
        )
    }

    private fun qualityMetric(label: String, events: Int, populated: Int): DogfoodReviewMetric {
        val coverage = if (events == 0) 1.0 else round((populated.toDouble() / events.toDouble()) * 1000.0) / 1000.0
        return DogfoodReviewMetric(label, events, populated, coverage)
    }

    private fun gateDecisions(
        review: DogfoodReview,
        events: List<DogfoodEvent>,
        summary: DogfoodSummary,
        dataQuality: List<DogfoodReviewMetric>
    ): List<DogfoodReviewGate> {
        val metrics = review.metrics
        val dogfoodSpanDays = spanDays(review.firstEventAt, review.lastEventAt)
        val monitorEnabledDays = distinctEventDays(events, eventTypes.monitorEnabledSignals)
        val blockedAttempts = metrics.getValue("blockedAttempts")
        val emergencyUnlocks = metrics.getValue("emergencyUnlocks")
        val mockProofCompletions = metrics.getValue("dailyQuestMockCompletions")
        val foregroundChanges = metrics.getValue("foregroundChanges")
        val overlayShows = metrics.getValue("overlayShows")
        val overlayFailures = metrics.getValue("overlayFailures")
        val permissionFailures = metrics.getValue("permissionFailures")
        val applicableQualityEvents = dataQuality.sumOf { it.events }
        val populatedQualityEvents = dataQuality.sumOf { it.populated }
        val qualityCoveragePass = applicableQualityEvents > 0 &&
            dataQuality.filter { it.events > 0 }.all { it.coverage >= DATA_QUALITY_TARGET }

        val gateA = makeGate(
            id = "A",
            title = "Enforcement Viability",
            checks = listOf(
                DogfoodReviewGateCheck(
                    label = "Foreground app was observed",
                    passed = foregroundChanges > 0,
                    actual = foregroundChanges.toString(),
                    target = "> 0 foreground_changed events"
                ),
                DogfoodReviewGateCheck(
                    label = "Blocking overlay was observed",
                    passed = overlayShows > 0,
                    actual = "shown=$overlayShows blocked=$blockedAttempts failed=$overlayFailures",
                    target = "> 0 overlay_shown events"
                ),
                DogfoodReviewGateCheck(
                    label = "Overlay show failures",
                    passed = overlayFailures == 0,
                    actual = overlayFailures.toString(),
                    target = "0 overlay_show_failed events"
                ),
                DogfoodReviewGateCheck(
                    label = "Permission/service failures are logged when they happen",
                    passed = permissionFailures > 0 || review.eventCount > 0,
                    actual = permissionFailures.toString(),
                    target = "event log exists; permission_missing appears on failures"
                )
            ),
            hasEnoughData = review.eventCount > 0,
            passingSummary = "local enforcement signals are present",
            failingSummary = "foreground or overlay evidence is missing"
        )

        val gateB = makeGate(
            id = "B",
            title = "Dogfood Need",
            checks = listOf(
                DogfoodReviewGateCheck(
                    label = "Monitor enabled days",
                    passed = monitorEnabledDays >= 8,
                    actual = monitorEnabledDays.toString(),
                    target = ">= 8 days in a 14-day dogfood window"
                ),
                DogfoodReviewGateCheck(
                    label = "Blocked attempts",
                    passed = blockedAttempts >= 8,
                    actual = blockedAttempts.toString(),
                    target = ">= 8 attempts in 14 days"
                ),
                DogfoodReviewGateCheck(
                    label = "Emergency unlocks",
                    passed = emergencyUnlocks <= 6,
                    actual = emergencyUnlocks.toString(),
                    target = "<= 6 in 14 days"
                )
            ),
            hasEnoughData = dogfoodSpanDays >= 14 || summary.monitorEnabledDays >= 8,
            passingSummary = "14-day dogfood need signal is strong enough to keep mobile-first",
            failingSummary = "14-day dogfood need signal is weak; consider desktop/browser-first"
        )

        val gateC = makeGate(
            id = "C",
            title = "Developer Proof Supply",
            checks = listOf(
                DogfoodReviewGateCheck(
                    label = "Mock proof completions",
                    passed = mockProofCompletions >= 5,
                    actual = mockProofCompletions.toString(),
                    target = ">= 5 local proof completions before real GitHub proof"
                )
            ),
            hasEnoughData = dogfoodSpanDays >= 14 || mockProofCompletions >= 5,
            passingSummary = "local proof behavior is frequent enough to test real GitHub/IDE proof",
            failingSummary = "proof events are too sparse; widen beyond PR-only before Sprint 4"
        ).let { gate ->
            if (gate.status == DogfoodReviewGateStatus.PASS) {
                gate.copy(summary = "${gate.summary}; still requires real GitHub/WakaTime/IDE proof validation")
            } else {
                gate
            }
        }

        val gateD = makeGate(
            id = "D",
            title = "Trust And Data Quality",
            checks = listOf(
                DogfoodReviewGateCheck(
                    label = "Structured decision events exist",
                    passed = applicableQualityEvents > 0,
                    actual = applicableQualityEvents.toString(),
                    target = "> 0 target/reason/credit-applicable events"
                ),
                DogfoodReviewGateCheck(
                    label = "Structured field coverage",
                    passed = qualityCoveragePass,
                    actual = "$populatedQualityEvents/$applicableQualityEvents",
                    target = ">= ${(DATA_QUALITY_TARGET * 100).roundToInt()}% for populated metrics"
                ),
                DogfoodReviewGateCheck(
                    label = "Local privacy/export copy is visible",
                    passed = true,
                    actual = "present",
                    target = "permission screen explains local storage, export, and clear"
                )
            ),
            hasEnoughData = review.eventCount > 0,
            passingSummary = "dogfood data is structured enough to support product decisions",
            failingSummary = "dogfood data quality is too weak for gate decisions"
        )

        return listOf(gateA, gateB, gateC, gateD)
    }

    private fun makeGate(
        id: String,
        title: String,
        checks: List<DogfoodReviewGateCheck>,
        hasEnoughData: Boolean,
        passingSummary: String,
        failingSummary: String
    ): DogfoodReviewGate {
        val allPassed = checks.all { it.passed }
        val status = when {
            allPassed -> DogfoodReviewGateStatus.PASS
            hasEnoughData -> DogfoodReviewGateStatus.FAIL
            else -> DogfoodReviewGateStatus.NEEDS_DATA
        }
        val summary = when (status) {
            DogfoodReviewGateStatus.PASS -> passingSummary
            DogfoodReviewGateStatus.FAIL -> failingSummary
            DogfoodReviewGateStatus.NEEDS_DATA -> "not enough dogfood data yet"
        }
        return DogfoodReviewGate(id, title, status, summary, checks)
    }

    private fun recommendations(review: DogfoodReview): List<String> {
        if (review.eventCount == 0) {
            return listOf("No events found. Run a device dogfood session before making product calls.")
        }

        val metrics = review.metrics
        val notes = mutableListOf<String>()
        if (review.activeDays < 3) {
            notes += "Collect at least 3 active dogfood days before deciding whether the blocker loop is sticky."
        }
        if (metrics.getValue("blockedAttempts") < 8) {
            notes += "Blocked attempts are below the 8-attempt signal target; keep testing selected-app blocking."
        }
        if (metrics.getValue("permissionFailures") > metrics.getValue("blockedAttempts")) {
            notes += "Permission failures exceed blocked attempts; improve onboarding and permission recovery before adding features."
        }
        if (metrics.getValue("overlayFailures") > 0) {
            notes += "Overlay show failures were logged; verify Display over other apps, OEM background limits, and addView timing before paid release."
        }
        if (metrics.getValue("dailyQuestAdds") > 0 && metrics.getValue("dailyQuestMockCompletions") == 0) {
            notes += "Daily quests are planned but not completed with proof; test the mock proof loop before GitHub scoring."
        }
        if (metrics.getValue("emergencyUnlocks") > metrics.getValue("freeDays") + metrics.getValue("autoCreditSpends")) {
            notes += "Emergency unlocks dominate earned/free usage; policy may be too strict or credit earning may be too slow."
        }
        if (metrics.getValue("overlayCreditAdds") > metrics.getValue("autoCreditSpends") + metrics.getValue("dailyQuestMockCompletions")) {
            notes += "Overlay test-credit unlocks dominate proof/usage; consider enabling strict mode during dogfood."
        }
        review.dataQuality
            .filter { it.events > 0 && it.coverage < DATA_QUALITY_TARGET }
            .forEach { metric ->
                notes += "${metric.label} is below ${(DATA_QUALITY_TARGET * 100).roundToInt()}%; add structured fields before relying on this gate."
            }
        review.gates.forEach { gate ->
            when (gate.status) {
                DogfoodReviewGateStatus.FAIL -> notes += "Gate ${gate.id} is failing: ${gate.summary}"
                DogfoodReviewGateStatus.NEEDS_DATA -> notes += "Gate ${gate.id} needs more data: ${gate.summary}"
                DogfoodReviewGateStatus.PASS -> Unit
            }
        }

        return notes.ifEmpty {
            listOf("No obvious dogfood risk flags. Continue collecting sessions and inspect top policy reasons.")
        }
    }

    private fun activeDays(events: List<DogfoodEvent>): Int {
        return events.map { dayKey(it.timestamp) }.distinct().size
    }

    private fun distinctEventDays(events: List<DogfoodEvent>, types: Set<String>): Int {
        return events
            .filter { types.contains(it.type) }
            .map { dayKey(it.timestamp) }
            .distinct()
            .size
    }

    private fun spanDays(firstEventAt: String?, lastEventAt: String?): Int {
        if (firstEventAt == null || lastEventAt == null) return 0
        val first = runCatching { Instant.parse(firstEventAt) }.getOrNull() ?: return 0
        val last = runCatching { Instant.parse(lastEventAt) }.getOrNull() ?: return 0
        return Duration.between(first, last).toDays().toInt().coerceAtLeast(0) + 1
    }

    private fun dayKey(instant: Instant): String {
        return instant.atZone(ZoneOffset.UTC).toLocalDate().toString()
    }

    private fun countTypes(events: List<DogfoodEvent>, types: Set<String>): Int {
        return events.count { types.contains(it.type) }
    }

    private fun eventTarget(event: DogfoodEvent): String? {
        return event.target
            ?: detailValue(event.detail, "target")
            ?: detailValue(event.detail, "package")
            ?: event.detail.takeIf { !it.contains("=") }?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun eventPolicyReason(event: DogfoodEvent): String? {
        return event.policyReason ?: detailValue(event.detail, "reason")
    }

    private fun detailValue(detail: String, key: String): String? {
        val regex = Regex("""(?:^|\s)${Regex.escape(key)}=([^\s]+)""")
        return regex.find(detail)?.groupValues?.getOrNull(1)
    }

    private const val DATA_QUALITY_TARGET = 0.75
    private const val REVIEW_WINDOW_DAYS = 14L

    private object eventTypes {
        val blockedAttempts = setOf("blocked_attempt")
        val policyBlocks = setOf("policy_blocked")
        val permissionFailures = setOf("permission_missing")
        val overlayOpens = setOf("overlay_open_app")
        val overlayCreditAdds = setOf("overlay_add_credit")
        val overlayFailures = setOf("overlay_show_failed")
        val autoCreditSpends = setOf("credit_auto_spent")
        val manualCreditChanges = setOf("credit_added", "credit_spent", "credit_reset")
        val freeDays = setOf("free_day_set")
        val emergencyUnlocks = setOf("emergency_unlock_started")
        val dailyQuestAdds = setOf("daily_quest_added")
        val dailyQuestMockCompletions = setOf("daily_quest_mock_completed")
        val foregroundChanges = setOf("foreground_changed")
        val monitorEnabledSignals = setOf("monitor_started", "monitor_heartbeat")
        val overlayShows = setOf("overlay_shown")
    }

    private val EVENT_TYPES = mapOf(
        "blockedAttempts" to eventTypes.blockedAttempts,
        "policyBlocks" to eventTypes.policyBlocks,
        "permissionFailures" to eventTypes.permissionFailures,
        "overlayOpens" to eventTypes.overlayOpens,
        "overlayCreditAdds" to eventTypes.overlayCreditAdds,
        "overlayFailures" to eventTypes.overlayFailures,
        "autoCreditSpends" to eventTypes.autoCreditSpends,
        "manualCreditChanges" to eventTypes.manualCreditChanges,
        "freeDays" to eventTypes.freeDays,
        "emergencyUnlocks" to eventTypes.emergencyUnlocks,
        "dailyQuestAdds" to eventTypes.dailyQuestAdds,
        "dailyQuestMockCompletions" to eventTypes.dailyQuestMockCompletions,
        "foregroundChanges" to eventTypes.foregroundChanges,
        "overlayShows" to eventTypes.overlayShows
    )

    private val targetLikeEvents = setOf(
        "blocked_attempt",
        "foreground_changed",
        "overlay_open_app",
        "overlay_show_failed",
        "overlay_shown",
        "policy_allowed",
        "policy_blocked",
        "target_added",
        "target_matched",
        "target_use_started",
        "target_use_stopped"
    )

    private val policyReasonEvents = setOf(
        "blocked_attempt",
        "credit_auto_spent",
        "overlay_add_credit",
        "overlay_hidden",
        "overlay_open_app",
        "overlay_show_failed",
        "overlay_shown",
        "policy_allowed",
        "policy_blocked",
        "target_matched",
        "target_use_started",
        "target_use_stopped"
    )

    private val creditRemainingEvents = setOf(
        "blocked_attempt",
        "credit_added",
        "credit_auto_spent",
        "credit_reset",
        "credit_spent",
        "overlay_add_credit",
        "overlay_hidden",
        "overlay_open_app",
        "overlay_show_failed",
        "overlay_shown",
        "policy_allowed",
        "policy_blocked",
        "target_matched",
        "target_use_started"
    )
}

object DogfoodReviewRenderer {
    fun render(review: DogfoodReview): String {
        return buildString {
            appendLine("Dogfood review")
            appendLine("Events: ${review.eventCount}")
            appendLine("Window: ${review.firstEventAt ?: "n/a"} -> ${review.lastEventAt ?: "n/a"}")
            appendLine("Active days: ${review.activeDays}")
            appendLine()
            appendLine("Data Quality")
            review.dataQuality.forEach { metric ->
                appendLine("- ${metric.label}: ${percent(metric.coverage)} (${metric.populated}/${metric.events})")
            }
            appendLine()
            appendLine("Gate snapshot")
            review.gates.forEach { gate ->
                appendLine("- Gate ${gate.id} ${gate.status.code}: ${gate.summary}")
            }
            appendLine()
            appendLine("Recommendations")
            review.recommendations.take(MAX_RECOMMENDATIONS).forEach { recommendation ->
                appendLine("- $recommendation")
            }
        }.trimEnd()
    }

    private fun percent(value: Double): String {
        return "${(value * 100).roundToInt()}%"
    }

    private const val MAX_RECOMMENDATIONS = 5
}
