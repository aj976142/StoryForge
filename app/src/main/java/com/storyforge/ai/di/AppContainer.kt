package com.storyforge.ai.di

import android.content.Context
import com.storyforge.ai.data.ai.AiService
import com.storyforge.ai.data.ai.ConfiguredAiService
import com.storyforge.ai.data.ai.HttpAiService
import com.storyforge.ai.data.ai.MockAiService
import com.storyforge.ai.data.local.JsonProjectStore
import com.storyforge.ai.data.local.SecretStore
import com.storyforge.ai.data.local.SettingsStore
import com.storyforge.ai.data.repository.ProjectRepository
import com.storyforge.ai.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.io.File

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val projectStore = JsonProjectStore(File(appContext.filesDir, "projects"))
    val settingsStore = SettingsStore(appContext)
    val secretStore = SecretStore(appContext)
    val projects = ProjectRepository(projectStore)
    val settings = SettingsRepository(settingsStore)
    val httpAiService = HttpAiService(
        settings = { settings.ai.first() },
        secretStore = secretStore
    )
    val demoAiService: AiService = MockAiService()
    val aiService: AiService = ConfiguredAiService(settings, httpAiService, demoAiService)
}
