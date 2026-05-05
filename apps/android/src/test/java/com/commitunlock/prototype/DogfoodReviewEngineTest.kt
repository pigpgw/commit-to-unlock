package com.commitunlock.prototype

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DogfoodReviewEngineTest {
    @Test
    fun emptyEventsNeedDogfoodData() {
        val review = analyze(emptyList())

        assertEquals(0, review.eventCount)
        assertEquals(0, review.activeDays)
        assertEquals(
            listOf(1.0, 1.0, 1.0),
            review.dataQuality.map { it.coverage }
        )
        assertEquals(
            listOf(
                DogfoodReviewGateStatus.NEEDS_DATA,
                DogfoodReviewGateStatus.NEEDS_DATA,
                DogfoodReviewGateStatus.NEEDS_DATA,
                DogfoodReviewGateStatus.NEEDS_DATA
            ),
            review.gates.map { it.status }
        )
        assertEquals(
            "No events found. Run a device dogfood session before making product calls.",
            review.recommendations.single()
        )
    }

    @Test
    fun calculatesStructuredDataQualityCoverage() {
        val events = listOf(
            event("foreground_changed", target = "com.video.app"),
            event(
                "blocked_attempt",
                detail = "target=com.video.app reason=credit_empty",
                creditRemaining = 0
            ),
            event(
                "policy_blocked",
                target = "com.video.app",
                policyReason = "credit_empty",
                creditRemaining = 0
            ),
            event("overlay_shown", target = "com.video.app", policyReason = "credit_empty")
        )

        val review = analyze(events)
        val metrics = review.dataQuality.associateBy { it.label }

        assertEquals(4, metrics.getValue("target coverage").events)
        assertEquals(4, metrics.getValue("target coverage").populated)
        assertEquals(1.0, metrics.getValue("target coverage").coverage)
        assertEquals(3, metrics.getValue("policy reason coverage").events)
        assertEquals(3, metrics.getValue("policy reason coverage").populated)
        assertEquals(1.0, metrics.getValue("policy reason coverage").coverage)
        assertEquals(3, metrics.getValue("credit remaining coverage").events)
        assertEquals(2, metrics.getValue("credit remaining coverage").populated)
        assertEquals(0.667, metrics.getValue("credit remaining coverage").coverage)
    }

    @Test
    fun analyzesOnlyRecentFourteenDayWindow() {
        val events = listOf(
            event(
                "blocked_attempt",
                target = "com.old.app",
                policyReason = "credit_empty",
                creditRemaining = 0
            ),
            event("foreground_changed", dayOffset = 20, target = "com.current.app")
        )

        val review = analyze(events)

        assertEquals(1, review.eventCount)
        assertEquals(0, review.metrics.getValue("blockedAttempts"))
        assertEquals(1, review.metrics.getValue("foregroundChanges"))
    }

    @Test
    fun gateAPassesWhenForegroundAndOverlaySignalsExist() {
        val events = listOf(
            event("foreground_changed", target = "com.video.app"),
            event("overlay_shown", target = "com.video.app", policyReason = "credit_empty", creditRemaining = 0),
            event("blocked_attempt", target = "com.video.app", policyReason = "credit_empty", creditRemaining = 0)
        )

        val gateA = analyze(events)
            .gates
            .single { it.id == "A" }

        assertEquals(DogfoodReviewGateStatus.PASS, gateA.status)
        assertTrue(gateA.checks.all { it.passed })
    }

    @Test
    fun gateAFailsWhenOverlayShowFailsAfterTargetMatch() {
        val events = listOf(
            event("foreground_changed", target = "com.video.app"),
            event("blocked_attempt", target = "com.video.app", policyReason = "credit_empty", creditRemaining = 0),
            event("overlay_show_failed", target = "com.video.app", policyReason = "credit_empty", creditRemaining = 0)
        )

        val review = analyze(events)
        val gateA = review.gates.single { it.id == "A" }

        assertEquals(DogfoodReviewGateStatus.FAIL, gateA.status)
        assertTrue(gateA.checks.any { it.label == "Overlay show failures" && !it.passed })
        assertTrue(review.recommendations.any { it.contains("Overlay show failures") })
    }

    @Test
    fun gateAFailsWhenRuntimeFailuresAreLogged() {
        val events = listOf(
            event("foreground_changed", target = "com.video.app"),
            event("overlay_shown", target = "com.video.app", policyReason = "credit_empty", creditRemaining = 0),
            event("blocked_attempt", target = "com.video.app", policyReason = "credit_empty", creditRemaining = 0),
            event("monitor_start_failed", detail = "ForegroundServiceStartNotAllowedException")
        )

        val review = analyze(events)
        val gateA = review.gates.single { it.id == "A" }

        assertEquals(DogfoodReviewGateStatus.FAIL, gateA.status)
        assertEquals(1, review.metrics.getValue("runtimeFailures"))
        assertTrue(gateA.checks.any { it.label == "Runtime failure events" && !it.passed })
        assertTrue(review.recommendations.any { it.contains("Runtime failures") })
    }

    @Test
    fun gateBPassesWithEightMonitorDaysAndBlockedAttempts() {
        val events = (0L until 8L).flatMap { day ->
            listOf(
                event("monitor_started", dayOffset = day),
                event(
                    "blocked_attempt",
                    dayOffset = day,
                    target = "com.video.app",
                    policyReason = "credit_empty",
                    creditRemaining = 0
                )
            )
        }

        val gateB = analyze(events)
            .gates
            .single { it.id == "B" }

        assertEquals(DogfoodReviewGateStatus.PASS, gateB.status)
    }

    @Test
    fun gateCPassesWithFiveMockProofCompletions() {
        val events = (0L until 5L).map { day ->
            event("daily_quest_mock_completed", dayOffset = day, detail = "id=quest-$day required=true")
        }

        val gateC = analyze(events)
            .gates
            .single { it.id == "C" }

        assertEquals(DogfoodReviewGateStatus.PASS, gateC.status)
        assertTrue(gateC.summary.contains("requires real GitHub"))
    }

    @Test
    fun gateDFailsWhenStructuredFieldsAreSparse() {
        val events = listOf(
            event("policy_blocked"),
            event("blocked_attempt", detail = "unstructured")
        )

        val gateD = analyze(events)
            .gates
            .single { it.id == "D" }

        assertEquals(DogfoodReviewGateStatus.FAIL, gateD.status)
        assertTrue(gateD.checks.any { !it.passed })
    }

    @Test
    fun rendererShowsDataQualityAndGateSummary() {
        val events = listOf(
            event("foreground_changed", target = "com.video.app"),
            event("overlay_shown", target = "com.video.app", policyReason = "credit_empty", creditRemaining = 0),
            event("blocked_attempt", target = "com.video.app", policyReason = "credit_empty", creditRemaining = 0)
        )

        val rendered = DogfoodReviewRenderer.render(analyze(events))

        assertTrue(rendered.contains("Dogfood review"))
        assertTrue(rendered.contains("Data Quality"))
        assertTrue(rendered.contains("Gate A pass"))
        assertTrue(rendered.contains("Recommendations"))
    }

    private fun event(
        type: String,
        dayOffset: Long = 0,
        target: String? = null,
        policyReason: String? = null,
        creditRemaining: Int? = null,
        detail: String = ""
    ): DogfoodEvent {
        return DogfoodEvent(
            timestamp = BASE_INSTANT.plus(Duration.ofDays(dayOffset)),
            type = type,
            target = target,
            policyReason = policyReason,
            creditRemaining = creditRemaining,
            detail = detail
        )
    }

    private fun analyze(events: List<DogfoodEvent>): DogfoodReview {
        val now = events.maxOfOrNull { it.timestamp } ?: BASE_INSTANT
        return DogfoodReviewEngine.analyze(events, summary(events), now)
    }

    private fun summary(events: List<DogfoodEvent>): DogfoodSummary {
        return DogfoodSummary(
            monitorEnabledDays = events
                .filter { it.type == "monitor_started" || it.type == "monitor_heartbeat" }
                .map { it.timestamp.atZone(ZoneOffset.UTC).toLocalDate() }
                .distinct()
                .size,
            blockedAttempts = events.count { it.type == "blocked_attempt" },
            policyBlocks = events.count { it.type == "policy_blocked" },
            emergencyUnlocks = events.count { it.type == "emergency_unlock_started" },
            freeDays = events.count { it.type == "free_day_set" },
            dailyQuestsAdded = events.count { it.type == "daily_quest_added" },
            dailyQuestMockCompletions = events.count { it.type == "daily_quest_mock_completed" },
            permissionFailures = events.count { it.type == "permission_missing" },
            overlayOpens = events.count { it.type == "overlay_open_app" },
            overlayCreditAdds = events.count { it.type == "overlay_add_credit" },
            overlayFailures = events.count { it.type == "overlay_show_failed" },
            runtimeFailures = events.count {
                it.type == "settings_open_failed" ||
                    it.type == "dogfood_export_share_failed" ||
                    it.type == "monitor_start_failed" ||
                    it.type == "monitor_stop_failed" ||
                    it.type == "notification_permission_request_failed" ||
                    it.type == "open_main_failed"
            },
            automaticCreditSpends = events.count { it.type == "credit_auto_spent" },
            manualCreditChanges = events.count {
                it.type == "credit_added" || it.type == "credit_spent" || it.type == "credit_reset"
            },
            eventCount = events.size
        )
    }

    private companion object {
        val BASE_INSTANT: Instant = Instant.parse("2026-05-04T12:00:00Z")
    }
}
