package com.storyforge.ai.data.repository

import com.storyforge.ai.data.local.ProjectStore
import com.storyforge.ai.domain.model.InputMode
import com.storyforge.ai.domain.model.OutputFormat
import com.storyforge.ai.domain.model.Project
import com.storyforge.ai.domain.model.ProjectStatus
import com.storyforge.ai.domain.model.WritingPreferences
import com.storyforge.ai.util.GenerationProvenance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class ProjectRepository(private val store: ProjectStore) {
    private val mutationMutex = Mutex()
    private val deletedIds = mutableSetOf<String>()

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
        return mutationMutex.withLock { store.upsert(project) }
    }

    suspend fun save(project: Project): Project = mutationMutex.withLock {
        if (deletedIds.contains(project.id) || store.getProject(project.id) == null) {
            throw IllegalStateException("This project no longer exists.")
        }
        val current = store.getProject(project.id) ?: throw IllegalStateException("This project no longer exists.")
        val safe = project.copy(
            revision = current.revision + 1,
            generatedForIdeaHash = current.generatedForIdeaHash,
            status = if (project.generatedText.isNotBlank() && generatedBelongsToContext(project)) {
                ProjectStatus.SAVED
            } else {
                ProjectStatus.DRAFT
            },
            title = project.title.ifBlank { "Untitled" }
        )
        store.upsert(safe)
    }

    suspend fun autosave(project: Project): Project = save(project)

    suspend fun updateIdea(id: String, idea: String): Project? = mutationMutex.withLock {
        val current = store.getProject(id) ?: return@withLock null
        if (deletedIds.contains(id)) return@withLock null
        val changed = GenerationProvenance.normalizeIdea(current.rawIdea) != GenerationProvenance.normalizeIdea(idea)
        val updated = current.copy(
            rawIdea = idea,
            generatedText = if (changed) "" else current.generatedText,
            generatedForIdeaHash = if (changed) "" else current.generatedForIdeaHash,
            status = if (changed) ProjectStatus.DRAFT else current.status,
            revision = current.revision + 1
        )
        store.upsert(updated)
    }

    suspend fun updateFormat(id: String, format: OutputFormat): Project? = mutationMutex.withLock {
        val current = store.getProject(id) ?: return@withLock null
        if (deletedIds.contains(id)) return@withLock null
        if (current.format == format) return@withLock current
        // A different output form invalidates the old manuscript context.
        store.upsert(
            current.copy(
                format = format,
                generatedText = "",
                generatedForIdeaHash = "",
                status = ProjectStatus.DRAFT,
                revision = current.revision + 1
            )
        )
    }

    suspend fun updateGenerated(
        id: String,
        expectedRevision: Long,
        text: String,
        title: String?,
        preferences: WritingPreferences
    ): Project? = mutationMutex.withLock {
        val current = store.getProject(id) ?: return@withLock null
        if (deletedIds.contains(id) || current.revision != expectedRevision) return@withLock null
        val fingerprint = GenerationProvenance.fingerprint(current.rawIdea, current.format, preferences)
        store.upsert(
            current.copy(
                generatedText = text,
                generatedForIdeaHash = fingerprint,
                title = title?.takeIf { it.isNotBlank() } ?: current.title,
                status = ProjectStatus.GENERATED,
                revision = current.revision + 1
            )
        )
    }

    suspend fun rename(id: String, title: String): Project? = mutationMutex.withLock {
        val current = store.getProject(id) ?: return@withLock null
        if (deletedIds.contains(id)) return@withLock null
        store.upsert(
            current.copy(
                title = title.trim().ifBlank { current.title },
                revision = current.revision + 1
            )
        )
    }

    suspend fun delete(id: String) = mutationMutex.withLock {
        deletedIds += id
        store.delete(id)
    }

    fun generatedBelongsToContext(project: Project, preferences: WritingPreferences = WritingPreferences()): Boolean =
        project.generatedText.isNotBlank() &&
            project.generatedForIdeaHash.isNotBlank() &&
            project.generatedForIdeaHash == GenerationProvenance.fingerprint(project.rawIdea, project.format, preferences)

    fun isGeneratedForCurrentContext(project: Project, preferences: WritingPreferences): Boolean =
        generatedBelongsToContext(project, preferences)
}
