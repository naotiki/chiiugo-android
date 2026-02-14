package me.naotiki.chiiugo.domain.comment

import org.junit.Assert.assertEquals
import org.junit.Test

class CommentSanitizerTest {
    @Test
    fun `toSingleSentence clips after first punctuation`() {
        assertEquals("おはよう。", "おはよう。今日は元気？".toSingleSentence())
    }

    @Test
    fun `toSingleSentence keeps plain string when no punctuation`() {
        assertEquals("きょうもよろしく", "きょうもよろしく".toSingleSentence())
    }
}
