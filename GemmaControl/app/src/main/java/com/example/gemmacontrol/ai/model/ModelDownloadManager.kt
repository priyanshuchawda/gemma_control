package com.example.gemmacontrol.ai.model

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.UUID

class ModelDownloadManager(
    private val workManagerProvider: (Context) -> WorkManager = { WorkManager.getInstance(it) }
) {
    fun enqueue(context: Context, request: ModelDownloadRequest): UUID {
        val workRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(request.toInputData())
            .setConstraints(downloadConstraints())
            .build()

        workManagerProvider(context).enqueueUniqueWork(
            workName(request.fileName),
            ExistingWorkPolicy.KEEP,
            workRequest
        )
        return workRequest.id
    }

    fun cancel(context: Context, fileName: String) {
        workManagerProvider(context).cancelUniqueWork(workName(fileName))
    }

    fun workName(fileName: String): String {
        return ModelDownloadContract.UNIQUE_WORK_PREFIX + ModelDownloadFiles.requireSafeFileName(fileName)
    }

    private fun downloadConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
