package com.storyforge.ai.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storyforge.ai.data.repository.ProjectRepository
import com.storyforge.ai.domain.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProjectSort { UPDATED, CREATED, TITLE }

data class ProjectsUiState(
    val items: List<Project> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val renamingId: String? = null,
    val query: String = "",
    val sort: ProjectSort = ProjectSort.UPDATED
) {
    val visibleItems: List<Project>
        get() = items.filter { query.isBlank() || it.title.contains(query, true) || it.rawIdea.contains(query, true) }
            .let { list -> when (sort) {
                ProjectSort.UPDATED -> list.sortedByDescending { it.updatedAt }
                ProjectSort.CREATED -> list.sortedByDescending { it.createdAt }
                ProjectSort.TITLE -> list.sortedBy { it.title.lowercase() }
            } }
}

class ProjectsViewModel(private val projects: ProjectRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProjectsUiState())
    val state: StateFlow<ProjectsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            projects.observeAll().collect { list -> _state.update { it.copy(items = list, loading = false) } }
        }
    }

    fun setQuery(value: String) = _state.update { it.copy(query = value) }
    fun setSort(value: ProjectSort) = _state.update { it.copy(sort = value) }
    fun beginRename(id: String) = _state.update { it.copy(renamingId = id) }
    fun cancelRename() = _state.update { it.copy(renamingId = null) }

    fun rename(id: String, title: String) {
        viewModelScope.launch {
            runCatching { projects.rename(id, title.trim()) }
                .onFailure { err -> _state.update { it.copy(error = err.message ?: "Rename failed.") } }
            _state.update { it.copy(renamingId = null) }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { projects.delete(id) }
                .onFailure { err -> _state.update { it.copy(error = err.message ?: "Delete failed.") } }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    companion object {
        fun factory(projects: ProjectRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ProjectsViewModel(projects) as T
        }
    }
}
