package com.storyforge.ai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storyforge.ai.data.repository.ProjectRepository
import com.storyforge.ai.domain.model.InputMode
import com.storyforge.ai.domain.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val recent: List<Project> = emptyList(),
    val loading: Boolean = true,
    val creating: Boolean = false,
    val error: String? = null
)

class HomeViewModel(private val projects: ProjectRepository) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            projects.observeRecent().collect { list ->
                _state.update { it.copy(recent = list, loading = false, error = null) }
            }
        }
    }

    fun startProject(mode: InputMode, onReady: (String) -> Unit) {
        if (_state.value.creating) return
        viewModelScope.launch {
            _state.update { it.copy(creating = true, error = null) }
            runCatching { projects.createDraft(mode) }
                .onSuccess { onReady(it.id) }
                .onFailure { err ->
                    _state.update { it.copy(error = err.message ?: "Could not create a project.") }
                }
            _state.update { it.copy(creating = false) }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    companion object {
        fun factory(projects: ProjectRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(projects) as T
        }
    }
}
