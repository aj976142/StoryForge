package com.storyforge.ai.data.ai

import com.storyforge.ai.domain.model.OutputFormat
import com.storyforge.ai.domain.model.WritingPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Real OpenAI-compatible AI implementation. Keep provider keys out of source control. */
class HttpAiService(
    private val apiKey: String,
    private val endpoint: String = "https://api.openai.com/v1/chat/completions",
    private val model: String = "gpt-4o-mini"
) : AiService {
    override val providerId = "http-openai-compatible"
    override val displayName = "AI Writer"

    override fun generate(request: GenerationRequest): Flow<GenerationEvent> = flow {
        emit(GenerationEvent.Progress(10, "Preparing your idea"))
        val result = generateText(request.idea, request.format, request.preferences, request.existingText)
        if (result == null) emit(GenerationEvent.Failed("AI request failed. Check your provider settings and connection."))
        else {
            emit(GenerationEvent.Progress(90, "Polishing writing"))
            emit(GenerationEvent.Chunk(result.first))
            emit(GenerationEvent.Completed(result.first, result.second))
        }
    }

    override fun continueWriting(request: ContinueRequest): Flow<GenerationEvent> = flow {
        emit(GenerationEvent.Progress(10, "Reading your draft"))
        val result = generateText(request.idea, request.format, request.preferences, request.currentText)
        if (result == null) emit(GenerationEvent.Failed("AI request failed. Check your provider settings and connection."))
        else {
            emit(GenerationEvent.Progress(90, "Continuing naturally"))
            emit(GenerationEvent.Chunk(result.first))
            emit(GenerationEvent.Completed(result.first, result.second))
        }
    }

    private suspend fun generateText(
        idea: String,
        format: OutputFormat,
        prefs: WritingPreferences,
        existing: String?
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || idea.isBlank()) return@withContext null
        val prompt = buildString {
            appendLine("You are StoryForge, a professional writing transformation assistant.")
            appendLine("Transform the user's raw idea into polished, original writing.")
            appendLine("Output format: ${format.displayName}")
            appendLine("Tone: ${prefs.tone}")
            appendLine("POV: ${prefs.pov}")
            appendLine("Length: ${prefs.length}")
            appendLine("Preserve the user's meaning; do not invent claims about real people.")
            if (!existing.isNullOrBlank()) {
                appendLine("Existing draft:")
                appendLine(existing)
                appendLine("Continue the existing draft naturally and preserve its established facts, characters and voice.")
            }
            appendLine("Raw idea:")
            appendLine(idea)
            appendLine("Return only the finished writing, with no explanation of the process.")
        }

        val body = JSONObject()
            .put("model", model)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", "You are an expert editor and writer."))
                .put(JSONObject().put("role", "user").put("content", prompt)))
            .put("temperature", 0.8)
            .put("max_tokens", 4000)
            .toString()

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) return@withContext null
            val text = JSONObject(response).getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content").trim()
            if (text.isBlank()) return@withContext null
            val title = text.lineSequence().firstOrNull()?.take(60)?.ifBlank { "Untitled" } ?: "Untitled"
            text to title
        } finally { connection.disconnect() }
    }
}
