package com.storyforge.ai.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String,
    val title: String,
    val rawIdea: String = "",
    val generatedText: String = "",
    // Fingerprint of the idea + format + writing preferences used by the current manuscript.
    // Empty means legacy/untrusted output.
    val generatedForIdeaHash: String = "",
    val format: OutputFormat = OutputFormat.SHORT_STORY,
    val inputMode: InputMode = InputMode.TEXT,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: ProjectStatus = ProjectStatus.DRAFT,
    // Monotonic in-app revision used to prevent stale generation results from overwriting edits.
    val revision: Long = 0L,
    // Local snapshots. Kept with the project so they remain isolated from every other project.
    val versions: List<ProjectVersion> = emptyList()
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
data class ThemePreference(
    val mode: String = "system" // system | light | dark
)

@Serializable
data class AiProviderSettings(
    val provider: String = "openai-compatible",
    val model: String = "gpt-4o-mini",
    val apiKeyConfigured: Boolean = false,
    val endpoint: String = "https://api.openai.com/v1/chat/completions"
)
