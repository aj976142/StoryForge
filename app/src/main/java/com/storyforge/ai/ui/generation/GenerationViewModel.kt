package com.storyforge.ai.ui.generation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storyforge.ai.data.ai.AiService
import com.storyforge.ai.data.ai.ContinueRequest
import com.storyforge.ai.data.ai.GenerationEvent
import com.storyforge.ai.data.ai.GenerationRequest
import com.storyforge.ai.data.repository.ProjectRepository
import com.storyforge.ai.data.repository.SettingsRepository
import com.storyforge.ai.domain.model.WritingPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GenerationUiState(
    val percent: Int = 0,
    val stage: String = "Preparing",
    val running: Boolean = true,
    val error: String? = null,
    val retryable: Boolean = true,
    val doneId: String? = null
)

class GenerationViewModel(
    private val projectId: String,
    private val continueWrite: Boolean,
    private val projects: ProjectRepository,
    private val settings: SettingsRepository,
    private val ai: AiService
) : ViewModel() {

    private val _state = MutableStateFlow(GenerationUiState())
    val state: StateFlow<GenerationUiState> = _state.asStateFlow()
    private var job: Job? = null

    init { start() }

    fun start() {
        job?.cancel()
        _state.value = GenerationUiState(running = true)
        job = viewModelScope.launch {
            val project = projects.get(projectId)
            if (project == null) {
                _state.update { it.copy(running = false, error = "Project missing.", retryable = false) }
                return@launch
            }
            if (project.rawIdea.isBlank()) {
                _state.update { it.copy(running = false, error = "This draft has no idea yet.", retryable = false) }
                return@launch
            }

            // Capture the exact project revision used to start this generation.
            // If the user edits, changes format, deletes, or otherwise saves the
            // project while generation is running, the result is rejected instead
            // of being attached to the newer state.
            val expectedRevision = project.revision
            val canContinue = continueWrite &&
                projects.generatedBelongsToContext(project)
            val prefs = runCatching { settings.writing.first() }.getOrDefault(WritingPreferences())
            val flow = if (canContinue) {
                ai.continueWriting(ContinueRequest(project.rawIdea, project.format, project.generatedText, prefs))
            } else {
                ai.generate(GenerationRequest(project.rawIdea, project.format, prefs))
            }

            try {
                flow.collect { event ->
                    when (event) {
                        is GenerationEvent.Progress -> _state.update {
                            it.copy(percent = event.percent, stage = event.stage, running = true)
                        }
                        is GenerationEvent.Chunk -> Unit
                        is GenerationEvent.Completed -> {
                            val saved = projects.updateGenerated(
                                id = projectId,
                                expectedRevision = expectedRevision,
                                text = event.fullText,
                                title = event.suggestedTitle
                            )
                            if (saved == null) {
                                _state.update {
                                    it.copy(
                                        running = false,
                                        error = "This project changed while StoryForge was writing. Nothing was overwritten; generate again from the latest draft.",
                                        retryable = true
                                    )
                                }
                            } else {
                                _state.update {
                                    it.copy(percent = 100, stage = "Ready", running = false, doneId = projectId)
                                }
                            }
                        }
                        is GenerationEvent.Failed -> _state.update {
                            it.copy(running = false, error = event.message, retryable = event.retryable)
                        }
                    }
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                _state.update { it.copy(running = false, stage = "Stopped", error = "Generation stopped.") }
            } catch (error: Exception) {
                _state.update { it.copy(running = false, error = error.message ?: "Something went wrong.", retryable = true) }
            }
        }
    }

    fun stop() {
        job?.cancel()
        _state.update { it.copy(running = false, stage = "Stopped", error = "Generation stopped.") }
    }

    companion object {
        fun factory(
            projectId: String,
            continueWrite: Boolean,
            projects: ProjectRepository,
            settings: SettingsRepository,
            ai: AiService
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GenerationViewModel(projectId, continueWrite, projects, settings, ai) as T
        }
    }
}
