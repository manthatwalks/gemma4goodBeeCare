package com.beecareanywhere.multimodal

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * Compose helper for capturing a single still image via the system camera.
 *
 * Returns a no-arg lambda that, when invoked, launches the camera activity. The result file (a
 * JPEG in app cache) is delivered to [onResult]; null means the user cancelled or the capture
 * failed.
 *
 * LiteRT-LM consumes images via `Content.ImageFile(File)`, so the returned File can be handed
 * straight to [com.beecareanywhere.model.BeekeepingModel.diagnose]. Callers should delete the
 * file after the query completes (see [deleteCaptureFile]).
 */
@Composable
fun rememberImageCapture(onResult: (File?) -> Unit): () -> Unit {
    val context = LocalContext.current
    var pendingFile by remember { mutableStateOf<File?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        onResult(if (success) pendingFile else null)
        pendingFile = null
    }

    return {
        val file = createCaptureFile(context)
        pendingFile = file
        launcher.launch(toContentUri(context, file))
    }
}

private fun createCaptureFile(context: Context): File {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    return File(dir, "capture-${System.currentTimeMillis()}.jpg")
}

private fun toContentUri(context: Context, file: File) = FileProvider.getUriForFile(
    context,
    "${context.packageName}.fileprovider",
    file,
)

fun deleteCaptureFile(file: File) {
    runCatching { file.delete() }
}
