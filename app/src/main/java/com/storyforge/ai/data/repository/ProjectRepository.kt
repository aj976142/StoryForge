package com.storyforge.ai.data.repository

import com.storyforge.ai.data.local.ProjectStore
import com.storyforge.ai.domain.model.InputMode
import com.storyforge.ai.domain.model.OutputFormat
import com.storyforge.ai.domain.model.Project
import com.storyforge.ai.domain.model.ProjectStatus
import com.storyforge.ai.domain.model.ProjectVersion
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
    fun observeRecent(limit: Int = 8): Flow<List<Project>> = store.observeProjects().map { it.take(limit) }
    suspend fun get(id: String): Project? = store.getProject(id)

    suspend fun createDraft(mode: InputMode): Project {
        val now = System.currentTimeMillis()
        return mutationMutex.withLock { store.upsert(Project(UUID.randomUUID().toString(), "Untitled draft", inputMode = mode, createdAt = now, updatedAt = now, status = ProjectStatus.DRAFT)) }
    }

    suspend fun save(project: Project): Project = mutationMutex.withLock {
        if (deletedIds.contains(project.id)) throw IllegalStateException("This project no longer exists.")
        val current = store.getProject(project.id) ?: throw IllegalStateException("This project no longer exists.")
        val manuscript = project.generatedText
        val trusted = manuscript.isNotBlank() && current.generatedForIdeaHash.isNotBlank() &&
            current.generatedForIdeaHash == GenerationProvenance.fingerprint(current.rawIdea, current.format)
        store.upsert(project.copy(
            revision = current.revision + 1,
            rawIdea = current.rawIdea,
            format = current.format,
            inputMode = current.inputMode,
            createdAt = current.createdAt,
            generatedForIdeaHash = if (trusted) current.generatedForIdeaHash else "",
            status = if (manuscript.isBlank()) ProjectStatus.DRAFT else ProjectStatus.SAVED,
            title = project.title.ifBlank { "Untitled" },
            versions = appendVersion(current, manuscript, project.title, "Saved")
        ))
    }

    suspend fun updateManuscript(id: String, text: String, title: String): Project? = mutationMutex.withLock {
        val current = store.getProject(id) ?: return@withLock null
        if (deletedIds.contains(id)) return@withLock null
        // Do not trim the manuscript: whitespace/newlines can be intentional in prose and scripts.
        val manuscript = text
        val fingerprint = if (manuscript.isBlank()) "" else GenerationProvenance.fingerprint(current.rawIdea, current.format)
        store.upsert(current.copy(
            generatedText = manuscript,
            generatedForIdeaHash = fingerprint,
            title = title.ifBlank { current.title },
            status = if (manuscript.isBlank()) ProjectStatus.DRAFT else ProjectStatus.SAVED,
            revision = current.revision + 1,
            versions = appendVersion(current, manuscript, title, "Autosave")
        ))
    }

    suspend fun autosave(project: Project): Project = save(project)

    suspend fun updateIdea(id: String, idea: String): Project? = mutationMutex.withLock {
        val current = store.getProject(id) ?: return@withLock null
        if (deletedIds.contains(id)) return@withLock null
        val changed = GenerationProvenance.normalizeIdea(current.rawIdea) != GenerationProvenance.normalizeIdea(idea)
        store.upsert(current.copy(
            rawIdea = idea,
            generatedText = if (changed) "" else current.generatedText,
            generatedForIdeaHash = if (changed) "" else current.generatedForIdeaHash,
            status = if (changed) ProjectStatus.DRAFT else current.status,
            revision = current.revision + 1,
            versions = if (changed) (current.versions + ProjectVersion(UUID.randomUUID().toString(), current.title, current.generatedText, label = "Before idea change")).takeLast(MAX_VERSIONS) else current.versions
        ))
    }

    suspend fun updateFormat(id: String, format: OutputFormat): Project? = mutationMutex.withLock {
        val current = store.getProject(id) ?: return@withLock null
        if (deletedIds.contains(id)) return@withLock null
        if (current.format == format) return@withLock current
        store.upsert(current.copy(
            format = format,
            generatedText = "",
            generatedForIdeaHash = "",
            status = ProjectStatus.DRAFT,
            revision = current.revision + 1,
            versions = if (current.generatedText.isNotBlank()) (current.versions + ProjectVersion(UUID.randomUUID().toString(), current.title, current.generatedText, label = "Before format change")).takeLast(MAX_VERSIONS) else current.versions
        ))
    }

    suspend fun updateGenerated(id: String, expectedRevision: Long, text: String, title: String?): Project? = mutationMutex.withLock {
        val current = store.getProject(id) ?: return@withLock null
        if (deletedIds.contains(id) || current.revision != expectedRevision) return@withLock null
        val manuscript = text.trim()
        store.upsert(current.copy(
            generatedText = manuscript,
            generatedForIdeaHash = if (manuscript.isBlank()) "" else GenerationProvenance.fingerprint(current.rawIdea, current.format),
            title = title?.takeIf { it.isNotBlank() } ?: current.title,
            status = if (manuscript.isBlank()) ProjectStatus.DRAFT else ProjectStatus.GENERATED,
            revision = current.revision + 1,
            versions = appendVersion(current, manuscript, title ?: current.title, "AI generation")
        ))
    }

    suspend fun restoreVersion(id: String, versionId: String): Project? = mutationMutex.withLock {
        val current = store.getProject(id) ?: return@withLock null
        if (deletedIds.contains(id)) return@withLock null
        val version = current.versions.firstOrNull { it.id == versionId } ?: return@withLock null
        val manuscript = version.text
        store.upsert(current.copy(
            generatedText = manuscript,
            title = version.title.ifBlank { current.title },
            generatedForIdeaHash = if (manuscript.isBlank()) "" else GenerationProvenance.fingerprint(current.rawIdea, current.format),
            status = if (manuscript.isBlank()) ProjectStatus.DRAFT else ProjectStatus.SAVED,
            revision = current.revision + 1,
            versions = appendVersion(current, current.generatedText, current.title, "Before restore")
        ))
    }

    suspend fun toggleFavorite(id: String): Project? = mutationMutex.withLock {
        val current = store.getProject(id) ?: return@withLock null
        if (deletedIds.contains(id)) return@withLock null
        store.upsert(current.copy(favorite = !current.favorite, revision = current.revision + 1))
    }

    suspend fun duplicate(id: String): Project? = mutationMutex.withLock {
        val source = store.getProject(id) ?: return@withLock null
        if (deletedIds.contains(id)) return@withLock null
        val now = System.currentTimeMillis()
        store.upsert(source.copy(id = UUID.randomUUID().toString(), title = "${source.title} copy", createdAt = now, updatedAt = now, revision = 0L, favorite = false, versions = source.versions.takeLast(10)))
    }

    suspend fun rename(id: String, title: String): Project? = mutationMutex.withLock {
        val current = store.getProject(id) ?: return@withLock null
        if (deletedIds.contains(id)) return@withLock null
        store.upsert(current.copy(title = title.trim().ifBlank { current.title }, revision = current.revision + 1))
    }

    suspend fun delete(id: String) = mutationMutex.withLock { deletedIds += id; store.delete(id) }

    fun generatedBelongsToContext(project: Project): Boolean =
        project.generatedText.isNotBlank() && project.generatedForIdeaHash.isNotBlank() &&
            project.generatedForIdeaHash == GenerationProvenance.fingerprint(project.rawIdea, project.format)

    private fun appendVersion(project: Project, text: String, title: String, label: String): List<ProjectVersion> {
        if (text.isBlank()) return project.versions
        val last = project.versions.lastOrNull()
        if (last?.text == text && last.title == title) return project.versions
        return (project.versions + ProjectVersion(UUID.randomUUID().toString(), title.ifBlank { project.title }, text, label = label)).takeLast(MAX_VERSIONS)
    }

    companion object { private const val MAX_VERSIONS = 20 }
}
