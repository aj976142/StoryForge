package com.storyforge.ai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Ember,
    onPrimary = Ink,
    primaryContainer = EmberDeep,
    onPrimaryContainer = Paper,
    secondary = Forest,
    onSecondary = Paper,
    background = Ink,
    onBackground = Paper,
    surface = InkElevated,
    onSurface = Paper,
    surfaceVariant = InkSoft,
    onSurfaceVariant = Mist,
    error = Danger,
    onError = Paper,
    outline = Slate
)

private val LightColors = lightColorScheme(
    primary = EmberDeep,
    onPrimary = Color.White,
    primaryContainer = Ember,
    onPrimaryContainer = Ink,
    secondary = Forest,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = PaperCard,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFE6D8),
    onSurfaceVariant = Slate,
    error = Danger,
    onError = Color.White,
    outline = Mist
)

@Composable
fun StoryForgeTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = StoryForgeTypography,
        content = content
    )
}
