package com.example.gemmacontrol.ai.tools

import android.content.Context
import android.content.Intent
import android.net.Uri

sealed interface WhatsAppDraftLaunchResult {
    data object Launched : WhatsAppDraftLaunchResult
    data object NoHandler : WhatsAppDraftLaunchResult
    data class Failed(val reason: String) : WhatsAppDraftLaunchResult
}

interface WhatsAppDraftLauncher {
    fun openShareDraft(messageText: String): WhatsAppDraftLaunchResult

    fun openClickToChatDraft(
        phoneNumberE164: String,
        messageText: String
    ): WhatsAppDraftLaunchResult
}

object UnavailableWhatsAppDraftLauncher : WhatsAppDraftLauncher {
    override fun openShareDraft(messageText: String): WhatsAppDraftLaunchResult {
        return WhatsAppDraftLaunchResult.NoHandler
    }

    override fun openClickToChatDraft(
        phoneNumberE164: String,
        messageText: String
    ): WhatsAppDraftLaunchResult {
        return WhatsAppDraftLaunchResult.NoHandler
    }
}

class AndroidWhatsAppDraftLauncher(
    private val context: Context
) : WhatsAppDraftLauncher {

    override fun openShareDraft(messageText: String): WhatsAppDraftLaunchResult {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage(WHATSAPP_PACKAGE)
            putExtra(Intent.EXTRA_TEXT, messageText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return startIfResolvable(intent)
    }

    override fun openClickToChatDraft(
        phoneNumberE164: String,
        messageText: String
    ): WhatsAppDraftLaunchResult {
        val phoneDigits = phoneNumberE164.removePrefix("+")
        val uri = Uri.Builder()
            .scheme("https")
            .authority("wa.me")
            .appendPath(phoneDigits)
            .appendQueryParameter("text", messageText)
            .build()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(WHATSAPP_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return startIfResolvable(intent)
    }

    private fun startIfResolvable(intent: Intent): WhatsAppDraftLaunchResult {
        return try {
            if (intent.resolveActivity(context.packageManager) == null) {
                WhatsAppDraftLaunchResult.NoHandler
            } else {
                context.startActivity(intent)
                WhatsAppDraftLaunchResult.Launched
            }
        } catch (e: Exception) {
            WhatsAppDraftLaunchResult.Failed("Unable to open WhatsApp draft safely.")
        }
    }

    private companion object {
        const val WHATSAPP_PACKAGE = "com.whatsapp"
    }
}
