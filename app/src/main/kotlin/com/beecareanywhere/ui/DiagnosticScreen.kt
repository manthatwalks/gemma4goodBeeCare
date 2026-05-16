package com.beecareanywhere.ui

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beecareanywhere.model.ModelState
import com.beecareanywhere.multimodal.rememberImageCapture
import com.beecareanywhere.multimodal.rememberPermissionRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(viewModel: DiagnosticViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val modelState by viewModel.modelState.collectAsStateWithLifecycle()

    val launchCamera = rememberImageCapture(onResult = viewModel::onImageCaptured)
    val requestCameraPermission = rememberPermissionRequest(Manifest.permission.CAMERA) { granted ->
        if (granted) launchCamera()
    }
    val requestMicPermission = rememberPermissionRequest(Manifest.permission.RECORD_AUDIO) { granted ->
        if (granted) viewModel.startAudioRecording()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BeeCare Anywhere") },
                actions = {
                    IconButton(onClick = viewModel::resetConversation) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModelStateBanner(modelState)

            OutlinedTextField(
                value = ui.text,
                onValueChange = viewModel::updateText,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describe what you're seeing in the hive…") },
                minLines = 3,
                maxLines = 5,
            )

            AttachmentsRow(
                hasImage = ui.capturedImage != null,
                hasAudio = ui.capturedAudio != null,
                onClearImage = viewModel::clearImage,
                onClearAudio = viewModel::clearAudio,
            )

            CaptureControls(
                isRecording = ui.isRecordingAudio,
                onCameraClick = requestCameraPermission,
                onMicClick = {
                    if (ui.isRecordingAudio) viewModel.stopAudioRecording() else requestMicPermission()
                },
                onSubmit = viewModel::submit,
                isSubmitEnabled = modelState is ModelState.Ready &&
                    !ui.isGenerating &&
                    !ui.isRecordingAudio &&
                    (ui.text.isNotBlank() || ui.capturedImage != null || ui.capturedAudio != null),
                isGenerating = ui.isGenerating,
                onCancelGeneration = viewModel::cancelGeneration,
            )

            ResponseArea(
                text = ui.response,
                error = ui.error,
                isGenerating = ui.isGenerating,
            )
        }
    }
}

@Composable
private fun ModelStateBanner(state: ModelState) {
    val (label, color) = when (state) {
        ModelState.Idle -> "Model not loaded" to MaterialTheme.colorScheme.surfaceVariant
        ModelState.Loading -> "Loading model…" to MaterialTheme.colorScheme.tertiaryContainer
        ModelState.Ready -> "Model ready" to MaterialTheme.colorScheme.primaryContainer
        is ModelState.Error -> "Model error: ${state.message}" to MaterialTheme.colorScheme.errorContainer
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state is ModelState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(16.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AttachmentsRow(
    hasImage: Boolean,
    hasAudio: Boolean,
    onClearImage: () -> Unit,
    onClearAudio: () -> Unit,
) {
    if (!hasImage && !hasAudio) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (hasImage) {
            AssistChip(
                onClick = onClearImage,
                label = { Text("Image attached") },
                trailingIcon = { Icon(Icons.Default.Cancel, contentDescription = "Remove image") },
            )
        }
        if (hasAudio) {
            AssistChip(
                onClick = onClearAudio,
                label = { Text("Audio attached") },
                trailingIcon = { Icon(Icons.Default.Cancel, contentDescription = "Remove audio") },
            )
        }
    }
}

@Composable
private fun CaptureControls(
    isRecording: Boolean,
    onCameraClick: () -> Unit,
    onMicClick: () -> Unit,
    onSubmit: () -> Unit,
    isSubmitEnabled: Boolean,
    isGenerating: Boolean,
    onCancelGeneration: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(onClick = onCameraClick) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(Modifier.padding(4.dp))
            Text("Photo")
        }
        FilledTonalButton(
            onClick = onMicClick,
            colors = if (isRecording) {
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                )
            } else {
                ButtonDefaults.filledTonalButtonColors()
            },
        ) {
            Icon(
                if (isRecording) Icons.Default.Stop else Icons.Outlined.Mic,
                contentDescription = null,
            )
            Spacer(Modifier.padding(4.dp))
            Text(if (isRecording) "Stop" else "Record")
        }
        Spacer(Modifier.weight(1f))
        if (isGenerating) {
            IconButton(onClick = onCancelGeneration) {
                Icon(Icons.Default.Cancel, contentDescription = "Cancel")
            }
        }
        Button(
            onClick = onSubmit,
            enabled = isSubmitEnabled,
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(Modifier.padding(4.dp))
            Text("Diagnose")
        }
    }
}

@Composable
private fun ResponseArea(
    text: String,
    error: String?,
    isGenerating: Boolean,
) {
    if (text.isEmpty() && error == null && !isGenerating) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        text = text.ifEmpty { "…" },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
