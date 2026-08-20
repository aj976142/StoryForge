package com.storyforge.ai.data.ai

/**
 * Curated provider/model presets for StoryForge.
 *
 * The catalog is intentionally editable at runtime through Settings. Providers that expose
 * OpenAI-compatible chat completions can be used directly. OpenRouter is included as the
 * universal gateway for models from labs that use different native APIs (Claude, Gemini,
 * DeepSeek, Qwen, Llama, Mistral, Grok, etc.).
 */
data class AiProviderPreset(
    val id: String,
    val name: String,
    val endpoint: String,
    val models: List<String>,
    val note: String = ""
)

object AiProviderCatalog {
    const val CUSTOM = "custom"
    const val MOCK = "mock"

    val presets: List<AiProviderPreset> = listOf(
        AiProviderPreset(
            id = "openai",
            name = "OpenAI",
            endpoint = "https://api.openai.com/v1/chat/completions",
            models = listOf(
                "gpt-5.6",
                "gpt-5.6-sol",
                "gpt-5.6-terra",
                "gpt-5.6-luna",
                "gpt-5.5",
                "gpt-5.5-pro",
                "gpt-5.4",
                "gpt-5.4-mini",
                "gpt-5.4-nano",
                "gpt-4.1"
            )
        ),
        AiProviderPreset(
            id = "gemini",
            name = "Google Gemini",
            endpoint = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
            models = listOf(
                "gemini-3.6-flash",
                "gemini-3.5-flash",
                "gemini-3.5-flash-lite",
                "gemini-3.1-flash-lite",
                "gemini-3.1-pro-preview",
                "gemini-3-flash-preview"
            )
        ),
        AiProviderPreset(
            id = "xai",
            name = "xAI / Grok",
            endpoint = "https://api.x.ai/v1/chat/completions",
            models = listOf(
                "grok-4.5",
                "grok-4.1",
                "grok-4.1-fast",
                "grok-4.20",
                "grok-latest"
            )
        ),
        AiProviderPreset(
            id = "deepseek",
            name = "DeepSeek",
            endpoint = "https://api.deepseek.com/chat/completions",
            models = listOf(
                "deepseek-chat",
                "deepseek-reasoner",
                "deepseek-v3.2"
            )
        ),
        AiProviderPreset(
            id = "mistral",
            name = "Mistral",
            endpoint = "https://api.mistral.ai/v1/chat/completions",
            models = listOf(
                "mistral-medium-3.5",
                "mistral-small-4",
                "mistral-large-3",
                "ministral-3-14b",
                "ministral-3-8b",
                "ministral-3-3b"
            )
        ),
        AiProviderPreset(
            id = "groq",
            name = "Groq",
            endpoint = "https://api.groq.com/openai/v1/chat/completions",
            models = listOf(
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant",
                "openai/gpt-oss-120b",
                "openai/gpt-oss-20b",
                "qwen/qwen3.6-27b",
                "groq/compound"
            )
        ),
        AiProviderPreset(
            id = "together",
            name = "Together AI",
            endpoint = "https://api.together.xyz/v1/chat/completions",
            models = listOf(
                "meta-llama/Llama-4-Maverick",
                "meta-llama/Llama-4-Scout",
                "Qwen/Qwen3-235B-A22B-Instruct",
                "deepseek-ai/DeepSeek-V3",
                "mistralai/Mistral-Small-3.1-24B-Instruct-2503"
            )
        ),
        AiProviderPreset(
            id = "openrouter",
            name = "OpenRouter · Many models",
            endpoint = "https://openrouter.ai/api/v1/chat/completions",
            models = listOf(
                "openai/gpt-5.6",
                "openai/gpt-5.5",
                "openai/gpt-5.4",
                "anthropic/claude-opus-4.8",
                "anthropic/claude-opus-4.7",
                "anthropic/claude-sonnet-4.6",
                "google/gemini-3.1-pro-preview",
                "google/gemini-3-flash-preview",
                "x-ai/grok-4.1",
                "deepseek/deepseek-v3.1",
                "deepseek/deepseek-v3.1-terminus",
                "qwen/qwen3.7-max",
                "qwen/qwen3.5-flash",
                "mistralai/mistral-medium-3.1",
                "meta-llama/llama-3.3-70b-instruct",
                "moonshotai/kimi-k2.6",
                "z-ai/glm-4.5-air",
                "minimax/minimax-m2.7"
            ),
            note = "One API key can access models from many major AI labs."
        ),
        AiProviderPreset(
            id = CUSTOM,
            name = "Custom · OpenAI-compatible",
            endpoint = "",
            models = emptyList(),
            note = "Use any compatible endpoint and enter its model ID."
        )
    )

    fun preset(id: String): AiProviderPreset? = presets.firstOrNull { it.id == id }

    fun options(id: String, currentModel: String): List<String> = buildList {
        preset(id)?.models?.let(::addAll)
        if (currentModel.isNotBlank() && currentModel !in this) add(currentModel)
    }.distinct()
}
