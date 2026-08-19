package com.storyforge.ai.domain.model

import kotlinx.serialization.Serializable

/** Structured memory owned by one StoryForge project. Never shared across projects. */
@Serializable
data class StoryBible(
    val characters: List<BibleCharacter> = emptyList(),
    val locations: List<BibleLocation> = emptyList(),
    val events: List<BibleEvent> = emptyList(),
    val themes: List<String> = emptyList(),
    val notes: String = ""
) {
    fun asPromptContext(): String = buildString {
        if (characters.isNotEmpty()) {
            appendLine("CHARACTERS")
            characters.forEach { appendLine("- ${it.name}: ${it.description}") }
        }
        if (locations.isNotEmpty()) {
            appendLine("LOCATIONS")
            locations.forEach { appendLine("- ${it.name}: ${it.description}") }
        }
        if (events.isNotEmpty()) {
            appendLine("TIMELINE")
            events.forEach { appendLine("- ${it.order}. ${it.title}: ${it.description}") }
        }
        if (themes.isNotEmpty()) appendLine("THEMES: ${themes.joinToString(", ")}")
        if (notes.isNotBlank()) appendLine("PROJECT NOTES\n$notes")
    }.trim()
}

@Serializable
data class BibleCharacter(
    val id: String,
    val name: String,
    val description: String = "",
    val relationships: String = "",
    val goals: String = ""
)

@Serializable
data class BibleLocation(
    val id: String,
    val name: String,
    val description: String = ""
)

@Serializable
data class BibleEvent(
    val id: String,
    val order: Int,
    val title: String,
    val description: String = ""
)
