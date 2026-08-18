package com.storyforge.ai.data.local

import com.storyforge.ai.domain.model.Project
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class JsonProjectStore(
    private val directory: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }
) : ProjectStore {

    private val mutex = Mutex()
    private val cache = MutableStateFlow<List<Project>>(emptyList())
    private val ready = MutableStateFlow(false)

    init {
        directory.mkdirs()
    }

    suspend fun hydrate() {
        withContext(io) {
            mutex.withLock {
                val loaded = directory
                    .listFiles { file -> file.extension == "json" }
                    .orEmpty()
                    .mapNotNull { file ->
                        runCatching { json.decodeFromString<Project>(file.readText()) }.getOrNull()
                    }
                    .sortedByDescending { it.updatedAt }
                cache.value = loaded
                ready.value = true
            }
        }
    }

    override fun observeProjects(): Flow<List<Project>> = cache.map { list ->
        list.sortedByDescending { it.updatedAt }
    }

    override suspend fun getProject(id: String): Project? {
        ensureReady()
        return cache.value.firstOrNull { it.id == id }
    }

    override suspend fun upsert(project: Project): Project = withContext(io) {
        ensureReady()
        mutex.withLock {
            val stamped = project.copy(updatedAt = System.currentTimeMillis())
            writeFile(stamped)
            cache.value = cache.value
                .filterNot { it.id == stamped.id } + stamped
            stamped
        }
    }

    override suspend fun delete(id: String) = withContext(io) {
        ensureReady()
        mutex.withLock {
            fileFor(id).delete()
            cache.value = cache.value.filterNot { it.id == id }
        }
    }

    override suspend fun rename(id: String, title: String): Project? = withContext(io) {
        ensureReady()
        mutex.withLock {
            val current = cache.value.firstOrNull { it.id == id } ?: return@withLock null
            val stamped = current.copy(
                title = title.trim().ifBlank { current.title },
                updatedAt = System.currentTimeMillis()
            )
            writeFile(stamped)
            cache.value = cache.value.map { if (it.id == id) stamped else it }
            stamped
        }
    }

    private fun fileFor(id: String): File = File(directory, "$id.json")

    private fun writeFile(project: Project) {
        val target = fileFor(project.id)
        val tmp = File(directory, "${project.id}.tmp")
        tmp.writeText(json.encodeToString(project))
        if (!tmp.renameTo(target)) {
            target.writeText(tmp.readText())
            tmp.delete()
        }
    }

    private suspend fun ensureReady() {
        if (!ready.value) hydrate()
    }
}
