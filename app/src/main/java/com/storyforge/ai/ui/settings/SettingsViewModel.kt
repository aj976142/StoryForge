package com.storyforge.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storyforge.ai.data.repository.SettingsRepository
import com.storyforge.ai.domain.model.AiProviderSettings
import com.storyforge.ai.domain.model.WritingPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: String = "system",
    val writing: WritingPreferences = WritingPreferences(),
    val ai: AiProviderSettings = AiProviderSettings()
)

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(
        settings.theme,
        settings.writing,
        settings.ai
    ) { theme, writing, ai ->
        SettingsUiState(themeMode = theme.mode, writing = writing, ai = ai)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: String) = viewModelScope.launch { settings.setThemeMode(mode) }

    fun setWriting(writing: WritingPreferences) = viewModelScope.launch { settings.setWriting(writing) }

    fun setProvider(provider: String) = viewModelScope.launch {
        settings.setAi(state.value.ai.copy(provider = provider))
    }

    companion object {
        fun factory(settings: SettingsRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(settings) as T
        }
    }
}
