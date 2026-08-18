package com.storyforge.ai.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String,
    val title: String,
    val rawIdea: String = "",
    val generatedText: String = "",
    val format: OutputFormat = OutputFormat.SHORT_STORY,
    val inputMode: InputMode = InputMode.TEXT,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: ProjectStatus = ProjectStatus.DRAFT
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
    val provider: String = "mock",
    val model: String = "storyforge-mock-v1",
    val apiKeyConfigured: Boolean = false
)
