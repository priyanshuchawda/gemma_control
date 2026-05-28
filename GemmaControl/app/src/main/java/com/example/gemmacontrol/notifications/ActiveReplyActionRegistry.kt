package com.example.gemmacontrol.notifications

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveReplyHandle(
    val notificationKey: String,
    val packageName: String,
    val capturedAt: Long,
    val textRemoteInputs: Array<RemoteInput>,
    val actionIntent: PendingIntent
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ActiveReplyHandle
        if (notificationKey != other.notificationKey) return false
        return true
    }

    override fun hashCode(): Int {
        return notificationKey.hashCode()
    }
}

interface ActiveReplyActionRegistry {
    val availabilityFlow: StateFlow<Map<String, Boolean>>

    fun registerFromNotification(
        notificationKey: String,
        packageName: String,
        notification: Notification,
        capturedAt: Long
    )

    fun remove(notificationKey: String)

    fun getReplyHandle(notificationKey: String): ActiveReplyHandle?

    fun clear()
}

object InMemoryActiveReplyActionRegistry : ActiveReplyActionRegistry {

    private val handles = mutableMapOf<String, ActiveReplyHandle>()
    private val _availabilityFlow = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val availabilityFlow: StateFlow<Map<String, Boolean>> = _availabilityFlow.asStateFlow()

    @Synchronized
    override fun registerFromNotification(
        notificationKey: String,
        packageName: String,
        notification: Notification,
        capturedAt: Long
    ) {
        // Only accept packages already allowed
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") {
            return
        }

        val actions = notification.actions ?: return
        var replyAction: Notification.Action? = null

        // 1. Prefer an action whose semantic action is SEMANTIC_ACTION_REPLY when present.
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            val hasTextInput = remoteInputs.any { it.allowFreeFormInput || (it.choices != null && it.choices.isNotEmpty()) }
            if (hasTextInput) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    action.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY
                ) {
                    replyAction = action
                    break
                }
            }
        }

        // 2. If semantic action is absent, fall back to any action that has a text remote input
        if (replyAction == null) {
            for (action in actions) {
                val remoteInputs = action.remoteInputs ?: continue
                val hasTextInput = remoteInputs.any { it.allowFreeFormInput || (it.choices != null && it.choices.isNotEmpty()) }
                if (hasTextInput) {
                    replyAction = action
                    break
                }
            }
        }

        if (replyAction != null && replyAction.actionIntent != null && replyAction.remoteInputs != null) {
            val textInputs = replyAction.remoteInputs.filter {
                it.allowFreeFormInput || (it.choices != null && it.choices.isNotEmpty())
            }.toTypedArray()

            if (textInputs.isNotEmpty()) {
                val handle = ActiveReplyHandle(
                    notificationKey = notificationKey,
                    packageName = packageName,
                    capturedAt = capturedAt,
                    textRemoteInputs = textInputs,
                    actionIntent = replyAction.actionIntent
                )
                handles[notificationKey] = handle
                updateAvailabilityFlow()
            }
        }
    }

    @Synchronized
    override fun remove(notificationKey: String) {
        if (handles.remove(notificationKey) != null) {
            updateAvailabilityFlow()
        }
    }

    @Synchronized
    override fun getReplyHandle(notificationKey: String): ActiveReplyHandle? {
        return handles[notificationKey]
    }

    @Synchronized
    override fun clear() {
        handles.clear()
        updateAvailabilityFlow()
    }

    private fun updateAvailabilityFlow() {
        _availabilityFlow.value = handles.mapValues { true }
    }
}
