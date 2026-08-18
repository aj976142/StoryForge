package com.storyforge.ai.di

import android.content.Context
import com.storyforge.ai.data.ai.AiService
import com.storyforge.ai.data.ai.MockAiService
import com.storyforge.ai.data.local.JsonProjectStore
import com.storyforge.ai.data.local.SettingsStore
import com.storyforge.ai.data.repository.ProjectRepository
import com.storyforge.ai.data.repository.SettingsRepository
import java.io.File

/**
 * Manual composition root. Replace [aiService] with a networked
 * implementation when a provider key is available.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val projectStore = JsonProjectStore(File(appContext.filesDir, "projects"))
    val settingsStore = SettingsStore(appContext)

    val projects = ProjectRepository(projectStore)
    val settings = SettingsRepository(settingsStore)

    val aiService: AiService = MockAiService()
}
