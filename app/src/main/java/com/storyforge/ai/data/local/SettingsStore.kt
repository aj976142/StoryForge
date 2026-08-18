package com.storyforge.ai.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.storyforge.ai.domain.model.AiProviderSettings
import com.storyforge.ai.domain.model.ThemePreference
import com.storyforge.ai.domain.model.WritingPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "storyforge_settings")

class SettingsStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val theme: Flow<ThemePreference> = context.dataStore.data.map { prefs ->
        decode(prefs[Keys.THEME], ThemePreference())
    }

    val writing: Flow<WritingPreferences> = context.dataStore.data.map { prefs ->
        decode(prefs[Keys.WRITING], WritingPreferences())
    }

    val ai: Flow<AiProviderSettings> = context.dataStore.data.map { prefs ->
        decode(prefs[Keys.AI], AiProviderSettings())
    }

    suspend fun setTheme(value: ThemePreference) = save(Keys.THEME, value)
    suspend fun setWriting(value: WritingPreferences) = save(Keys.WRITING, value)
    suspend fun setAi(value: AiProviderSettings) = save(Keys.AI, value)

    private suspend inline fun <reified T> save(
        key: androidx.datastore.preferences.core.Preferences.Key<String>,
        value: T
    ) {
        context.dataStore.edit { it[key] = json.encodeToString(value) }
    }

    private inline fun <reified T> decode(raw: String?, fallback: T): T {
        if (raw.isNullOrBlank()) return fallback
        return runCatching { json.decodeFromString<T>(raw) }.getOrDefault(fallback)
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val WRITING = stringPreferencesKey("writing")
        val AI = stringPreferencesKey("ai")
    }
}
