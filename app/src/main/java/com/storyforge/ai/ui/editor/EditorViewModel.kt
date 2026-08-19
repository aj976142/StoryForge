package com.storyforge.ai.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storyforge.ai.data.repository.ProjectRepository
import com.storyforge.ai.domain.model.Project
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val project: Project? = null,
    val text: String = "",
    val title: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val copied: Boolean = false,
    val error: String? = null
)

class EditorViewModel(private val projectId: String, private val projects: ProjectRepository) : ViewModel() {
    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()
    private var autosaveJob: Job? = null

    init {
        viewModelScope.launch {
            val project = runCatching { projects.get(projectId) }.getOrNull()
            if (project == null) _state.update { it.copy(loading = false, error = "Project could not be opened.") }
            else _state.update { it.copy(project = project, text = project.generatedText.ifBlank { project.rawIdea }, title = project.title, loading = false) }
        }
    }

    fun onTextChange(value: String) {
        _state.update { it.copy(text = value, saved = false, copied = false) }
        scheduleAutosave()
    }

    fun onTitleChange(value: String) {
        _state.update { it.copy(title = value, saved = false) }
        scheduleAutosave()
    }

    fun save(onDone: (() -> Unit)? = null) {
        viewModelScope.launch { persist(markSaved = true); onDone?.invoke() }
    }

    fun markCopied() {
        _state.update { it.copy(copied = true) }
        viewModelScope.launch { delay(1600); _state.update { it.copy(copied = false) } }
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch { delay(600); persist(markSaved = false) }
    }

    private suspend fun persist(markSaved: Boolean) {
        val current = _state.value
        val project = current.project ?: return
        _state.update { it.copy(saving = true, error = null) }
        val updated = project.copy(generatedText = current.text, title = current.title.ifBlank { project.title })
        runCatching { projects.save(updated) }
            .onSuccess { savedProject -> _state.update { it.copy(project = savedProject, saving = false, saved = markSaved) } }
            .onFailure { err -> _state.update { it.copy(saving = false, error = err.message ?: "Save failed.") } }
    }

    companion object {
        fun factory(projectId: String, projects: ProjectRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = EditorViewModel(projectId, projects) as T
        }
    }
}
