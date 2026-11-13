package com.hucw.skipads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkipMatcherTest {

    private val keywords = listOf("跳过", "跳过广告", "skip", "skip ad")

    @Test
    fun matchesPlainSkip() {
        assertTrue(SkipMatcher.isSkipText("跳过", keywords))
    }

    @Test
    fun matchesWithCountdown() {
        assertTrue(SkipMatcher.isSkipText("跳过 5", keywords))
        assertTrue(SkipMatcher.isSkipText("5s 跳过", keywords))
    }

    @Test
    fun matchesEnglishKeywords() {
        assertTrue(SkipMatcher.isSkipText("Skip", keywords))
        assertTrue(SkipMatcher.isSkipText("SKIP AD", keywords))
    }

    @Test
    fun matchesContentDescription() {
        assertTrue(SkipMatcher.isSkipText("跳过广告", keywords))
    }

    @Test
    fun rejectsNullOrBlank() {
        assertFalse(SkipMatcher.isSkipText(null, keywords))
        assertFalse(SkipMatcher.isSkipText("   ", keywords))
    }

    @Test
    fun rejectsUnrelatedText() {
        assertFalse(SkipMatcher.isSkipText("立即打开", keywords))
        assertFalse(SkipMatcher.isSkipText("同意并继续", keywords))
    }

    @Test
    fun rejectsLongParagraph() {
        assertFalse(SkipMatcher.isSkipText("点击跳过广告并进入应用首页的说明文字", keywords))
    }

    @Test
    fun rejectsWhenKeywordsEmpty() {
        assertFalse(SkipMatcher.isSkipText("跳过", emptyList()))
    }
}
