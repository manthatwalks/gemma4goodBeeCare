package com.beecareanywhere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beecareanywhere.di.ServiceLocator
import com.beecareanywhere.ui.DiagnosticScreen
import com.beecareanywhere.ui.DiagnosticViewModel
import com.beecareanywhere.ui.ModelDownloadScreen
import com.beecareanywhere.ui.ModelDownloadViewModel
import com.beecareanywhere.ui.theme.BeeCareAnywhereTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeeCareAnywhereTheme {
                BeeCareNavHost()
            }
        }
    }
}

private enum class Route { Diagnostic, Download }

@Composable
private fun BeeCareNavHost() {
    // Manual route state. Compose Navigation would be overkill for two screens; we'll graduate
    // to it when the screen count grows.
    var route by remember { mutableStateOf(Route.Diagnostic) }

    when (route) {
        Route.Diagnostic -> {
            val viewModel: DiagnosticViewModel = viewModel(
                factory = DiagnosticViewModel.Factory(ServiceLocator.provideModel()),
            )
            DiagnosticScreen(
                viewModel = viewModel,
                onOpenDownload = { route = Route.Download },
            )
        }
        Route.Download -> {
            val viewModel: ModelDownloadViewModel = viewModel(
                factory = ModelDownloadViewModel.Factory(
                    repository = ServiceLocator.provideModelRepository(),
                    settings = ServiceLocator.provideSettings(),
                ),
            )
            ModelDownloadScreen(
                viewModel = viewModel,
                onBack = { route = Route.Diagnostic },
            )
        }
    }
}
