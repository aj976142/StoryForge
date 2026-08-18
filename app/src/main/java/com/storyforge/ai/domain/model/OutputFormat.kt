package com.storyforge.ai.domain.model

enum class OutputFormat(
    val displayName: String,
    val tagline: String,
    val description: String
) {
    NOVEL(
        displayName = "Novel",
        tagline = "Chaptered long-form fiction",
        description = "Turn a spark into chapters, characters, and a narrative arc ready to grow."
    ),
    MOVIE_SCREENPLAY(
        displayName = "Movie Screenplay",
        tagline = "Industry-style script pages",
        description = "Scene headings, action lines, and dialogue shaped for the screen."
    ),
    SHORT_STORY(
        displayName = "Short Story",
        tagline = "A complete tale in one sitting",
        description = "A focused beginning, turn, and ending with a clear emotional beat."
    ),
    YOUTUBE_SCRIPT(
        displayName = "YouTube Script",
        tagline = "Hook, story, and CTA",
        description = "Spoken-word pacing with a hook, beats, b-roll notes, and a closer."
    ),
    POLISHED_WRITING(
        displayName = "Polished Writing",
        tagline = "Cleaner, stronger prose",
        description = "Keep your voice. Tighten rhythm, clarity, and word choice."
    );

    companion object {
        fun fromName(raw: String?): OutputFormat {
            if (raw.isNullOrBlank()) return SHORT_STORY
            return entries.firstOrNull { it.name == raw } ?: SHORT_STORY
        }
    }
}
