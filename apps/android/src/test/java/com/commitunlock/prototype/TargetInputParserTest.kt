package com.commitunlock.prototype

import kotlin.test.Test
import kotlin.test.assertEquals

class TargetInputParserTest {
    @Test
    fun parsesCommaNewlineAndWhitespaceSeparatedPackages() {
        assertEquals(
            listOf(
                "com.android.chrome",
                "com.google.android.youtube",
                "com.instagram.android"
            ),
            TargetInputParser.parse(
                "com.android.chrome,\ncom.google.android.youtube  com.instagram.android"
            )
        )
    }

    @Test
    fun ignoresEmptySegments() {
        assertEquals(
            listOf("com.android.chrome"),
            TargetInputParser.parse("  ,  \n com.android.chrome \n\n")
        )
    }
}
