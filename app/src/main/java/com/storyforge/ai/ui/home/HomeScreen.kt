package com.storyforge.ai.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storyforge.ai.domain.model.InputMode
import com.storyforge.ai.ui.components.EmptyState
import com.storyforge.ai.ui.components.ErrorBanner
import com.storyforge.ai.ui.components.formatBadge
import com.storyforge.ai.ui.components.formatModified
import com.storyforge.ai.ui.components.previewLine

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenInput: (projectId: String, mode: String) -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenProjects: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.LocalFireDepartment, "StoryForge", tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text("StoryForge", style = MaterialTheme.typography.titleLarge, color = textPrimary)
                Text("Your writing space", style = MaterialTheme.typography.labelMedium, color = textSecondary)
            }
            IconButton(onClick = onOpenProjects) {
                Icon(Icons.Outlined.Search, "Search projects", tint = textSecondary)
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Turn an idea into something real.", style = MaterialTheme.typography.titleMedium, color = textPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Start with a rough thought, voice note, or a blank page.", style = MaterialTheme.typography.bodyMedium, color = textSecondary)
            }
        }

        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { viewModel.startProject(InputMode.TEXT) { onOpenInput(it, "TEXT") } },
            enabled = !state.creating,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text(if (state.creating) "Opening…" else "New Project") }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(
                onClick = { viewModel.startProject(InputMode.VOICE) { onOpenInput(it, "VOICE") } },
                enabled = !state.creating,
                modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.KeyboardVoice, null); Spacer(Modifier.size(7.dp)); Text("Voice")
            }
            FilledTonalButton(
                onClick = { viewModel.startProject(InputMode.TEXT) { onOpenInput(it, "TEXT") } },
                enabled = !state.creating,
                modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.EditNote, null); Spacer(Modifier.size(7.dp)); Text("Type idea")
            }
        }

        state.error?.let { Spacer(Modifier.height(14.dp)); ErrorBanner(it, onDismiss = viewModel::dismissError) }
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Recent Projects", style = MaterialTheme.typography.titleLarge, color = textPrimary)
                Text("Your latest drafts", style = MaterialTheme.typography.bodySmall, color = textSecondary)
            }
            if (state.recent.isNotEmpty()) Text("${state.recent.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        Spacer(Modifier.height(12.dp))

        when {
            state.loading -> Text("Loading your projects…", color = textSecondary)
            state.recent.isEmpty() -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                EmptyState(
                    title = "No stories yet",
                    body = "Start with a messy thought. We'll shape it into a story, script, article, or polished prose.",
                    actionLabel = "Create your first project",
                    onAction = { viewModel.startProject(InputMode.TEXT) { onOpenInput(it, "TEXT") } }
                )
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.recent.forEach { project ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onOpenProject(project.id) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(project.title.ifBlank { "Untitled project" }, style = MaterialTheme.typography.titleMedium, color = textPrimary)
                            Spacer(Modifier.height(5.dp))
                            Text(project.previewLine().ifBlank { "Empty draft" }, style = MaterialTheme.typography.bodyMedium, color = textSecondary)
                            Spacer(Modifier.height(9.dp))
                            Text("${formatBadge(project.format)} · ${formatModified(project.updatedAt)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
