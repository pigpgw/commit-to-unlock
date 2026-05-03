package com.commitunlock.prototype

import kotlin.test.Test
import kotlin.test.assertEquals

class TimeInputParserTest {
    @Test
    fun normalizesBlankAndValidTimes() {
        assertEquals(TimeInputValue.Blank, TimeInputParser.normalize("  "))
        assertEquals(TimeInputValue.Valid("09:05"), TimeInputParser.normalize("9:05"))
        assertEquals(TimeInputValue.Valid("23:59"), TimeInputParser.normalize("23:59"))
    }

    @Test
    fun rejectsMalformedOrOutOfRangeTimes() {
        assertEquals(TimeInputValue.Invalid, TimeInputParser.normalize("24:00"))
        assertEquals(TimeInputValue.Invalid, TimeInputParser.normalize("12:60"))
        assertEquals(TimeInputValue.Invalid, TimeInputParser.normalize("nope"))
        assertEquals(TimeInputValue.Invalid, TimeInputParser.normalize("9:5"))
    }
}
