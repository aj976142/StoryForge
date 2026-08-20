package com.storyforge.ai.ui.input

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storyforge.ai.data.repository.ProjectRepository
import com.storyforge.ai.data.repository.SettingsRepository
import com.storyforge.ai.domain.model.InputMode
import com.storyforge.ai.domain.model.OutputFormat
import com.storyforge.ai.util.TextMetrics
import com.storyforge.ai.util.VoiceInputController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InputUiState(
    val projectId: String = "",
    val text: String = "",
    val mode: InputMode = InputMode.TEXT,
    val format: OutputFormat = OutputFormat.SHORT_STORY,
    val listening: Boolean = false,
    val voiceAvailable: Boolean = true,
    val saving: Boolean = false,
    val savedHint: String? = null,
    val error: String? = null,
    val loaded: Boolean = false
) {
    val characters: Int get() = TextMetrics.characters(text)
    val words: Int get() = TextMetrics.words(text)
    val canContinue: Boolean get() = text.trim().isNotEmpty()
}

class InputViewModel(
    application: Application,
    private val projects: ProjectRepository,
    private val settings: SettingsRepository,
    private val projectId: String,
    initialMode: InputMode
) : AndroidViewModel(application) {
    private val voice = VoiceInputController(application)
    private val _state = MutableStateFlow(InputUiState(projectId = projectId, mode = initialMode))
    val state: StateFlow<InputUiState> = _state.asStateFlow()
    private var autosaveJob: Job? = null

    init {
        viewModelScope.launch {
            val project = projects.get(projectId)
            if (project == null) _state.update { it.copy(loaded = true, error = "This draft could not be opened.") }
            else _state.update {
                it.copy(text = project.rawIdea, format = project.format, mode = if (initialMode == InputMode.VOICE) InputMode.VOICE else project.inputMode, loaded = true)
            }
        }
        viewModelScope.launch {
            voice.state.collect { vs ->
                _state.update { it.copy(listening = vs.listening, voiceAvailable = vs.available, error = vs.error ?: it.error) }
            }
        }
    }

    fun onTextChange(value: String) { _state.update { it.copy(text = value, savedHint = null) }; scheduleAutosave() }
    fun setFormat(format: OutputFormat) {
        _state.update { it.copy(format = format, savedHint = null) }
        viewModelScope.launch { runCatching { projects.updateFormat(projectId, format) } }
    }
    fun clear() { _state.update { it.copy(text = "", savedHint = null) }; scheduleAutosave() }

    fun toggleVoice() {
        val current = _state.value
        if (current.listening) {
            voice.stop()
        } else {
            viewModelScope.launch {
                voice.clearError()
                val language = settings.writing.first().language
                voice.start(language) { spoken ->
                    val merged = listOf(_state.value.text.trim(), spoken.trim()).filter { it.isNotEmpty() }.joinToString(" ")
                    onTextChange(merged)
                }
            }
        }
    }

    fun continueNext(onReady: (String) -> Unit) {
        viewModelScope.launch {
            persist()
            if (_state.value.text.trim().isEmpty()) {
                _state.update { it.copy(error = "Write or speak an idea first.") }
                return@launch
            }
            onReady(projectId)
        }
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch { delay(450); persist() }
    }

    private suspend fun persist() {
        _state.update { it.copy(saving = true) }
        runCatching {
            projects.updateIdea(projectId, _state.value.text)
            projects.updateFormat(projectId, _state.value.format)
        }.onSuccess { _state.update { it.copy(saving = false, savedHint = "Saved") } }
            .onFailure { err -> _state.update { it.copy(saving = false, error = err.message ?: "Autosave failed.") } }
    }

    override fun onCleared() { voice.destroy(); super.onCleared() }

    companion object {
        fun factory(application: Application, projects: ProjectRepository, settings: SettingsRepository, projectId: String, mode: InputMode) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = InputViewModel(application, projects, settings, projectId, mode) as T
        }
    }
}
