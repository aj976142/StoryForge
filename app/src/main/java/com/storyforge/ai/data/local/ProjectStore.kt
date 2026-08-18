package com.storyforge.ai.data.local

import com.storyforge.ai.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectStore {
    fun observeProjects(): Flow<List<Project>>
    suspend fun getProject(id: String): Project?
    suspend fun upsert(project: Project): Project
    suspend fun delete(id: String)
    suspend fun rename(id: String, title: String): Project?
}
