package com.commitunlock.prototype

import kotlin.test.Test
import kotlin.test.assertEquals

class DogfoodExportRedactorTest {
    @Test
    fun redactsPresentTargetsButLeavesEmptyTargetsEmpty() {
        assertEquals("<target:redacted>", DogfoodExportRedactor.target("com.video.app"))
        assertEquals("", DogfoodExportRedactor.target(null))
        assertEquals("", DogfoodExportRedactor.target(""))
    }

    @Test
    fun redactsSensitiveFreeTextDetailValues() {
        assertEquals(
            "id=quest-1 required=true title=<redacted>",
            DogfoodExportRedactor.detail("id=quest-1 required=true title=fix auth on Friday")
        )
        assertEquals(
            "id=unlock-1 minutes=15 reason=<redacted>",
            DogfoodExportRedactor.detail("id=unlock-1 minutes=15 reason=production incident")
        )
    }
}
