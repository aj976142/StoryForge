package com.storyforge.ai.util

import com.storyforge.ai.domain.model.OutputFormat
import com.storyforge.ai.domain.model.WritingPreferences
import java.security.MessageDigest

/**
 * Identifies the exact project context a generated manuscript belongs to.
 * The fingerprint includes the raw idea, selected form, and writing preferences
 * so stale output cannot be reused after a context change.
 */
object GenerationProvenance {
    fun normalizeIdea(idea: String): String =
        idea.trim().replace(Regex("\\s+"), " ")

    fun fingerprint(
        idea: String,
        format: OutputFormat,
        preferences: WritingPreferences
    ): String {
        val normalized = buildString {
            append(normalizeIdea(idea)).append('\n')
            append(format.name).append('\n')
            append(preferences.tone.trim()).append('\n')
            append(preferences.length.trim()).append('\n')
            append(preferences.pov.trim()).append('\n')
            append(preferences.language.trim())
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    // Backward-compatible helper for legacy callers/tests.
    fun hashIdea(idea: String): String =
        fingerprint(idea, OutputFormat.SHORT_STORY, WritingPreferences())
}
