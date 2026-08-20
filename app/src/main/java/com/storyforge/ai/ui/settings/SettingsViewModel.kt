package com.storyforge.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storyforge.ai.data.ai.AiProviderCatalog
import com.storyforge.ai.data.ai.HttpAiService
import com.storyforge.ai.data.local.SecretStore
import com.storyforge.ai.data.repository.SettingsRepository
import com.storyforge.ai.domain.model.AiProviderSettings
import com.storyforge.ai.domain.model.WritingPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: String = "system",
    val writing: WritingPreferences = WritingPreferences(),
    val ai: AiProviderSettings = AiProviderSettings(),
    val testingConnection: Boolean = false,
    val connectionMessage: String? = null
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val secretStore: SecretStore,
    private val testAi: HttpAiService
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val liveModels = MutableStateFlow<List<String>>(emptyList())
    private val loadingModels = MutableStateFlow(false)

    val state: StateFlow<SettingsUiState> = combine(settings.theme, settings.writing, settings.ai, busy, message) { theme, writing, ai, testing, msg ->
        SettingsUiState(theme.mode, writing, ai, testing, msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    val models: StateFlow<List<String>> = liveModels
    val isLoadingModels: StateFlow<Boolean> = loadingModels

    init {
        viewModelScope.launch {
            val current = settings.ai.first()
            if (current.provider == "openai-compatible") {
                settings.setAi(current.copy(provider = "openai"))
            }
        }
    }

    fun setTheme(mode: String) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setWriting(writing: WritingPreferences) = viewModelScope.launch { settings.setWriting(writing) }

    fun setProvider(provider: String) = viewModelScope.launch {
        val current = state.value.ai
        val preset = AiProviderCatalog.preset(provider)
        val next = if (preset == null) current.copy(provider = provider) else current.copy(
            provider = provider,
            endpoint = if (preset.endpoint.isNotBlank()) preset.endpoint else current.endpoint,
            model = preset.models.firstOrNull() ?: current.model
        )
        settings.setAi(next.copy(apiKeyConfigured = secretStore.getApiKey()?.isNotBlank() == true))
        liveModels.value = emptyList()
        message.value = null
    }

    fun setModel(model: String) = viewModelScope.launch { settings.setAi(state.value.ai.copy(model = model.trim())) }
    fun setEndpoint(endpoint: String) = viewModelScope.launch { settings.setAi(state.value.ai.copy(endpoint = endpoint.trim())) }

    fun refreshModels() {
        viewModelScope.launch {
            loadingModels.value = true
            val result = testAi.listModels()
            liveModels.value = result.getOrDefault(emptyList())
            message.value = result.exceptionOrNull()?.message ?: if (liveModels.value.isNotEmpty()) "Loaded ${liveModels.value.size} models from the provider" else null
            loadingModels.value = false
        }
    }

    fun saveApiKey(key: String) = viewModelScope.launch {
        secretStore.saveApiKey(key.trim())
        settings.setAi(state.value.ai.copy(apiKeyConfigured = key.isNotBlank()))
        message.value = if (key.isBlank()) "API key removed" else "API key saved securely on this device"
    }
    fun clearApiKey() = saveApiKey("")

    fun testConnection() {
        viewModelScope.launch {
            busy.value = true
            message.value = null
            val result = testAi.testConnection()
            message.value = result.fold({ it }, { it.message ?: "Connection failed" })
            busy.value = false
        }
    }

    companion object {
        fun factory(settings: SettingsRepository, secrets: SecretStore, testAi: HttpAiService) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(settings, secrets, testAi) as T
        }
    }
}
