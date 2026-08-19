package com.storyforge.ai.ui.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storyforge.ai.ui.components.ErrorBanner
import com.storyforge.ai.util.StoryBrain

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onRegenerate: (String) -> Unit,
    onContinueWriting: (String) -> Unit,
    onWritingAction: (String, String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val wordCount = state.text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    val scrollState = rememberScrollState()
    var menuExpanded by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showBrain by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    var renameText by remember(state.title) { mutableStateOf(state.title) }

    fun copyStory() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("StoryForge", state.text))
        viewModel.markCopied()
    }
    fun shareStory() {
        val shareText = buildString { if (state.title.isNotBlank()) append(state.title.trim()).append("\n\n"); append(state.text) }
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }, "Share story"))
    }

    Column(Modifier.fillMaxSize().navigationBarsPadding().background(MaterialTheme.colorScheme.background)) {
        Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
                Text("Editor", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menuExpanded = false; renameText = state.title; showRename = true })
                        DropdownMenuItem(text = { Text("AI writing tools") }, leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null) }, onClick = { menuExpanded = false; showActions = true })
                        DropdownMenuItem(text = { Text("Story Brain") }, leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null) }, onClick = { menuExpanded = false; showBrain = true })
                        DropdownMenuItem(text = { Text("Writing stats") }, leadingIcon = { Icon(Icons.Outlined.Info, null) }, onClick = { menuExpanded = false; showStats = true })
                        DropdownMenuItem(text = { Text("Share story") }, leadingIcon = { Icon(Icons.Outlined.Share, null) }, onClick = { menuExpanded = false; shareStory() })
                        DropdownMenuItem(text = { Text("Copy story") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { menuExpanded = false; copyStory() })
                        DropdownMenuItem(text = { Text("Regenerate") }, leadingIcon = { Icon(Icons.Outlined.Refresh, null) }, onClick = { menuExpanded = false; state.project?.id?.let(onRegenerate) })
                        DropdownMenuItem(text = { Text("Continue writing") }, onClick = { menuExpanded = false; state.project?.id?.let(onContinueWriting) })
                        DropdownMenuItem(text = { Text("Save draft") }, leadingIcon = { Icon(Icons.Outlined.Save, null) }, onClick = { menuExpanded = false; viewModel.save() })
                    }
                }
            }
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.project == null -> Column(Modifier.padding(16.dp)) { ErrorBanner(state.error ?: "Missing project", onDismiss = onBack) }
            else -> Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(8.dp))
                BasicTextField(value = state.title, onValueChange = viewModel::onTitleChange, singleLine = true, textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> if (state.title.isBlank()) Text("Untitled story", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 25.sp); inner() })
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState).padding(vertical = 8.dp)) {
                    BasicTextField(value = state.text, onValueChange = viewModel::onTextChange, textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 17.sp, lineHeight = 27.sp), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> if (state.text.isBlank()) Text("Start writing…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp); inner() })
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(when { state.saving -> "Saving…"; state.saved -> "Saved"; state.copied -> "Copied"; else -> state.project?.format?.displayName ?: "" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$wordCount words", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.error?.let { Spacer(Modifier.height(4.dp)); ErrorBanner(it) }
                Surface(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 1.dp) {
                    Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onRegenerate(state.project!!.id) }, Modifier.weight(1f)) { Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Regenerate") }
                        OutlinedButton(onClick = { onContinueWriting(state.project!!.id) }, Modifier.weight(1f)) { Text("Continue") }
                        IconButton(onClick = { showActions = true }) { Icon(Icons.Outlined.AutoAwesome, "AI tools") }
                        IconButton(onClick = { copyStory() }) { Icon(Icons.Outlined.ContentCopy, "Copy") }
                        Button(onClick = { viewModel.save() }, Modifier.height(48.dp)) { Icon(Icons.Outlined.Save, "Save", Modifier.size(18.dp)) }
                    }
                }
            }
        }
    }

    if (showActions) {
        AlertDialog(
            onDismissRequest = { showActions = false },
            title = { Text("AI writing tools") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("These actions keep your idea and voice, then change only what you ask for.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(
                        "Polish writing" to "Polish grammar, clarity, flow and word choice while preserving my voice.",
                        "Make it more cinematic" to "Make the writing more cinematic using stronger scenes, sensory detail and emotional beats without changing the core events.",
                        "Expand" to "Expand the writing with useful detail, context and stronger transitions without adding contradictions.",
                        "Shorten" to "Make the writing substantially shorter while preserving the key meaning, facts and emotional point.",
                        "Make natural" to "Rewrite this to sound natural and human, removing stiff or generic AI-like phrasing while preserving meaning.",
                        "Convert to screenplay" to "Convert this material into a professional movie screenplay while preserving the story, characters and events."
                    ).forEach { (label, instruction) ->
                        OutlinedButton(onClick = { showActions = false; state.project?.id?.let { onWritingAction(it, instruction) } }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showActions = false }) { Text("Cancel") } }
        )
    }

    if (showRename) AlertDialog(onDismissRequest = { showRename = false }, title = { Text("Rename project") }, text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true, label = { Text("Title") }) }, confirmButton = { TextButton(onClick = { viewModel.onTitleChange(renameText.trim()); viewModel.save(); showRename = false }) { Text("Save") } }, dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } })

    if (showStats) AlertDialog(onDismissRequest = { showStats = false }, title = { Text("Writing stats") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("$wordCount words"); Text("${state.text.length} characters"); Text("Estimated reading time: ${((wordCount + 179) / 180).coerceAtLeast(1)} min"); Text("Format: ${state.project?.format?.displayName ?: "Draft"}") } }, confirmButton = { TextButton(onClick = { showStats = false }) { Text("Done") } })

    if (showBrain) {
        val brain = remember(state.text) { StoryBrain.analyze(state.text) }
        AlertDialog(onDismissRequest = { showBrain = false }, title = { Text("Story Brain") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Continuity snapshot", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary); Text("Characters: ${brain.characters.ifEmpty { listOf("Not detected yet") }.joinToString(", ")}"); Text("Places: ${brain.places.ifEmpty { listOf("Not detected yet") }.joinToString(", ")}"); Text("Themes: ${brain.themes.ifEmpty { listOf("Not enough text yet") }.joinToString(", ")}"); Text("${brain.wordCount} words · ~${brain.readingMinutes} min read", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("This snapshot is generated offline on your device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, confirmButton = { TextButton(onClick = { showBrain = false }) { Text("Done") } })
    }
}
