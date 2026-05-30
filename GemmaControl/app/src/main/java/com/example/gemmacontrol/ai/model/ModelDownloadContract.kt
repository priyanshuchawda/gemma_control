package com.example.gemmacontrol.ai.model

import androidx.work.Data
import androidx.work.workDataOf
import java.net.URI

data class ModelDownloadRequest(
    val url: String,
    val fileName: String,
    val sha256: String
) {
    init {
        ModelDownloadFiles.requireSafeFileName(fileName)
        requireHttpsUrl(url)
        require(SHA256_PATTERN.matches(sha256)) {
            "Model SHA-256 must be 64 lowercase or uppercase hex characters."
        }
    }

    fun toInputData(): Data {
        return workDataOf(
            ModelDownloadContract.KEY_MODEL_URL to url,
            ModelDownloadContract.KEY_MODEL_FILE_NAME to fileName,
            ModelDownloadContract.KEY_MODEL_SHA256 to sha256
        )
    }

    companion object {
        private val SHA256_PATTERN = Regex("[A-Fa-f0-9]{64}")

        fun requireHttpsUrl(url: String): String {
            require(url.isNotBlank()) { "Model URL is required." }
            val uri = try {
                URI.create(url)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Model URL must be valid.", e)
            }
            require(uri.scheme.equals("https", ignoreCase = true)) {
                "Model downloads must use HTTPS."
            }
            require(!uri.host.isNullOrBlank()) { "Model URL must include a host." }
            return url
        }
    }
}

object ModelDownloadContract {
    const val UNIQUE_WORK_PREFIX = "model_download:"

    const val KEY_MODEL_URL = "model_url"
    const val KEY_MODEL_FILE_NAME = "model_file_name"
    const val KEY_MODEL_SHA256 = "model_sha256"
    const val KEY_MODEL_PATH = "model_path"
    const val KEY_MODEL_ERROR = "model_error"
    const val KEY_MODEL_TOTAL_BYTES = "model_total_bytes"
    const val KEY_MODEL_DOWNLOAD_RECEIVED_BYTES = "model_download_received_bytes"
    const val KEY_MODEL_DOWNLOAD_RATE = "model_download_rate"
    const val KEY_MODEL_DOWNLOAD_REMAINING_MS = "model_download_remaining_ms"
}
