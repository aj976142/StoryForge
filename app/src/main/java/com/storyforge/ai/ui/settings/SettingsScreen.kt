package com.storyforge.ai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings") })
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            SectionTitle("Theme")
            ChipRow(
                options = listOf("system" to "System", "light" to "Light", "dark" to "Dark"),
                selected = state.themeMode,
                onSelect = viewModel::setTheme
            )

            Spacer(Modifier.height(24.dp))
            SectionTitle("AI provider")
            Text(
                "A real provider can be wired through AiService. V1 ships with a local mock writer so the app runs without an API key.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            ChipRow(
                options = listOf(
                    "mock" to "Mock (on-device)",
                    "openai" to "OpenAI",
                    "gemini" to "Gemini",
                    "anthropic" to "Claude"
                ),
                selected = state.ai.provider,
                onSelect = viewModel::setProvider
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = "",
                onValueChange = {},
                enabled = false,
                label = { Text("API key") },
                placeholder = { Text("Coming in a later release") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Active model: ${state.ai.model}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(24.dp))
            SectionTitle("Writing preferences")
            Label("Tone")
            ChipRow(
                options = listOf("Cinematic", "Literary", "Casual", "Dramatic").map { it to it },
                selected = state.writing.tone,
                onSelect = { viewModel.setWriting(state.writing.copy(tone = it)) }
            )
            Label("Length")
            ChipRow(
                options = listOf("Short", "Medium", "Long").map { it to it },
                selected = state.writing.length,
                onSelect = { viewModel.setWriting(state.writing.copy(length = it)) }
            )
            Label("Point of view")
            ChipRow(
                options = listOf("First person", "Second person", "Third person").map { it to it },
                selected = state.writing.pov,
                onSelect = { viewModel.setWriting(state.writing.copy(pov = it)) }
            )
            Label("Language")
            ChipRow(
                options = listOf("English", "Spanish", "French").map { it to it },
                selected = state.writing.language,
                onSelect = { viewModel.setWriting(state.writing.copy(language = it)) }
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Label(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(text, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}
