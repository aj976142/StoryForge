package com.storyforge.ai.util

object TextMetrics {
    fun characters(text: String): Int = text.length

    fun words(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split(Regex("\\s+")).count { it.isNotBlank() }
    }
}
