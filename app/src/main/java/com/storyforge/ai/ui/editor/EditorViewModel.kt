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

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            val project = runCatching { projects.get(projectId) }.getOrNull()
            if (project == null) {
                _state.update { it.copy(loading = false, error = "Project could not be opened.") }
            } else {
                val displayText = if (projects.generatedBelongsToContext(project)) project.generatedText else project.rawIdea
                _state.update { it.copy(project = project, text = displayText, title = project.title, loading = false) }
            }
        }
    }

    fun onTextChange(value: String) { _state.update { it.copy(text = value, saved = false, copied = false) }; scheduleAutosave() }
    fun onTitleChange(value: String) { _state.update { it.copy(title = value, saved = false) }; scheduleAutosave() }

    fun save(onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            if (persist(markSaved = true)) onDone?.invoke()
        }
    }

    fun restore(versionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            val restored = runCatching { projects.restoreVersion(projectId, versionId) }.getOrNull()
            if (restored == null) _state.update { it.copy(saving = false, error = "That version is no longer available.") }
            else _state.update { it.copy(project = restored, text = restored.generatedText, title = restored.title, saving = false, saved = true) }
        }
    }

    fun markCopied() {
        _state.update { it.copy(copied = true) }
        viewModelScope.launch { delay(1600); _state.update { it.copy(copied = false) } }
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch { delay(600); persist(markSaved = false) }
    }

    private suspend fun persist(markSaved: Boolean): Boolean {
        val current = _state.value
        if (current.project == null) return false
        _state.update { it.copy(saving = true, error = null) }
        return runCatching {
            projects.updateManuscript(projectId, current.text, current.title.trim())
                ?: throw IllegalStateException("This project no longer exists.")
        }.fold(
            onSuccess = { savedProject ->
                _state.update { it.copy(project = savedProject, saving = false, saved = markSaved, error = null) }
                true
            },
            onFailure = { err ->
                _state.update { it.copy(saving = false, error = err.message ?: "Save failed.") }
                false
            }
        )
    }

    companion object {
        fun factory(projectId: String, projects: ProjectRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = EditorViewModel(projectId, projects) as T
        }
    }
}
