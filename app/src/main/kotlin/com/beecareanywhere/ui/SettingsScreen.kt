package com.beecareanywhere.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.beecareanywhere.data.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val language by viewModel.language.collectAsStateWithLifecycle(initialValue = Settings.Language.English)
    val strings = strings(language)
    val modelInfo by viewModel.modelInfo.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settings) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            LanguageSection(
                selected = language,
                onSelect = viewModel::setLanguage,
                strings = strings,
            )
            HorizontalDivider()
            ModelSection(
                info = modelInfo,
                onDelete = { showDeleteDialog = true },
                strings = strings,
            )
            HorizontalDivider()
            AboutSection(strings)
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmation(
            onConfirm = {
                viewModel.deleteModel()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
            strings = strings,
        )
    }
}

@Composable
private fun LanguageSection(
    selected: Settings.Language,
    onSelect: (Settings.Language) -> Unit,
    strings: UiStrings,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.responseLanguage, style = MaterialTheme.typography.titleMedium)
        Text(
            strings.responseLanguageDescription,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            strings.uiLanguageDescription,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Settings.Language.entries.forEach { lang ->
                FilterChip(
                    selected = lang == selected,
                    onClick = { onSelect(lang) },
                    label = { Text(lang.name) },
                )
            }
        }
    }
}

@Composable
private fun ModelSection(
    info: SettingsViewModel.ModelInfo?,
    onDelete: () -> Unit,
    strings: UiStrings,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.model, style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val i = info
                if (i == null) {
                    Text(strings.loading, style = MaterialTheme.typography.bodyMedium)
                } else {
                    KeyValue(strings.filename, i.filename)
                    KeyValue(strings.installed, if (i.installed) strings.yes else strings.no)
                    if (i.installed) {
                        KeyValue(strings.size, formatBytes(i.sizeBytes))
                        KeyValue(strings.path, i.path ?: "-")
                    }
                    KeyValue(strings.pinnedSha, i.sha256 ?: strings.noneVerificationSkipped)
                }
            }
        }
        AssistChip(
            onClick = {},
            label = { Text(strings.stubModelActive) },
        )
        OutlinedButton(
            onClick = onDelete,
            enabled = info?.installed == true,
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null)
            Text(strings.deleteModelFile, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun AboutSection(strings: UiStrings) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(strings.about, style = MaterialTheme.typography.titleMedium)
        KeyValue(strings.version, "0.1.0")
        KeyValue(strings.repository, "github.com/manthatwalks/gemma4goodBeeCare")
    }
}

@Composable
private fun KeyValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DeleteConfirmation(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    strings: UiStrings,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.deleteModelTitle) },
        text = { Text(strings.deleteModelBody) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(strings.delete) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "—"
    val kib = 1024.0
    val mib = kib * 1024
    val gib = mib * 1024
    return when {
        bytes >= gib -> "%.2f GB".format(bytes / gib)
        bytes >= mib -> "%.1f MB".format(bytes / mib)
        bytes >= kib -> "%.0f KB".format(bytes / kib)
        else -> "$bytes B"
    }
}
