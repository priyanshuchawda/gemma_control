package com.example.gemmacontrol.notifications

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log

sealed interface ReplySendResult {
    data object Success : ReplySendResult
    data object EmptyText : ReplySendResult
    data object NoActiveReplyAction : ReplySendResult
    data object NotificationExpired : ReplySendResult
    data object CanceledBySystem : ReplySendResult
    data object FailedSafely : ReplySendResult
}

class ActiveNotificationReplyExecutor(
    private val registry: ActiveReplyActionRegistry = InMemoryActiveReplyActionRegistry
) {
    companion object {
        private const val TAG = "ReplyExecutor"
    }

    fun sendConfirmedReply(
        context: Context,
        notificationKey: String,
        text: String
    ): ReplySendResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return ReplySendResult.EmptyText
        }

        // Apply a reasonable UI limit such as 1000 characters.
        if (trimmed.length > 1000) {
            return ReplySendResult.FailedSafely
        }

        val handle = registry.getReplyHandle(notificationKey) ?: return ReplySendResult.NoActiveReplyAction

        val fillInIntent = Intent()
        val resultBundle = Bundle()

        // Map relevant RemoteInput result keys to the user text.
        for (input in handle.textRemoteInputs) {
            resultBundle.putCharSequence(input.resultKey, trimmed)
        }

        RemoteInput.addResultsToIntent(
            handle.textRemoteInputs,
            fillInIntent,
            resultBundle
        )

        // Mark free-form input source when supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            RemoteInput.setResultsSource(
                fillInIntent,
                RemoteInput.SOURCE_FREE_FORM_INPUT
            )
        }

        return try {
            Log.d(TAG, "Attempting to invoke reply action for notification key suffix: ${if (notificationKey.length > 8) notificationKey.takeLast(8) else notificationKey}")
            handle.actionIntent.send(context, 0, fillInIntent)
            Log.d(TAG, "Reply action PendingIntent sent successfully")
            ReplySendResult.Success
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG, "PendingIntent was canceled", e)
            ReplySendResult.CanceledBySystem
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send reply safely", e)
            ReplySendResult.FailedSafely
        }
    }
}
