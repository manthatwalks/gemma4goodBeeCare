package com.beecareanywhere.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Top-level diagnostic container. Delegates to ComposeScreen or ChatScreen
 * based on the current stage in DiagnosticViewModel.
 *
 * The old single-screen layout has been replaced by the new BeeCare design
 * (3-step compose flow → chat response view). Settings / download access
 * is now via the HomeScreen app bar or the Settings route directly.
 */
@Composable
fun DiagnosticScreen(
    viewModel: DiagnosticViewModel,
    onOpenDownload: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit = {},
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    when (ui.stage) {
        DiagnosticStage.Compose -> ComposeScreen(
            viewModel = viewModel,
            onBack = onBack,
        )
        DiagnosticStage.Chat -> ChatScreen(
            viewModel = viewModel,
            onBack = viewModel::goToCompose,
        )
    }
}
