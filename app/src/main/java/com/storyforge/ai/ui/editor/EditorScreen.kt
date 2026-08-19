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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storyforge.ai.ui.components.ErrorBanner

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onRegenerate: (String) -> Unit,
    onContinueWriting: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val wordCount = state.text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    val scrollState = rememberScrollState()
    var menuExpanded by remember { mutableStateOf(false) }

    fun copyStory() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("StoryForge", state.text))
        viewModel.markCopied()
    }

    fun shareStory() {
        val shareText = buildString {
            if (state.title.isNotBlank()) append(state.title.trim()).append("\\n\\n")
            append(state.text)
        }
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                },
                "Share story"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Editor",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share story") },
                            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                shareStory()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy story") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                copyStory()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Regenerate") },
                            leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                state.project?.id?.let(onRegenerate)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Continue writing") },
                            onClick = {
                                menuExpanded = false
                                state.project?.id?.let(onContinueWriting)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Save draft") },
                            leadingIcon = { Icon(Icons.Outlined.Save, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                viewModel.save()
                            }
                        )
                    }
                }
            }
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.project == null -> Column(Modifier.padding(16.dp)) {
                ErrorBanner(state.error ?: "Missing project", onDismiss = onBack)
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (state.title.isBlank()) Text(
                            "Untitled story",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 24.sp
                        )
                        inner()
                    }
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(vertical = 8.dp)
                ) {
                    BasicTextField(
                        value = state.text,
                        onValueChange = viewModel::onTextChange,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 17.sp,
                            lineHeight = 27.sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (state.text.isBlank()) Text(
                                "Start writing…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 17.sp
                            )
                            inner()
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when {
                            state.saving -> "Saving…"
                            state.saved -> "Saved"
                            state.copied -> "Copied"
                            else -> state.project?.format?.displayName ?: ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$wordCount words",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                state.error?.let {
                    Spacer(Modifier.height(4.dp))
                    ErrorBanner(it)
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 10.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { onRegenerate(state.project!!.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Regenerate")
                        }
                        androidx.compose.material3.OutlinedButton(
                            onClick = { onContinueWriting(state.project!!.id) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Continue") }
                        IconButton(onClick = { copyStory() }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy")
                        }
                        Button(onClick = { viewModel.save() }, modifier = Modifier.height(48.dp)) {
                            Icon(Icons.Outlined.Save, contentDescription = "Save", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
