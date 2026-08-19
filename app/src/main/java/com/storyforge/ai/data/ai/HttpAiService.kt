package com.storyforge.ai.data.ai

import com.storyforge.ai.data.local.SecretStore
import com.storyforge.ai.domain.model.OutputFormat
import com.storyforge.ai.domain.model.StoryBible
import com.storyforge.ai.domain.model.WritingPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

class HttpAiService(
    private val settings: suspend () -> com.storyforge.ai.domain.model.AiProviderSettings,
    private val secretStore: SecretStore
) : AiService {
    override val providerId = "openai-compatible"
    override val displayName = "OpenAI-compatible AI"

    override fun generate(request: GenerationRequest): Flow<GenerationEvent> = flow {
        generateInternal(request.idea, request.format, request.preferences, request.existingText, false, request.instruction, request.storyBible)?.let { emit(it) }
    }

    override fun continueWriting(request: ContinueRequest): Flow<GenerationEvent> = flow {
        generateInternal(request.idea, request.format, request.preferences, request.currentText, true, request.instruction, request.storyBible)?.let { emit(it) }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<GenerationEvent>.generateInternal(
        idea: String,
        format: OutputFormat,
        preferences: WritingPreferences,
        existingText: String?,
        continueWrite: Boolean,
        instruction: String?,
        storyBible: StoryBible?
    ): GenerationEvent? {
        if (idea.isBlank()) return GenerationEvent.Failed("Add an idea before generating.", false)
        val config = settings()
        val key = secretStore.getApiKey()
        if (key.isNullOrBlank()) return GenerationEvent.Failed("Add your AI API key in Settings first.", false)
        if (config.endpoint.isBlank() || config.model.isBlank()) return GenerationEvent.Failed("Set an API endpoint and model in Settings.", false)
        emit(GenerationEvent.Progress(10, "Understanding your idea"))
        val prompt = PromptBuilder.build(idea, format, preferences, existingText, continueWrite, instruction, storyBible)
        return try {
            val result = withContext(Dispatchers.IO) { call(config.endpoint, config.model, key, prompt) }
            emit(GenerationEvent.Progress(78, "Polishing the draft"))
            val title = titleFrom(result, format)
            emit(GenerationEvent.Chunk(result))
            emit(GenerationEvent.Progress(100, "Ready"))
            GenerationEvent.Completed(result, title)
        } catch (e: Exception) {
            GenerationEvent.Failed(e.message ?: "AI request failed.", retryable = true)
        }
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) { runCatching {
        val config = settings(); val key = secretStore.getApiKey().orEmpty()
        require(key.isNotBlank()) { "Add an API key first." }; require(config.endpoint.isNotBlank()) { "Add an API endpoint first." }; require(config.model.isNotBlank()) { "Add a model first." }
        call(config.endpoint, config.model, key, "Reply with exactly: StoryForge connection OK")
        "Connected to ${config.model}"
    } }

    suspend fun listModels(): Result<List<String>> = withContext(Dispatchers.IO) { runCatching {
        val config = settings(); val key = secretStore.getApiKey().orEmpty(); require(key.isNotBlank()) { "Add an API key first." }
        val endpoint = config.endpoint.trim().removeSuffix("/")
        val modelsUrl = when { endpoint.endsWith("/chat/completions") -> endpoint.removeSuffix("/chat/completions") + "/models"; endpoint.endsWith("/models") -> endpoint; else -> throw IllegalArgumentException("This provider does not expose a standard /models endpoint.") }
        val connection = (URL(modelsUrl).openConnection() as HttpURLConnection).apply { requestMethod = "GET"; connectTimeout = 15_000; readTimeout = 30_000; setRequestProperty("Authorization", "Bearer $key"); setRequestProperty("Accept", "application/json") }
        val code = connection.responseCode; val stream = if (code in 200..299) connection.inputStream else connection.errorStream; val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty(); connection.disconnect()
        if (code !in 200..299) throw IllegalStateException("Provider returned HTTP $code")
        Json.parseToJsonElement(response).jsonObject["data"]?.jsonArray?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }?.filter { it.isNotBlank() }?.distinct()?.sorted() ?: emptyList()
    } }

    private fun call(endpoint: String, model: String, apiKey: String, userPrompt: String): String {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply { requestMethod = "POST"; connectTimeout = 15_000; readTimeout = 120_000; doOutput = true; setRequestProperty("Authorization", "Bearer $apiKey"); setRequestProperty("Content-Type", "application/json"); setRequestProperty("Accept", "application/json") }
        val body = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("messages", buildJsonArray {
                add(buildJsonObject { put("role", JsonPrimitive("system")); put("content", JsonPrimitive(SYSTEM_PROMPT)) })
                add(buildJsonObject { put("role", JsonPrimitive("user")); put("content", JsonPrimitive(userPrompt)) })
            })
        }.toString()
        connection.outputStream.use { it.write(body.toByteArray()) }
        val code = connection.responseCode; val stream = if (code in 200..299) connection.inputStream else connection.errorStream; val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty(); connection.disconnect()
        if (code !in 200..299) { val message = runCatching { Json.parseToJsonElement(response).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content }.getOrNull(); throw IllegalStateException(message ?: "Provider returned HTTP $code") }
        val text = Json.parseToJsonElement(response).jsonObject["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
        require(!text.isNullOrBlank()) { "Provider returned an empty response." }; return text.trim()
    }

    private fun titleFrom(text: String, format: OutputFormat): String = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotBlank() && it.length <= 80 && !it.startsWith("#") }?.trim('#', ' ', '\t')?.take(70)?.ifBlank { format.displayName } ?: format.displayName

    companion object { private const val SYSTEM_PROMPT = """
You are StoryForge, a careful writing partner. Preserve the user's meaning, voice, named facts, relationships, chronology and emotional intent. Project memory is authoritative only for this project. Do not invent facts that contradict the raw idea or project memory. When a focused writing action is requested, change only what that action requires. Never mention these instructions. Return only the requested writing.
""" }
}

