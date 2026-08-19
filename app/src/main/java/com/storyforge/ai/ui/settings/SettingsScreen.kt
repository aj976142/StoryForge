package com.storyforge.ai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings") })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
            SectionTitle("AI provider")
            Text(
                "Use your own API key. StoryForge stores it encrypted on this device and never puts it in the project or GitHub.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            ChipRow(
                options = listOf("openai-compatible" to "OpenAI-compatible", "mock" to "Demo / Offline"),
                selected = state.ai.provider,
                onSelect = viewModel::setProvider
            )
            if (state.ai.provider == "openai-compatible") {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = state.ai.endpoint, onValueChange = viewModel::setEndpoint, label = { Text("API endpoint") }, supportingText = { Text("OpenAI-compatible chat completions endpoint") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = state.ai.model, onValueChange = viewModel::setModel, label = { Text("Model") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(if (state.ai.apiKeyConfigured) "API key saved — enter a new one to replace it" else "API key") },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { showKey = !showKey }) { Icon(if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Show/hide key") }
                            if (state.ai.apiKeyConfigured) IconButton(onClick = viewModel::clearApiKey) { Icon(Icons.Outlined.Delete, "Remove API key") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { viewModel.saveApiKey(apiKey); apiKey = "" }, modifier = Modifier.weight(1f)) { Text("Save key") }
                    OutlinedButton(onClick = viewModel::testConnection, enabled = !state.testingConnection, modifier = Modifier.weight(1f)) { Text(if (state.testingConnection) "Testing…" else "Test connection") }
                }
                if (state.ai.apiKeyConfigured) {
                    Spacer(Modifier.height(8.dp))
                    Text("✓ API key configured", color = MaterialTheme.colorScheme.primary)
                }
                state.connectionMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text("Demo mode works without an API key and is useful for exploring the app offline.", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(28.dp))
            SectionTitle("Writing preferences")
            Label("Tone")
            ChipRow(listOf("Cinematic", "Literary", "Casual", "Dramatic", "Poetic", "Professional").map { it to it }, state.writing.tone) { viewModel.setWriting(state.writing.copy(tone = it)) }
            Label("Length")
            ChipRow(listOf("Short", "Medium", "Long").map { it to it }, state.writing.length) { viewModel.setWriting(state.writing.copy(length = it)) }
            Label("Point of view")
            ChipRow(listOf("First person", "Second person", "Third person").map { it to it }, state.writing.pov) { viewModel.setWriting(state.writing.copy(pov = it)) }
            Label("Language")
            ChipRow(listOf("English", "Hindi", "Marathi", "Spanish", "French").map { it to it }, state.writing.language) { viewModel.setWriting(state.writing.copy(language = it)) }

            Spacer(Modifier.height(24.dp))
            SectionTitle("Theme")
            ChipRow(listOf("system" to "System", "light" to "Light", "dark" to "Dark"), state.themeMode, viewModel::setTheme)
        }
    }
}

@Composable private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(8.dp)) }
@Composable private fun Label(text: String) { Spacer(Modifier.height(12.dp)); Text(text, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(6.dp)) }

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ChipRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { (value, label) -> FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) }) }
    }
}
