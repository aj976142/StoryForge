package com.storyforge.ai.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storyforge.ai.ui.components.EmptyState
import com.storyforge.ai.ui.components.ErrorBanner
import com.storyforge.ai.ui.components.LoadingBlock
import com.storyforge.ai.ui.components.formatBadge
import com.storyforge.ai.ui.components.formatModified
import com.storyforge.ai.ui.components.previewLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(viewModel: ProjectsViewModel, onOpen: (String) -> Unit, onCreate: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var sortExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Projects") },
            actions = {
                IconButton(onClick = { sortExpanded = true }) { Icon(Icons.Outlined.Sort, "Sort") }
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    ProjectSort.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(when (sort) { ProjectSort.UPDATED -> "Recently updated"; ProjectSort.CREATED -> "Recently created"; ProjectSort.TITLE -> "Title A–Z" }) },
                            onClick = { viewModel.setSort(sort); sortExpanded = false }
                        )
                    }
                }
            }
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, "Search") },
                placeholder = { Text("Search projects or ideas") }
            )
        }
        state.error?.let { Column(Modifier.padding(16.dp)) { ErrorBanner(it, onDismiss = viewModel::dismissError) } }
        when {
            state.loading -> LoadingBlock("Loading projects…")
            state.items.isEmpty() -> EmptyState("Nothing saved yet", "Finished drafts will live here. You can rename or delete them anytime.", "New Project", onCreate)
            state.visibleItems.isEmpty() -> EmptyState("No matches", "Try another title or keyword.", "Clear search", { viewModel.setQuery("") })
            else -> LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.visibleItems, key = { it.id }) { project ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(project.title.ifBlank { "Untitled project" }, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("${formatBadge(project.format)} · ${formatModified(project.updatedAt)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text(project.previewLine().ifBlank { "Empty draft" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Row {
                                TextButton(onClick = { onOpen(project.id) }) { Text("Open") }
                                TextButton(onClick = { renameText = project.title; viewModel.beginRename(project.id) }) { Text("Rename") }
                                TextButton(onClick = { pendingDelete = project.id }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }

    val renamingId = state.renamingId
    if (renamingId != null) AlertDialog(
        onDismissRequest = viewModel::cancelRename,
        title = { Text("Rename project") },
        text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true, label = { Text("Title") }) },
        confirmButton = { TextButton(onClick = { viewModel.rename(renamingId, renameText) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = viewModel::cancelRename) { Text("Cancel") } }
    )

    pendingDelete?.let { id -> AlertDialog(
        onDismissRequest = { pendingDelete = null },
        title = { Text("Delete project?") },
        text = { Text("This cannot be undone.") },
        confirmButton = { TextButton(onClick = { viewModel.delete(id); pendingDelete = null }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
    ) }
}
