package com.storyforge.ai.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.storyforge.ai.StoryForgeApplication
import com.storyforge.ai.di.AppContainer
import com.storyforge.ai.domain.model.InputMode
import com.storyforge.ai.ui.components.StoryForgeScaffold
import com.storyforge.ai.ui.editor.EditorScreen
import com.storyforge.ai.ui.editor.EditorViewModel
import com.storyforge.ai.ui.format.FormatScreen
import com.storyforge.ai.ui.generation.GenerationScreen
import com.storyforge.ai.ui.generation.GenerationViewModel
import com.storyforge.ai.ui.home.HomeScreen
import com.storyforge.ai.ui.home.HomeViewModel
import com.storyforge.ai.ui.input.InputScreen
import com.storyforge.ai.ui.input.InputViewModel
import com.storyforge.ai.ui.projects.ProjectsScreen
import com.storyforge.ai.ui.projects.ProjectsViewModel
import com.storyforge.ai.ui.settings.SettingsScreen
import com.storyforge.ai.ui.settings.SettingsViewModel
import com.storyforge.ai.ui.theme.StoryForgeTheme

@Composable
fun StoryForgeRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as StoryForgeApplication
    val container = app.container
    val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container.settings, container.secretStore, container.httpAiService))
    val settingsState by settingsVm.state.collectAsStateWithLifecycle()
    StoryForgeTheme(themeMode = settingsState.themeMode) { StoryForgeNav(container, settingsVm) }
}

@Composable
fun StoryForgeNav(container: AppContainer, settingsVm: SettingsViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val tab = when {
        route.startsWith("projects") -> "projects"
        route.startsWith("settings") -> "settings"
        else -> "home"
    }
    val focusedScreen = route.startsWith("input") || route.startsWith("format") || route.startsWith("generation") || route.startsWith("editor")

    fun goHome() {
        nav.navigate(Routes.HOME) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    StoryForgeScaffold(
        selected = tab,
        onHome = ::goHome,
        onProjects = { nav.navigate(Routes.PROJECTS) { launchSingleTop = true } },
        onSettings = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } },
        showBottomNavigation = !focusedScreen
    ) {
        NavHost(navController = nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(container.projects))
                HomeScreen(
                    vm,
                    onOpenInput = { id, mode -> nav.navigate(Routes.input(id, mode)) },
                    onOpenProject = { id -> nav.navigate(Routes.editor(id)) },
                    onOpenProjects = { nav.navigate(Routes.PROJECTS) { launchSingleTop = true } }
                )
            }
            composable(route = Routes.INPUT, arguments = listOf(navArgument("projectId") { type = NavType.StringType }, navArgument("mode") { type = NavType.StringType; defaultValue = "TEXT" })) { entry ->
                val id = entry.arguments?.getString("projectId").orEmpty()
                val mode = runCatching { InputMode.valueOf(entry.arguments?.getString("mode") ?: "TEXT") }.getOrDefault(InputMode.TEXT)
                val app = LocalContext.current.applicationContext as Application
                val vm: InputViewModel = viewModel(factory = InputViewModel.factory(app, container.projects, id, mode))
                InputScreen(vm, onBack = { nav.popBackStack() }, onContinue = { nav.navigate(Routes.format(it)) })
            }
            composable(route = Routes.FORMAT, arguments = listOf(navArgument("projectId") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("projectId").orEmpty()
                FormatScreen(id, container.projects, onBack = { nav.popBackStack() }, onContinue = { nav.navigate(Routes.generation(it)) })
            }
            composable(route = Routes.GENERATION, arguments = listOf(navArgument("projectId") { type = NavType.StringType }, navArgument("continueWrite") { type = NavType.BoolType; defaultValue = false })) { entry ->
                val id = entry.arguments?.getString("projectId").orEmpty()
                val continueWrite = entry.arguments?.getBoolean("continueWrite") ?: false
                val vm: GenerationViewModel = viewModel(factory = GenerationViewModel.factory(id, continueWrite, container.projects, container.settings, container.aiService))
                GenerationScreen(vm, onFinished = { finishedId -> nav.navigate(Routes.editor(finishedId)) { popUpTo(Routes.HOME) } }, onBack = { nav.popBackStack() })
            }
            composable(route = Routes.EDITOR, arguments = listOf(navArgument("projectId") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("projectId").orEmpty()
                val vm: EditorViewModel = viewModel(factory = EditorViewModel.factory(id, container.projects))
                EditorScreen(vm, onBack = { nav.popBackStack() }, onRegenerate = { nav.navigate(Routes.generation(it, false)) }, onContinueWriting = { nav.navigate(Routes.generation(it, true)) })
            }
            composable(Routes.PROJECTS) {
                val vm: ProjectsViewModel = viewModel(factory = ProjectsViewModel.factory(container.projects))
                ProjectsScreen(vm, onOpen = { nav.navigate(Routes.editor(it)) }, onCreate = ::goHome)
            }
            composable(Routes.SETTINGS) { SettingsScreen(settingsVm) }
        }
    }
}
