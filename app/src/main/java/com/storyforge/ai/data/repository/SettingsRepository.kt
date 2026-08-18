package com.storyforge.ai.data.repository

import com.storyforge.ai.data.local.SettingsStore
import com.storyforge.ai.domain.model.AiProviderSettings
import com.storyforge.ai.domain.model.ThemePreference
import com.storyforge.ai.domain.model.WritingPreferences
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val store: SettingsStore) {
    val theme: Flow<ThemePreference> = store.theme
    val writing: Flow<WritingPreferences> = store.writing
    val ai: Flow<AiProviderSettings> = store.ai

    suspend fun setThemeMode(mode: String) = store.setTheme(ThemePreference(mode))
    suspend fun setWriting(value: WritingPreferences) = store.setWriting(value)
    suspend fun setAi(value: AiProviderSettings) = store.setAi(value)
}
