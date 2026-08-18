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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
    onOpenProject: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text("StoryForge AI", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Speak a spark. Leave with a story.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { viewModel.startProject(InputMode.TEXT) { onOpenInput(it, "TEXT") } },
            enabled = !state.creating,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(if (state.creating) "Opening…" else "New Project")
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(
                onClick = { viewModel.startProject(InputMode.VOICE) { onOpenInput(it, "VOICE") } },
                enabled = !state.creating,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.KeyboardVoice, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Voice Input")
            }
            FilledTonalButton(
                onClick = { viewModel.startProject(InputMode.TEXT) { onOpenInput(it, "TEXT") } },
                enabled = !state.creating,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.EditNote, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Type Idea")
            }
        }

        state.error?.let {
            Spacer(Modifier.height(16.dp))
            ErrorBanner(it, onDismiss = viewModel::dismissError)
        }

        Spacer(Modifier.height(28.dp))
        Text("Recent Projects", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        when {
            state.loading -> Text("Loading your forge…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.recent.isEmpty() -> EmptyState(
                title = "No stories yet",
                body = "Start with a messy thought. We'll shape it into a novel, script, or clean prose.",
                actionLabel = "New Project",
                onAction = { viewModel.startProject(InputMode.TEXT) { onOpenInput(it, "TEXT") } }
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.recent.forEach { project ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenProject(project.id) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(project.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                project.previewLine().ifBlank { "Empty draft" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${formatBadge(project.format)} · ${formatModified(project.updatedAt)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
