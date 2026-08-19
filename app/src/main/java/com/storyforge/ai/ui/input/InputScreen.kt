package com.storyforge.ai.ui.input

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storyforge.ai.domain.model.InputMode
import com.storyforge.ai.domain.model.OutputFormat
import com.storyforge.ai.ui.components.ErrorBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    viewModel: InputViewModel,
    onBack: () -> Unit,
    onContinue: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) viewModel.toggleVoice() }

    LaunchedEffect(state.mode) {
        if (state.mode == InputMode.VOICE && state.loaded && state.text.isBlank()) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (granted) viewModel.toggleVoice()
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (state.mode == InputMode.VOICE) "Voice your idea" else "Type your idea") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }
        )
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 4.dp)) {
            Text("Dump the raw thought. StoryForge will shape it.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Text("Create as", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(7.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(OutputFormat.entries) { format ->
                    FilterChip(selected = state.format == format, onClick = { viewModel.setFormat(format) }, label = { Text(format.displayName) })
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.text,
                onValueChange = viewModel::onTextChange,
                modifier = Modifier.fillMaxWidth().weight(1f),
                placeholder = { Text("A detective who can only remember other people's dreams…") },
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${state.words} words · ${state.characters} characters", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(state.savedHint ?: if (state.saving) "Saving…" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            state.error?.let { ErrorBanner(it) }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (granted) viewModel.toggleVoice() else permission.launch(Manifest.permission.RECORD_AUDIO)
                }) { Icon(if (state.listening) Icons.Outlined.Stop else Icons.Outlined.Mic, "Microphone") }
                FilledTonalIconButton(onClick = viewModel::clear) { Icon(Icons.Outlined.Close, "Clear") }
                Button(onClick = { viewModel.continueNext(onContinue) }, enabled = state.canContinue, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text("Build ${state.format.displayName}")
                }
            }
        }
    }
}
