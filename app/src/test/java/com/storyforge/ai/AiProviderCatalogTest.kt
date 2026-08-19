package com.storyforge.ai

import com.storyforge.ai.data.ai.AiProviderCatalog
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProviderCatalogTest {
    @Test
    fun catalogContainsMajorProviders() {
        val ids = AiProviderCatalog.presets.map { it.id }
        assertTrue(ids.containsAll(listOf("openai", "gemini", "xai", "deepseek", "mistral", "groq", "together", "openrouter", "custom")))
    }

    @Test
    fun everyCuratedProviderHasUsefulModelsOrIsCustom() {
        AiProviderCatalog.presets
            .filter { it.id != AiProviderCatalog.CUSTOM }
            .forEach { preset -> assertTrue("${preset.id} should have models", preset.models.isNotEmpty()) }
    }

    @Test
    fun currentModelIsPreservedForCustomProvider() {
        val options = AiProviderCatalog.options(AiProviderCatalog.CUSTOM, "my-private-model")
        assertTrue(options.contains("my-private-model"))
    }
}
