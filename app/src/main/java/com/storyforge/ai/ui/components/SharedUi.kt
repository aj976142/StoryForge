package com.storyforge.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.storyforge.ai.domain.model.OutputFormat
import com.storyforge.ai.domain.model.Project
import com.storyforge.ai.util.GenerationProvenance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StoryForgeScaffold(
    selected: String,
    onHome: () -> Unit,
    onProjects: () -> Unit,
    onSettings: () -> Unit,
    showBottomNavigation: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.weight(1f)) { content() }
        if (showBottomNavigation) {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(selected = selected == "home", onClick = onHome, icon = { Icon(Icons.Outlined.Home, "Home") }, label = { Text("Home") })
                NavigationBarItem(selected = selected == "projects", onClick = onProjects, icon = { Icon(Icons.Outlined.FolderOpen, "Projects") }, label = { Text("Projects") })
                NavigationBarItem(selected = selected == "settings", onClick = onSettings, icon = { Icon(Icons.Outlined.Settings, "Settings") }, label = { Text("Settings") })
            }
        } else {
            Box(Modifier.navigationBarsPadding().height(0.dp))
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.AutoStories, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ErrorBanner(message: String, onRetry: (() -> Unit)? = null, onDismiss: (() -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (onDismiss != null) TextButton(onClick = onDismiss) { Text("Dismiss") }
                if (onRetry != null) TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
fun LoadingBlock(label: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

fun formatModified(millis: Long): String = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(millis))
fun formatBadge(format: OutputFormat): String = format.displayName

fun Project.previewLine(): String {
    val trusted = generatedText.isNotBlank() &&
        generatedForIdeaHash.isNotBlank() &&
        generatedForIdeaHash == GenerationProvenance.fingerprint(rawIdea, format)
    return (if (trusted) generatedText else rawIdea).replace(Regex("\\s+"), " ").take(110)
}
