package com.beecareanywhere.model

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
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
        val file = modelFile(filename)
        return file.takeIf { it.exists() && it.length() > 0 }
    }

    fun delete(filename: String): Boolean {
        val safeName = safeModelFilename(filename)
        File(modelsDir, "$safeName.partial").delete()
        return File(modelsDir, safeName).delete()
    }

    /**
     * Stream the download as a [DownloadState] Flow. Emits Downloading frequently (each buffer
     * chunk), then Verifying (if [expectedSha256] is provided), then Complete or Error.
     *
     * The download writes to `<filename>.partial` and renames atomically on success. Failed and
     * cancelled downloads remove the partial file so later attempts start cleanly.
     */
    fun download(
        url: String,
        filename: String,
        expectedSha256: String? = null,
    ): Flow<DownloadState> = flow {
        val outputFile = modelFile(filename)
        val tempFile = File(modelsDir, "${outputFile.name}.partial")
        val normalizedSha = expectedSha256?.trim()?.takeIf { it.isNotEmpty() }
        if (normalizedSha != null && !SHA_256_REGEX.matches(normalizedSha)) {
            emit(DownloadState.Error("Pinned SHA-256 must be 64 hex characters"))
            return@flow
        }
        tempFile.delete()

        val request = buildRequest(url)
        val call = httpClient.newCall(request)
        val coroutineContext = currentCoroutineContext()
        val cancelHook = coroutineContext[Job]?.invokeOnCompletion { cause ->
            if (cause is CancellationException) call.cancel()
        }

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadState.Error("HTTP ${response.code} ${response.message}"))
                    return@flow
                }

                val body = response.body ?: run {
                    emit(DownloadState.Error("Empty response body"))
                    return@flow
                }

                val total = body.contentLength()
                var downloaded = 0L
                val buf = ByteArray(BUFFER_BYTES)

                body.byteStream().use { input ->
                    tempFile.outputStream().use { output ->
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buf)
                            if (read == -1) break
                            output.write(buf, 0, read)
                            downloaded += read
                            emit(DownloadState.Downloading(downloaded, total))
                        }
                    }
                }

                if (total >= 0 && downloaded != total) {
                    tempFile.delete()
                    emit(DownloadState.Error("Download incomplete: expected $total bytes, got $downloaded"))
                    return@flow
                }
            }
        } catch (e: CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: IllegalArgumentException) {
            tempFile.delete()
            emit(DownloadState.Error(e.message ?: "Invalid download request"))
            return@flow
        } catch (e: IOException) {
            tempFile.delete()
            emit(DownloadState.Error(e.message ?: "Download failed"))
            return@flow
        } finally {
            cancelHook?.dispose()
        }

        if (normalizedSha != null) {
            emit(DownloadState.Verifying)
            val actualSha = sha256(tempFile)
            if (!actualSha.equals(normalizedSha, ignoreCase = true)) {
                tempFile.delete()
                emit(DownloadState.Error("SHA-256 mismatch: expected $normalizedSha, got $actualSha"))
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

    private fun buildRequest(url: String): Request {
        val httpUrl = url.trim().toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Model URL is invalid")
        require(httpUrl.isHttps) { "Model downloads must use HTTPS" }
        return Request.Builder().url(httpUrl).build()
    }

    private fun modelFile(filename: String): File = File(modelsDir, safeModelFilename(filename))

    private fun safeModelFilename(filename: String): String {
        val safeName = filename.trim()
        require(safeName == File(safeName).name) { "Model filename must not contain path separators" }
        require(!safeName.contains('\\')) { "Model filename must not contain path separators" }
        require(safeName != "." && safeName != "..") { "Model filename is invalid" }
        require(safeName.endsWith(".litertlm", ignoreCase = true)) {
            "Model filename must end with .litertlm"
        }
        return safeName
    }

    companion object {
        private const val BUFFER_BYTES = 64 * 1024
        private val SHA_256_REGEX = Regex("^[A-Fa-f0-9]{64}$")
    }
}
