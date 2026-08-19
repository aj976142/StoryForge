package com.storyforge.ai.util

import com.storyforge.ai.domain.model.OutputFormat
import java.security.MessageDigest

/** Identifies the exact idea + output form a manuscript belongs to. */
object GenerationProvenance {
    fun normalizeIdea(idea: String): String =
        idea.trim().replace(Regex("\\s+"), " ")

    fun fingerprint(idea: String, format: OutputFormat): String {
        val normalized = normalizeIdea(idea) + "\n" + format.name
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    // Kept for compatibility with older stored/test code.
    fun hashIdea(idea: String): String = fingerprint(idea, OutputFormat.SHORT_STORY)
}
