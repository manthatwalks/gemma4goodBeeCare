package com.beecareanywhere.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beecareanywhere.model.DownloadState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadScreen(
    viewModel: ModelDownloadViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val url by viewModel.url.collectAsStateWithLifecycle(initialValue = "")
    val filename by viewModel.filename.collectAsStateWithLifecycle(initialValue = "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model download") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DownloadInfoCard(filename = filename, url = url)
            DownloadStateView(state)
            ActionRow(
                state = state,
                onStart = viewModel::startDownload,
                onCancel = viewModel::cancelDownload,
                onClear = viewModel::reset,
            )
        }
    }
}

@Composable
private fun DownloadInfoCard(filename: String, url: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("File", style = MaterialTheme.typography.labelMedium)
            Text(filename.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
            Text("Source", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            Text(url.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DownloadStateView(state: DownloadState) {
    when (state) {
        DownloadState.Idle -> Text(
            "Ready to download.",
            style = MaterialTheme.typography.bodyMedium,
        )
        is DownloadState.Downloading -> DownloadingView(state)
        DownloadState.Verifying -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
            Text("Verifying SHA-256…", style = MaterialTheme.typography.bodyMedium)
        }
        is DownloadState.Complete -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                "Model installed at ${state.file.absolutePath}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        is DownloadState.Error -> Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
                state.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun DownloadingView(state: DownloadState.Downloading) {
    val pct = state.progressFraction
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (pct != null) {
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "${(pct * 100).roundToInt()}% • ${formatBytes(state.bytesDownloaded)} / ${formatBytes(state.totalBytes)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                "Downloaded ${formatBytes(state.bytesDownloaded)} (unknown total size)",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ActionRow(
    state: DownloadState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(top = 16.dp)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state) {
            DownloadState.Idle, is DownloadState.Error -> {
                Button(onClick = onStart) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Text("Start download", modifier = Modifier.padding(start = 8.dp))
                }
            }
            is DownloadState.Downloading, DownloadState.Verifying -> {
                Button(onClick = onCancel) {
                    Text("Cancel")
                }
            }
            is DownloadState.Complete -> {
                Button(onClick = onClear) {
                    Text("Done")
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "—"
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