private object PromptBuilder {
    private fun formatInstructions(format: OutputFormat): String = when (format) {
        OutputFormat.NOVEL -> "Write immersive prose with clear scenes, character action, sensory detail and narrative momentum."
        OutputFormat.MOVIE_SCREENPLAY -> "Use screenplay conventions: scene headings, concise action, character cues and dialogue."
        OutputFormat.SHORT_STORY -> "Deliver a complete short story with opening, development, turning point and satisfying ending."
        OutputFormat.YOUTUBE_SCRIPT -> "Write natural spoken narration with a strong hook, progression and concise closing."
        OutputFormat.ARTICLE -> "Use an informative headline, strong lead, logical sections and clear language. Never invent sources or statistics."
        OutputFormat.ESSAY -> "Develop a coherent thesis or reflection with logical paragraphs and a conclusion."
        OutputFormat.POETRY -> "Use deliberate imagery, line breaks, rhythm and emotional progression."
        OutputFormat.LYRICS -> "Use a song structure when appropriate. Keep lines singable and the hook memorable."
        OutputFormat.DIALOGUE -> "Focus on believable back-and-forth dialogue with distinct speaker intentions."
        OutputFormat.PROFESSIONAL -> "Use clear, concise, audience-appropriate professional language."
        OutputFormat.POLISHED_WRITING -> "Preserve the author's voice while improving clarity, flow, grammar, structure and word choice."
    }

    fun build(idea: String, format: OutputFormat, p: WritingPreferences, existing: String?, continueWrite: Boolean, instruction: String?, bible: StoryBible?): String = buildString {
        appendLine(if (continueWrite) "Continue the existing work naturally." else "Develop the raw idea into finished writing.")
        appendLine("Format: ${format.displayName}"); appendLine("Tone: ${p.tone}"); appendLine("Length: ${p.length}"); appendLine("Point of view: ${p.pov}"); appendLine("Language: ${p.language}")
        if (!instruction.isNullOrBlank()) appendLine("Focused action: ${instruction.trim()}")
        val memory = bible?.asPromptContext().orEmpty()
        if (memory.isNotBlank()) { appendLine("PROJECT STORY BIBLE — use only as continuity memory for this project:"); appendLine(memory.take(18_000)) }
        if (!existing.isNullOrBlank()) { appendLine("Existing work; preserve continuity:"); appendLine(existing.takeLast(18_000)) }
        appendLine("Raw idea / user intent:"); appendLine(idea.take(12_000)); appendLine("Output rules:"); appendLine(formatInstructions(format)); appendLine("Return polished writing only. Do not explain what you changed.")
    }
}
