package com.storyforge.ai

import com.storyforge.ai.data.ai.GenerationEvent
import com.storyforge.ai.data.ai.GenerationRequest
import com.storyforge.ai.data.ai.MockAiService
import com.storyforge.ai.domain.model.OutputFormat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class MockAiServiceTest {
    @Test
    fun generateIncludesIdeaAndCompletes() = runTest {
        val service = MockAiService()
        val events = service.generate(
            GenerationRequest("a lighthouse that remembers sailors", OutputFormat.SHORT_STORY)
        ).toList()
        val completed = events.filterIsInstance<GenerationEvent.Completed>().single()
        assertTrue(completed.fullText.contains("lighthouse"))
        assertTrue(completed.suggestedTitle.isNotBlank())
    }

    @Test
    fun emptyIdeaFailsWithoutRetryLoop() = runTest {
        val service = MockAiService()
        val events = service.generate(GenerationRequest("   ", OutputFormat.NOVEL)).toList()
        val failed = events.filterIsInstance<GenerationEvent.Failed>().single()
        assertTrue(!failed.retryable)
    }
}
