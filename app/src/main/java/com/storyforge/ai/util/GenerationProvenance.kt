package com.storyforge.ai.util

import java.security.MessageDigest

/**
 * Identifies the exact idea a generated draft belongs to.
 * This prevents stale output from another idea/project being reused.
 */
object GenerationProvenance {
    fun normalizeIdea(idea: String): String =
        idea.trim().replace(Regex("\\s+"), " ")

    fun hashIdea(idea: String): String {
        val normalized = normalizeIdea(idea)
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
