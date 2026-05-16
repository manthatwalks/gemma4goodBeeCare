package com.beecareanywhere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beecareanywhere.di.ServiceLocator
import com.beecareanywhere.ui.DiagnosticScreen
import com.beecareanywhere.ui.DiagnosticViewModel
import com.beecareanywhere.ui.theme.BeeCareAnywhereTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeeCareAnywhereTheme {
                val viewModel: DiagnosticViewModel = viewModel(
                    factory = DiagnosticViewModel.Factory(ServiceLocator.provideModel()),
                )
                DiagnosticScreen(viewModel)
            }
        }
    }
}
