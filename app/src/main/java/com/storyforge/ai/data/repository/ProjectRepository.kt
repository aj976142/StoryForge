package com.storyforge.ai.data.repository

import com.storyforge.ai.data.local.ProjectStore
import com.storyforge.ai.domain.model.InputMode
import com.storyforge.ai.domain.model.OutputFormat
import com.storyforge.ai.domain.model.Project
import com.storyforge.ai.domain.model.ProjectStatus
import com.storyforge.ai.util.GenerationProvenance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProjectRepository(private val store: ProjectStore) {

    fun observeAll(): Flow<List<Project>> = store.observeProjects()

    fun observeRecent(limit: Int = 8): Flow<List<Project>> =
        store.observeProjects().map { it.take(limit) }

    suspend fun get(id: String): Project? = store.getProject(id)

    suspend fun createDraft(mode: InputMode): Project {
        val now = System.currentTimeMillis()
        val project = Project(
            id = UUID.randomUUID().toString(),
            title = "Untitled draft",
            inputMode = mode,
            createdAt = now,
            updatedAt = now,
            status = ProjectStatus.DRAFT
        )
        return store.upsert(project)
    }

    suspend fun save(project: Project): Project = store.upsert(
        project.copy(
            status = if (project.generatedText.isNotBlank() && generatedBelongsToIdea(project)) {
                ProjectStatus.SAVED
            } else {
                ProjectStatus.DRAFT
            },
            title = project.title.ifBlank { "Untitled" }
        )
    )

    suspend fun autosave(project: Project): Project = store.upsert(project)

    suspend fun updateIdea(id: String, idea: String): Project? {
        val current = store.getProject(id) ?: return null
        val changed = GenerationProvenance.normalizeIdea(current.rawIdea) != GenerationProvenance.normalizeIdea(idea)
        return store.upsert(
            current.copy(
                rawIdea = idea,
                // Changing the idea invalidates any previous generated draft.
                generatedText = if (changed) "" else current.generatedText,
                generatedForIdeaHash = if (changed) "" else current.generatedForIdeaHash,
                status = if (changed) ProjectStatus.DRAFT else current.status
            )
        )
    }

    suspend fun updateFormat(id: String, format: OutputFormat): Project? {
        val current = store.getProject(id) ?: return null
        return store.upsert(current.copy(format = format))
    }

    suspend fun updateGenerated(id: String, text: String, title: String? = null): Project? {
        val current = store.getProject(id) ?: return null
        return store.upsert(
            current.copy(
                generatedText = text,
                generatedForIdeaHash = GenerationProvenance.hashIdea(current.rawIdea),
                title = title?.takeIf { it.isNotBlank() } ?: current.title,
                status = ProjectStatus.GENERATED
            )
        )
    }

    suspend fun rename(id: String, title: String): Project? = store.rename(id, title)

    suspend fun delete(id: String) = store.delete(id)

    fun generatedBelongsToIdea(project: Project): Boolean =
        project.generatedText.isNotBlank() &&
            project.generatedForIdeaHash.isNotBlank() &&
            project.generatedForIdeaHash == GenerationProvenance.hashIdea(project.rawIdea)
}
