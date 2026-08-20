package com.storyforge.ai.data.ai

import com.storyforge.ai.domain.model.OutputFormat
import com.storyforge.ai.domain.model.StoryBible
import com.storyforge.ai.domain.model.WritingPreferences
import kotlinx.coroutines.flow.Flow

/** Provider-agnostic AI contract for drafting and focused editing actions. */
interface AiService {
    val providerId: String
    val displayName: String

    fun generate(request: GenerationRequest): Flow<GenerationEvent>
    fun continueWriting(request: ContinueRequest): Flow<GenerationEvent>
}

data class GenerationRequest(
    val idea: String,
    val format: OutputFormat,
    val preferences: WritingPreferences = WritingPreferences(),
    val existingText: String? = null,
    /** Optional focused instruction such as polish, expand, shorten or screenplay conversion. */
    val instruction: String? = null,
    /** Continuity memory owned by the current project only. */
    val storyBible: StoryBible = StoryBible()
)

data class ContinueRequest(
    val idea: String,
    val format: OutputFormat,
    val currentText: String,
    val preferences: WritingPreferences = WritingPreferences(),
    val instruction: String? = null,
    /** Continuity memory owned by the current project only. */
    val storyBible: StoryBible = StoryBible()
)

sealed class GenerationEvent {
    data class Progress(val percent: Int, val stage: String) : GenerationEvent()
    data class Chunk(val text: String) : GenerationEvent()
    data class Completed(val fullText: String, val suggestedTitle: String) : GenerationEvent()
    data class Failed(val message: String, val retryable: Boolean = true) : GenerationEvent()
}
