package com.beecareanywhere.multimodal

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Returns a lambda that ensures a runtime permission is granted, then calls [onResult].
 *
 * - If already granted: calls onResult(true) immediately.
 * - Otherwise: launches the system permission dialog and calls onResult with the outcome.
 */
@Composable
fun rememberPermissionRequest(
    permission: String,
    onResult: (granted: Boolean) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        onResult(granted)
    }
    return {
        if (hasPermission(context, permission)) onResult(true) else launcher.launch(permission)
    }
}

fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
