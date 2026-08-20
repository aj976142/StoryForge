package com.storyforge.ai.data.ai

import com.storyforge.ai.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/** Routes requests to demo mode or the user's configured provider without exposing credentials to UI. */
class ConfiguredAiService(
    private val settings: SettingsRepository,
    private val real: AiService,
    private val demo: AiService
) : AiService {
    override val providerId = "configured"
    override val displayName = "Configured StoryForge AI"

    override fun generate(request: GenerationRequest): Flow<GenerationEvent> = flow {
        val service = if (settings.ai.first().provider == "mock") demo else real
        service.generate(request).collect { emit(it) }
    }

    override fun continueWriting(request: ContinueRequest): Flow<GenerationEvent> = flow {
        val service = if (settings.ai.first().provider == "mock") demo else real
        service.continueWriting(request).collect { emit(it) }
    }
}
