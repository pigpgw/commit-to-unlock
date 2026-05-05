package com.commitunlock.prototype

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DogfoodEventStoreTest {
    @Test
    fun exportsHeaderAndStructuredColumns() {
        val storage = FakeDogfoodEventStorage()
        val clock = StepClock(Instant.parse("2026-05-04T12:00:00Z"))
        val store = DogfoodEventStore(storage, clock::now)

        store.recordStructured(
            type = "blocked_attempt",
            target = "com.video.app",
            policyReason = "credit_empty",
            creditRemaining = 0,
            detail = "reason=credit_empty"
        )

        val export = store.exportTsv()
        val lines = export.lines()

        assertEquals("timestamp\ttype\ttarget\tpolicy_reason\tcredit_remaining\tdetail", lines[0])
        assertEquals(2, lines.size)
        assertEquals(
            listOf(
                "2026-05-04T12:00:00Z",
                "blocked_attempt",
                "com.video.app",
                "credit_empty",
                "0",
                "reason=credit_empty"
            ),
            lines[1].split("\t")
        )
        assertEquals(export, storage.exports.last())
    }

    @Test
    fun parsesLegacyRowsAndExportsCurrentTsvShape() {
        val storage = FakeDogfoodEventStorage(
            listOf("2026-05-04T12:00:00Z\tlegacy_event\tlegacy detail")
        )
        val store = DogfoodEventStore(storage)

        val event = store.read().single()

        assertEquals("legacy_event", event.type)
        assertNull(event.target)
        assertNull(event.policyReason)
        assertNull(event.creditRemaining)
        assertEquals("legacy detail", event.detail)

        val row = store.exportTsv().lines()[1].split("\t")
        assertEquals(6, row.size)
        assertEquals("legacy detail", row[5])
    }

    @Test
    fun sanitizesTabsNewlinesAndNegativeCreditBeforeSaving() {
        val storage = FakeDogfoodEventStorage()
        val clock = StepClock(Instant.parse("2026-05-04T12:00:00Z"))
        val store = DogfoodEventStore(storage, clock::now)

        store.recordStructured(
            type = " blocked\tattempt\n ",
            target = " com.video\tapp ",
            policyReason = " credit\nempty ",
            creditRemaining = -3,
            detail = " hello\tworld\nagain "
        )

        val row = store.exportTsv().lines()[1].split("\t")

        assertEquals(6, row.size)
        assertEquals("blocked attempt", row[1])
        assertEquals("com.video app", row[2])
        assertEquals("credit empty", row[3])
        assertEquals("0", row[4])
        assertEquals("hello world again", row[5])
    }

    @Test
    fun keepsOnlyTheMostRecentOneThousandEvents() {
        val storage = FakeDogfoodEventStorage()
        val clock = StepClock(Instant.parse("2026-05-04T12:00:00Z"))
        val store = DogfoodEventStore(storage, clock::now)

        repeat(1_005) { index ->
            store.recordStructured(type = "foreground_changed", detail = "event-$index")
        }

        assertEquals(1_000, store.read().size)

        val exportRows = store.exportTsv().lines().drop(1)
        assertEquals(1_000, exportRows.size)
        assertEquals("event-5", exportRows.first().split("\t")[5])
        assertEquals("event-1004", exportRows.last().split("\t")[5])
    }

    @Test
    fun dropsAdjacentDuplicatesButKeepsNonAdjacentRepeats() {
        val storage = FakeDogfoodEventStorage()
        val clock = StepClock(Instant.parse("2026-05-04T12:00:00Z"))
        val store = DogfoodEventStore(storage, clock::now)

        store.recordStructured(type = "permission_missing", detail = "usage")
        store.recordStructured(type = "permission_missing", detail = "usage")
        store.recordStructured(type = "permission_missing", detail = "overlay")
        store.recordStructured(type = "permission_missing", detail = "usage")

        val details = store.read().map { it.detail }

        assertEquals(3, details.size)
        assertEquals(2, details.count { it == "usage" })
        assertEquals(1, details.count { it == "overlay" })
    }

    @Test
    fun summaryCountsOverlayShowFailures() {
        val storage = FakeDogfoodEventStorage()
        val clock = StepClock(Instant.parse("2026-05-04T12:00:00Z"))
        val store = DogfoodEventStore(storage, clock::now)

        store.recordStructured(
            type = "overlay_show_failed",
            target = "com.video.app",
            policyReason = "credit_empty",
            creditRemaining = 0,
            detail = "add_view_failed"
        )

        val summary = store.summary(Instant.parse("2026-05-04T12:00:01Z"))

        assertEquals(1, summary.overlayFailures)
    }

    @Test
    fun summaryCountsRuntimeFailures() {
        val storage = FakeDogfoodEventStorage()
        val clock = StepClock(Instant.parse("2026-05-04T12:00:00Z"))
        val store = DogfoodEventStore(storage, clock::now)

        store.record("settings_open_failed", "usage_access ActivityNotFoundException")
        store.record("dogfood_export_share_failed", "ActivityNotFoundException")

        val summary = store.summary(Instant.parse("2026-05-04T12:00:01Z"))

        assertEquals(2, summary.runtimeFailures)
    }

    @Test
    fun redactedExportHidesTargetsAndSensitiveDetails() {
        val storage = FakeDogfoodEventStorage()
        val clock = StepClock(Instant.parse("2026-05-04T12:00:00Z"))
        val store = DogfoodEventStore(storage, clock::now)

        store.recordStructured(
            type = "daily_quest_added",
            target = "com.video.app",
            policyReason = "credit_empty",
            creditRemaining = 0,
            detail = "id=quest-1 required=true title=fix secret repo issue"
        )

        val row = store.exportTsv(redactSensitive = true).lines()[1].split("\t")

        assertEquals("<target:redacted>", row[2])
        assertEquals("credit_empty", row[3])
        assertEquals("id=quest-1 required=true title=<redacted>", row[5])
    }

    @Test
    fun recordSurvivesExportFileWriteFailure() {
        val storage = FakeDogfoodEventStorage(writeExportSucceeds = false)
        val clock = StepClock(Instant.parse("2026-05-04T12:00:00Z"))
        val store = DogfoodEventStore(storage, clock::now)

        store.recordStructured(
            type = "monitor_start_failed",
            detail = "ForegroundServiceStartNotAllowedException"
        )

        assertEquals("monitor_start_failed", store.read().single().type)
        assertEquals(emptyList(), storage.exports)
    }

    @Test
    fun recordDoesNotExportWhenRawWriteFails() {
        val storage = FakeDogfoodEventStorage(writeRawSucceeds = false)
        val clock = StepClock(Instant.parse("2026-05-04T12:00:00Z"))
        val store = DogfoodEventStore(storage, clock::now)

        store.recordStructured(type = "monitor_started")

        assertEquals(emptyList(), store.read())
        assertEquals(1, storage.exports.size)
    }
}

private class FakeDogfoodEventStorage(
    initialRaw: List<String> = emptyList(),
    private val writeRawSucceeds: Boolean = true,
    private val writeExportSucceeds: Boolean = true
) : DogfoodEventStorage {
    private var raw = initialRaw
    val exports = mutableListOf<String>()

    override fun readRaw(): List<String> {
        return raw.toList()
    }

    override fun writeRaw(events: List<String>): Boolean {
        if (!writeRawSucceeds) return false
        raw = events.toList()
        return true
    }

    override fun writeExport(export: String): Boolean {
        if (!writeExportSucceeds) return false
        exports += export
        return true
    }
}

private class StepClock(start: Instant) {
    private var current = start

    fun now(): Instant {
        return current.also {
            current = current.plusSeconds(1)
        }
    }
}
