package com.storyforge.ai.util

/** Lightweight offline continuity helper. It never sends text anywhere. */
data class StoryBrain(
    val characters: List<String>,
    val places: List<String>,
    val themes: List<String>,
    val wordCount: Int,
    val readingMinutes: Int
) {
    companion object {
        fun analyze(text: String): StoryBrain {
            val clean = text.trim()
            val words = TextMetrics.words(clean)
            val reading = ((words + 179) / 180).coerceAtLeast(1)
            val characters = Regex("(?m)^(?:CHARACTER:|CHARACTER\\s+)?([A-Z][A-Za-z'-]{2,24}(?:\\s+[A-Z][A-Za-z'-]{2,24})?)\\s*$")
                .findAll(clean)
                .map { it.groupValues[1].trim() }
                .filterNot { it.equals("FADE IN") || it.equals("FADE OUT") }
                .distinct()
                .take(8)
                .toList()
            val places = Regex("(?i)\\b(?:in|at|near|inside|outside|from)\\s+([A-Z][A-Za-z0-9' -]{2,35})")
                .findAll(clean)
                .map { it.groupValues[1].trim().trim('.', ',', ';', ':') }
                .filter { it.length in 3..35 }
                .distinct()
                .take(6)
                .toList()
            val stop = setOf("the", "and", "that", "this", "with", "from", "have", "they", "there", "into", "your", "their", "about", "what", "when", "where", "which", "would", "could", "should", "story", "like")
            val themes = Regex("[A-Za-z]{5,}")
                .findAll(clean.lowercase())
                .map { it.value }
                .filterNot { it in stop }
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }
                .take(5)
                .map { it.key.replaceFirstChar(Char::uppercase) }
            return StoryBrain(characters, places, themes, words, reading)
        }
    }
}
