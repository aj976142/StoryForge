package com.storyforge.ai

import com.storyforge.ai.util.TextMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class TextMetricsTest {
    @Test
    fun emptyTextHasZeroWords() {
        assertEquals(0, TextMetrics.words("   "))
        assertEquals(0, TextMetrics.characters(""))
    }

    @Test
    fun countsWordsAndCharacters() {
        val text = "hello brave world"
        assertEquals(3, TextMetrics.words(text))
        assertEquals(17, TextMetrics.characters(text))
    }
}
