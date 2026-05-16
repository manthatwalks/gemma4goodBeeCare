package com.beecareanywhere.model

import java.io.File

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
    ) : DownloadState {
        val progressFraction: Float? = if (totalBytes > 0) {
            (bytesDownloaded.toDouble() / totalBytes).toFloat().coerceIn(0f, 1f)
        } else {
            null
        }
    }
    data object Verifying : DownloadState
    data class Complete(val file: File) : DownloadState
    data class Error(val message: String) : DownloadState
}
