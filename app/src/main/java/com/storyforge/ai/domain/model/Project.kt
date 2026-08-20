package com.storyforge.ai.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String,
    val title: String,
    val rawIdea: String = "",
    val generatedText: String = "",
    val generatedForIdeaHash: String = "",
    val format: OutputFormat = OutputFormat.SHORT_STORY,
    val inputMode: InputMode = InputMode.TEXT,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: ProjectStatus = ProjectStatus.DRAFT,
    val revision: Long = 0L,
    val versions: List<ProjectVersion> = emptyList(),
    val favorite: Boolean = false,
    val tags: List<String> = emptyList(),
    // Project-local memory. It is never shared with another project.
    val storyBible: StoryBible = StoryBible()
)

@Serializable
data class ProjectVersion(
    val id: String,
    val title: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val label: String = "Autosave"
)

@Serializable
data class StoryBible(
    val characters: List<StoryBibleEntry> = emptyList(),
    val locations: List<StoryBibleEntry> = emptyList(),
    val plotNotes: List<String> = emptyList(),
    val themes: List<String> = emptyList(),
    val customNotes: String = ""
)

@Serializable
data class StoryBibleEntry(
    val id: String,
    val name: String,
    val details: String = ""
)

@Serializable
enum class InputMode { VOICE, TEXT }

@Serializable
enum class ProjectStatus { DRAFT, GENERATED, SAVED }

@Serializable
data class WritingPreferences(
    val tone: String = "Cinematic",
    val length: String = "Medium",
    val language: String = "English",
    val pov: String = "Third person"
)

@Serializable
data class ThemePreference(val mode: String = "system")

@Serializable
data class AiProviderSettings(
    val provider: String = "openai-compatible",
    val model: String = "gpt-4o-mini",
    val apiKeyConfigured: Boolean = false,
    val endpoint: String = "https://api.openai.com/v1/chat/completions"
)
