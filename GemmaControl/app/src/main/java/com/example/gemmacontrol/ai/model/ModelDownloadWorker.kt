package com.example.gemmacontrol.ai.model

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class ModelDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val request = try {
            ModelDownloadRequest(
                url = inputData.getString(ModelDownloadContract.KEY_MODEL_URL)
                    ?: return Result.failure(errorData("Missing model URL.")),
                fileName = inputData.getString(ModelDownloadContract.KEY_MODEL_FILE_NAME)
                    ?: return Result.failure(errorData("Missing model file name.")),
                sha256 = inputData.getString(ModelDownloadContract.KEY_MODEL_SHA256)
                    ?: return Result.failure(errorData("Missing model SHA-256."))
            )
        } catch (e: Exception) {
            return Result.failure(errorData(e.message ?: "Invalid model download request."))
        }

        val modelDirectory = File(applicationContext.filesDir, ModelDownloadFiles.MODEL_DIRECTORY).apply {
            mkdirs()
        }
        val targetFile = File(modelDirectory, request.fileName)
        val temporaryFile = File(modelDirectory, ModelDownloadFiles.temporaryFileName(request.fileName))

        if (targetFile.exists() && isShaValid(targetFile, request.sha256)) {
            return Result.success(successData(targetFile))
        }

        return try {
            download(request.url, temporaryFile)
            if (!isShaValid(temporaryFile, request.sha256)) {
                temporaryFile.delete()
                return Result.retry()
            }
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!temporaryFile.renameTo(targetFile)) {
                return Result.retry()
            }
            Result.success(successData(targetFile))
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun download(url: String, temporaryFile: File) {
        val existingBytes = temporaryFile.takeIf { it.exists() }?.length() ?: 0L
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                if (existingBytes > 0L) {
                    setRequestProperty("Range", "bytes=$existingBytes-")
                }
            }

            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                throw IOException("Model download failed with HTTP $responseCode.")
            }
            val append = existingBytes > 0L && responseCode == HttpURLConnection.HTTP_PARTIAL
            if (existingBytes > 0L && !append) {
                temporaryFile.delete()
            }
            val startingBytes = if (append) existingBytes else 0L
            val totalBytes = connection.contentLengthLong
                .takeIf { it > 0L }
                ?.plus(startingBytes)
                ?: 0L
            val startedAtMs = System.currentTimeMillis()
            var receivedBytes = startingBytes

            connection.inputStream.use { input ->
                FileOutputStream(temporaryFile, append).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) {
                            break
                        }
                        output.write(buffer, 0, read)
                        receivedBytes += read
                        setProgress(progressData(receivedBytes, totalBytes, startedAtMs))
                    }
                }
            }
        } finally {
            connection?.disconnect()
        }
    }

    private fun progressData(
        receivedBytes: Long,
        totalBytes: Long,
        startedAtMs: Long
    ): Data {
        val progress = ModelDownloadProgress.calculate(
            receivedBytes = receivedBytes,
            totalBytes = totalBytes,
            startedAtMs = startedAtMs,
            nowMs = System.currentTimeMillis()
        )
        return workDataOf(
            ModelDownloadContract.KEY_MODEL_TOTAL_BYTES to progress.totalBytes,
            ModelDownloadContract.KEY_MODEL_DOWNLOAD_RECEIVED_BYTES to progress.receivedBytes,
            ModelDownloadContract.KEY_MODEL_DOWNLOAD_RATE to progress.bytesPerSecond,
            ModelDownloadContract.KEY_MODEL_DOWNLOAD_REMAINING_MS to (progress.remainingMs ?: -1L)
        )
    }

    private fun isShaValid(file: File, expectedSha256: String): Boolean {
        return sha256(file).equals(expectedSha256, ignoreCase = true)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }

    private fun successData(targetFile: File): Data {
        return workDataOf(ModelDownloadContract.KEY_MODEL_PATH to targetFile.absolutePath)
    }

    private fun errorData(message: String): Data {
        return workDataOf(ModelDownloadContract.KEY_MODEL_ERROR to message)
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
    }
}
