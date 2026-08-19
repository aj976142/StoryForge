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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.storyforge.ai.data.ai.AiProviderCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val liveModels by viewModel.models.collectAsStateWithLifecycle()
    val loadingModels by viewModel.isLoadingModels.collectAsStateWithLifecycle()
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val providerOptions = AiProviderCatalog.presets
    val selectedPreset = AiProviderCatalog.preset(state.ai.provider)
    val curatedModels = AiProviderCatalog.options(state.ai.provider, state.ai.model)
    val modelOptions = (liveModels + curatedModels).distinct().take(200)

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings", color = textPrimary) })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            SettingsCard {
                SectionTitle("AI provider")
                Text("Bring your own API key. It stays encrypted on this device and is never included in the project or GitHub.", style = MaterialTheme.typography.bodyMedium, color = textSecondary)
                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = !providerExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPreset?.name ?: "Custom / ${state.ai.provider}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(providerExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false }
                    ) {
                        providerOptions.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    viewModel.setProvider(preset.id)
                                    providerExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Demo / Offline") },
                            onClick = {
                                viewModel.setProvider(AiProviderCatalog.MOCK)
                                providerExpanded = false
                            }
                        )
                    }
                }

                if (selectedPreset?.note?.isNotBlank() == true) {
                    Spacer(Modifier.height(6.dp))
                    Text(selectedPreset.note, style = MaterialTheme.typography.bodySmall, color = textSecondary)
                }

                if (state.ai.provider != AiProviderCatalog.MOCK) {
                    Spacer(Modifier.height(12.dp))
                    ExposedDropdownMenuBox(
                        expanded = modelExpanded,
                        onExpandedChange = { modelExpanded = !modelExpanded }
                    ) {
                        OutlinedTextField(
                            value = state.ai.model,
                            onValueChange = viewModel::setModel,
                            label = { Text("AI model") },
                            supportingText = { Text("Preset models are included; Refresh loads models currently exposed by your provider") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false }
                        ) {
                            modelOptions.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        viewModel.setModel(model)
                                        modelExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = viewModel::refreshModels,
                        enabled = !loadingModels,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(Modifier.height(0.dp))
                        Text(if (loadingModels) "Loading available models…" else "Refresh available models")
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.ai.endpoint,
                        onValueChange = viewModel::setEndpoint,
                        label = { Text("API endpoint") },
                        supportingText = { Text("You can override the preset endpoint when needed") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text(if (state.ai.apiKeyConfigured) "API key saved · enter a new one to replace" else "API key") },
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { showKey = !showKey }) { Icon(if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Show or hide API key") }
                                if (state.ai.apiKeyConfigured) IconButton(onClick = viewModel::clearApiKey) { Icon(Icons.Outlined.Delete, "Remove API key") }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { viewModel.saveApiKey(apiKey); apiKey = "" }, modifier = Modifier.weight(1f)) { Text("Save key") }
                        OutlinedButton(onClick = viewModel::testConnection, enabled = !state.testingConnection, modifier = Modifier.weight(1f)) { Text(if (state.testingConnection) "Testing…" else "Test") }
                    }
                    if (state.ai.apiKeyConfigured) {
                        Spacer(Modifier.height(8.dp))
                        Text("API key configured", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                    state.connectionMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Spacer(Modifier.height(10.dp))
                    Text("Demo mode works without an API key and is useful for testing the app offline.", style = MaterialTheme.typography.bodyMedium, color = textSecondary)
                }
            }

            Spacer(Modifier.height(14.dp))
            SettingsCard {
                SectionTitle("Writing preferences")
                Label("Tone"); ChipRow(listOf("Cinematic", "Literary", "Casual", "Dramatic", "Poetic", "Professional").map { it to it }, state.writing.tone) { viewModel.setWriting(state.writing.copy(tone = it)) }
                Label("Length"); ChipRow(listOf("Short", "Medium", "Long").map { it to it }, state.writing.length) { viewModel.setWriting(state.writing.copy(length = it)) }
                Label("Point of view"); ChipRow(listOf("First person", "Second person", "Third person").map { it to it }, state.writing.pov) { viewModel.setWriting(state.writing.copy(pov = it)) }
                Label("Language"); ChipRow(listOf("English", "Hindi", "Marathi", "Spanish", "French").map { it to it }, state.writing.language) { viewModel.setWriting(state.writing.copy(language = it)) }
            }

            Spacer(Modifier.height(14.dp))
            SettingsCard {
                SectionTitle("Appearance")
                Text("Choose how StoryForge looks on your device.", style = MaterialTheme.typography.bodyMedium, color = textSecondary)
                Spacer(Modifier.height(10.dp))
                ChipRow(listOf("system" to "System", "light" to "Light", "dark" to "Dark"), state.themeMode, viewModel::setTheme)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable private fun SettingsCard(content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(7.dp))
}

@Composable private fun Label(text: String) {
    Spacer(Modifier.height(14.dp)); Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface); Spacer(Modifier.height(7.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ChipRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { (value, label) -> FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) }) }
    }
}
