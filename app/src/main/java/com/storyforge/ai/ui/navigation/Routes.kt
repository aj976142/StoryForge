package com.storyforge.ai.ui.navigation

object Routes {
    const val HOME = "home"
    const val INPUT = "input/{projectId}?mode={mode}"
    const val FORMAT = "format/{projectId}"
    const val GENERATION = "generation/{projectId}?continueWrite={continueWrite}&instruction={instruction}"
    const val EDITOR = "editor/{projectId}"
    const val PROJECTS = "projects"
    const val SETTINGS = "settings"

    fun input(projectId: String, mode: String) = "input/$projectId?mode=$mode"
    fun format(projectId: String) = "format/$projectId"
    fun generation(projectId: String, continueWrite: Boolean = false, instruction: String? = null): String {
        val safe = instruction.orEmpty().replace(" ", "%20").replace("&", "%26")
        return "generation/$projectId?continueWrite=$continueWrite&instruction=$safe"
    }
    fun editor(projectId: String) = "editor/$projectId"
}
