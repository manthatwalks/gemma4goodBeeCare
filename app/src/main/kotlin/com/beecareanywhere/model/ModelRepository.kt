package com.beecareanywhere.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

/**
 * Owns the on-device model file lifecycle: locating an installed model, downloading from a URL
 * with progress reporting, verifying integrity via SHA-256, and atomically replacing the cached
 * file.
 *
 * Single source of truth for `<modelsDir>/<filename>.litertlm`. The diagnose flow reads from the
 * path produced here; the download flow writes here.
 */
class ModelRepository(
    context: Context,
    private val httpClient: OkHttpClient,
) {

    val modelsDir: File = File(context.filesDir, "models").apply { mkdirs() }

    fun installedModel(filename: String): File? {
        val file = File(modelsDir, filename)
        return file.takeIf { it.exists() && it.length() > 0 }
    }

    fun delete(filename: String): Boolean {
        return File(modelsDir, filename).delete()
    }

    /**
     * Stream the download as a [DownloadState] Flow. Emits Downloading frequently (each buffer
     * chunk), then Verifying (if [expectedSha256] is provided), then Complete or Error.
     *
     * The download writes to `<filename>.partial` and renames atomically on success — a crashed or
     * cancelled download leaves only the `.partial` file behind.
     */
    fun download(
        url: String,
        filename: String,
        expectedSha256: String? = null,
    ): Flow<DownloadState> = flow {
        val outputFile = File(modelsDir, filename)
        val tempFile = File(modelsDir, "$filename.partial")
        tempFile.delete()

        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            emit(DownloadState.Error("HTTP ${response.code} ${response.message}"))
            return@flow
        }

        val body = response.body ?: run {
            response.close()
            emit(DownloadState.Error("Empty response body"))
            return@flow
        }

        val total = body.contentLength()
        var downloaded = 0L
        val buf = ByteArray(BUFFER_BYTES)

        try {
            body.byteStream().use { input ->
                tempFile.outputStream().use { output ->
                    while (true) {
                        val read = input.read(buf)
                        if (read == -1) break
                        output.write(buf, 0, read)
                        downloaded += read
                        emit(DownloadState.Downloading(downloaded, total))
                    }
                }
            }
        } catch (e: Throwable) {
            tempFile.delete()
            emit(DownloadState.Error(e.message ?: "Download failed"))
            return@flow
        }

        if (expectedSha256 != null) {
            emit(DownloadState.Verifying)
            val actualSha = sha256(tempFile)
            if (!actualSha.equals(expectedSha256, ignoreCase = true)) {
                tempFile.delete()
                emit(DownloadState.Error("SHA-256 mismatch: expected $expectedSha256, got $actualSha"))
                return@flow
            }
        }

        if (outputFile.exists()) outputFile.delete()
        if (!tempFile.renameTo(outputFile)) {
            tempFile.delete()
            emit(DownloadState.Error("Failed to move downloaded file into place"))
            return@flow
        }

        emit(DownloadState.Complete(outputFile))
    }.flowOn(Dispatchers.IO)

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(BUFFER_BYTES)
            while (true) {
                val read = input.read(buf)
                if (read == -1) break
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val BUFFER_BYTES = 64 * 1024
    }
}
