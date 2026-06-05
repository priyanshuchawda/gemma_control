package com.example.gemmacontrol.ai.tools

enum class AssistantCapabilitySource {
    NotificationListener,
    Microphone,
    PostNotifications,
    LocalEncryptedStorage,
    LocalReminderScheduler,
    CapturePreferences,
    WhatsAppIntent,
    ActiveNotificationRemoteInput,
    PackageVisibility,
    ContactsPermission,
    CalendarIntent,
    MediaPicker,
    AccessibilityService,
    AdbDevelopmentOnly
}

data class AssistantCapability(
    val source: AssistantCapabilitySource,
    val permission: String,
    val works: String,
    val doesNotWork: String,
    val safetyLevel: String,
    val userSetupNeeded: String,
    val testMethod: String,
    val developmentOnly: Boolean = false
)

data class ToolCapabilityRequirement(
    val source: AssistantCapabilitySource,
    val setupResponse: String
)

data class AssistantCapabilityState(
    val notificationListenerEnabled: Boolean,
    val microphoneGranted: Boolean,
    val postNotificationsGranted: Boolean,
    val localEncryptedStorageReady: Boolean,
    val localReminderSchedulerReady: Boolean,
    val capturePreferencesReady: Boolean,
    val whatsappInstalled: Boolean,
    val activeNotificationReplyAvailable: Boolean,
    val packageVisibilityReady: Boolean,
    val contactsPermissionGranted: Boolean,
    val calendarIntentAvailable: Boolean,
    val mediaPickerAvailable: Boolean,
    val accessibilityServiceEnabled: Boolean,
    val adbAvailableForDevelopment: Boolean
) {
    companion object {
        fun assumeReady(): AssistantCapabilityState {
            return AssistantCapabilityState(
                notificationListenerEnabled = true,
                microphoneGranted = true,
                postNotificationsGranted = true,
                localEncryptedStorageReady = true,
                localReminderSchedulerReady = true,
                capturePreferencesReady = true,
                whatsappInstalled = true,
                activeNotificationReplyAvailable = true,
                packageVisibilityReady = true,
                contactsPermissionGranted = true,
                calendarIntentAvailable = true,
                mediaPickerAvailable = true,
                accessibilityServiceEnabled = false,
                adbAvailableForDevelopment = false
            )
        }
    }
}

class AssistantCapabilityMatrix private constructor(
    private val capabilities: Map<AssistantCapabilitySource, AssistantCapability>,
    private val toolRequirements: Map<WhatsAppToolName, List<ToolCapabilityRequirement>>
) {
    fun capability(source: AssistantCapabilitySource): AssistantCapability {
        return capabilities[source] ?: error("Unsupported capability source: $source")
    }

    fun requirementsForTool(toolName: WhatsAppToolName): List<ToolCapabilityRequirement> {
        return toolRequirements[toolName].orEmpty()
    }

    fun missingRequirements(
        toolName: WhatsAppToolName,
        state: AssistantCapabilityState
    ): List<ToolCapabilityRequirement> {
        return requirementsForTool(toolName).filterNot { it.isSatisfiedBy(state) }
    }

    fun firstSetupResponse(
        toolName: WhatsAppToolName,
        state: AssistantCapabilityState
    ): String? {
        return missingRequirements(toolName, state).firstOrNull()?.setupResponse
    }

    private fun ToolCapabilityRequirement.isSatisfiedBy(state: AssistantCapabilityState): Boolean {
        return when (source) {
            AssistantCapabilitySource.NotificationListener -> state.notificationListenerEnabled
            AssistantCapabilitySource.Microphone -> state.microphoneGranted
            AssistantCapabilitySource.PostNotifications -> state.postNotificationsGranted
            AssistantCapabilitySource.LocalEncryptedStorage -> state.localEncryptedStorageReady
            AssistantCapabilitySource.LocalReminderScheduler -> state.localReminderSchedulerReady
            AssistantCapabilitySource.CapturePreferences -> state.capturePreferencesReady
            AssistantCapabilitySource.WhatsAppIntent -> state.whatsappInstalled
            AssistantCapabilitySource.ActiveNotificationRemoteInput ->
                state.notificationListenerEnabled && state.activeNotificationReplyAvailable
            AssistantCapabilitySource.PackageVisibility -> state.packageVisibilityReady
            AssistantCapabilitySource.ContactsPermission -> state.contactsPermissionGranted
            AssistantCapabilitySource.CalendarIntent -> state.calendarIntentAvailable
            AssistantCapabilitySource.MediaPicker -> state.mediaPickerAvailable
            AssistantCapabilitySource.AccessibilityService -> state.accessibilityServiceEnabled
            AssistantCapabilitySource.AdbDevelopmentOnly -> false
        }
    }

    companion object {
        fun default(): AssistantCapabilityMatrix {
            val capabilities = AssistantCapabilitySource.entries.associateWith(::capabilityFor)
            return AssistantCapabilityMatrix(
                capabilities = capabilities,
                toolRequirements = mapOf(
                    WhatsAppToolName.ListRecentWhatsAppMessages to listOf(localStorage(), notificationListener()),
                    WhatsAppToolName.SearchWhatsAppMessages to listOf(localStorage(), notificationListener()),
                    WhatsAppToolName.GetWhatsAppMessageDetails to listOf(localStorage()),
                    WhatsAppToolName.GetActionableInbox to listOf(localStorage()),
                    WhatsAppToolName.CreateFollowUpFromMessage to listOf(localStorage()),
                    WhatsAppToolName.ListPendingFollowUps to listOf(localStorage()),
                    WhatsAppToolName.MarkFollowUpCompleted to listOf(localStorage()),
                    WhatsAppToolName.ScheduleReminderForMessage to listOf(
                        localStorage(),
                        localReminderScheduler(),
                        postNotifications()
                    ),
                    WhatsAppToolName.MarkMessagePriority to listOf(localStorage()),
                    WhatsAppToolName.DraftWhatsAppReply to listOf(localStorage()),
                    WhatsAppToolName.OpenWhatsAppShareDraft to listOf(packageVisibility(), whatsAppIntent()),
                    WhatsAppToolName.OpenWhatsAppClickToChat to listOf(packageVisibility(), whatsAppIntent()),
                    WhatsAppToolName.SendReplyToActiveWhatsAppNotification to listOf(
                        notificationListener(),
                        activeNotificationRemoteInput()
                    ),
                    WhatsAppToolName.PauseWhatsAppCapture to listOf(capturePreferences(), notificationListener()),
                    WhatsAppToolName.ResumeWhatsAppCapture to listOf(capturePreferences(), notificationListener()),
                    WhatsAppToolName.DeleteLocalWhatsAppData to listOf(localStorage())
                )
            )
        }

        private fun capabilityFor(source: AssistantCapabilitySource): AssistantCapability {
            return when (source) {
                AssistantCapabilitySource.NotificationListener -> AssistantCapability(
                    source = source,
                    permission = "Notification listener access",
                    works = "Read active WhatsApp notifications, capture future notifications, and discover RemoteInput reply actions exposed by WhatsApp.",
                    doesNotWork = "Cannot read WhatsApp history, deleted messages, private WhatsApp databases, or messages that never appeared as notifications.",
                    safetyLevel = "Read-only capture plus strict confirmation for replies.",
                    userSetupNeeded = "User must enable GemmaControl in Android Notification Access settings.",
                    testMethod = "Settings screen status, secure setting inspection, privacy-safe notification listener tests."
                )
                AssistantCapabilitySource.Microphone -> AssistantCapability(
                    source = source,
                    permission = "android.permission.RECORD_AUDIO",
                    works = "Capture the user's spoken command for local parsing and FunctionGemma routing.",
                    doesNotWork = "Does not grant access to phone calls or other app audio.",
                    safetyLevel = "User-initiated input only.",
                    userSetupNeeded = "User grants Microphone permission.",
                    testMethod = "Voice UI permission state and local speech recognition tests."
                )
                AssistantCapabilitySource.PostNotifications -> AssistantCapability(
                    source = source,
                    permission = "android.permission.POST_NOTIFICATIONS",
                    works = "Post local reminder/status notifications created by GemmaControl.",
                    doesNotWork = "Does not read WhatsApp notifications and does not bypass WhatsApp sends.",
                    safetyLevel = "Local notification output only.",
                    userSetupNeeded = "User grants Reminder Notifications on Android 13+.",
                    testMethod = "Runtime permission state and reminder worker tests."
                )
                AssistantCapabilitySource.LocalEncryptedStorage -> AssistantCapability(
                    source = source,
                    permission = "App-private Room database and Android Keystore",
                    works = "Store and query encrypted captured message metadata, follow-ups, priorities, and reminders.",
                    doesNotWork = "Does not access WhatsApp's private database or unencrypted chat history.",
                    safetyLevel = "Local encrypted data boundary.",
                    userSetupNeeded = "User enables storage consent for persisted inbox data.",
                    testMethod = "Room, Keystore, migration, and raw-row plaintext leak tests."
                )
                AssistantCapabilitySource.LocalReminderScheduler -> AssistantCapability(
                    source = source,
                    permission = "WorkManager inside the app sandbox",
                    works = "Schedule local reminder workers linked to stored messages.",
                    doesNotWork = "Does not create system calendar events unless a future calendar flow is added.",
                    safetyLevel = "Local scheduled work.",
                    userSetupNeeded = "No separate setup beyond app install; notification posting controls visible alerts.",
                    testMethod = "WorkManager enqueue and reminder repository tests."
                )
                AssistantCapabilitySource.CapturePreferences -> AssistantCapability(
                    source = source,
                    permission = "App DataStore preferences",
                    works = "Pause or resume GemmaControl's own WhatsApp capture behavior.",
                    doesNotWork = "Does not change WhatsApp settings or Android notification policy.",
                    safetyLevel = "Local app setting.",
                    userSetupNeeded = "No system permission; confirmation required for model-proposed changes.",
                    testMethod = "Preferences repository and tool executor tests."
                )
                AssistantCapabilitySource.WhatsAppIntent -> AssistantCapability(
                    source = source,
                    permission = "WhatsApp package and supported Android intents",
                    works = "Open WhatsApp share or click-to-chat draft flows after user confirmation.",
                    doesNotWork = "Does not silently send, scrape chats, or guarantee WhatsApp accepts every URI.",
                    safetyLevel = "External app launch after confirmation.",
                    userSetupNeeded = "WhatsApp must be installed and resolvable.",
                    testMethod = "Package manager resolution and manual intent validation."
                )
                AssistantCapabilitySource.ActiveNotificationRemoteInput -> AssistantCapability(
                    source = source,
                    permission = "Live notification RemoteInput exposed by WhatsApp",
                    works = "Reply through an active WhatsApp notification after strict manual confirmation.",
                    doesNotWork = "Cannot reply if the notification expired, was cleared, or lacks RemoteInput.",
                    safetyLevel = "Strict manual confirmation.",
                    userSetupNeeded = "Notification must still be active and expose reply input.",
                    testMethod = "Physical active-notification reply and expiry tests."
                )
                AssistantCapabilitySource.PackageVisibility -> AssistantCapability(
                    source = source,
                    permission = "Android package visibility and intent resolution",
                    works = "Resolve installed apps and supported deep links exposed to GemmaControl.",
                    doesNotWork = "Does not grant hidden package access beyond manifest/query visibility.",
                    safetyLevel = "Intent resolution only.",
                    userSetupNeeded = "Target app must be installed and visible to package manager.",
                    testMethod = "PackageManager resolveActivity tests and physical launch validation."
                )
                AssistantCapabilitySource.ContactsPermission -> AssistantCapability(
                    source = source,
                    permission = "android.permission.READ_CONTACTS (future)",
                    works = "Resolve names such as Mom or Dad only if future contact lookup is approved.",
                    doesNotWork = "Not used by current V1 tools.",
                    safetyLevel = "Future sensitive read permission.",
                    userSetupNeeded = "Not requested in current V1.",
                    testMethod = "Future permission and resolver tests."
                )
                AssistantCapabilitySource.CalendarIntent -> AssistantCapability(
                    source = source,
                    permission = "Calendar app intents or calendar permissions (future)",
                    works = "Future user-confirmed event creation if added.",
                    doesNotWork = "Current reminders are local app reminders, not calendar events.",
                    safetyLevel = "Future confirmation-required external write.",
                    userSetupNeeded = "Not requested in current V1.",
                    testMethod = "Future calendar intent resolution tests."
                )
                AssistantCapabilitySource.MediaPicker -> AssistantCapability(
                    source = source,
                    permission = "Android photo picker or scoped media permissions (future)",
                    works = "Analyze user-selected media only if future media understanding is added.",
                    doesNotWork = "Does not grant background gallery access.",
                    safetyLevel = "Future user-selected media only.",
                    userSetupNeeded = "Not requested in current V1.",
                    testMethod = "Future picker contract tests."
                )
                AssistantCapabilitySource.AccessibilityService -> AssistantCapability(
                    source = source,
                    permission = "Accessibility service (V2 evaluation only)",
                    works = "Could read visible screen text or perform narrow UI actions only if explicitly approved later.",
                    doesNotWork = "Does not read WhatsApp databases, recover hidden content, or allow silent sends/destructive actions.",
                    safetyLevel = "High-risk assistive mode; no V1 production tools.",
                    userSetupNeeded = "Not requested in current V1; future setup requires explicit disclosure and manual enablement.",
                    testMethod = "Accessibility decision doc, future policy review, and physical Xiaomi persistence tests."
                )
                AssistantCapabilitySource.AdbDevelopmentOnly -> AssistantCapability(
                    source = source,
                    permission = "ADB development bridge",
                    works = "Install, inspect, and test during development.",
                    doesNotWork = "Never production app functionality for real users.",
                    safetyLevel = "Development-only, excluded from tools.",
                    userSetupNeeded = "Developer-only USB debugging.",
                    testMethod = "Development terminal commands only.",
                    developmentOnly = true
                )
            }
        }

        private fun notificationListener() = ToolCapabilityRequirement(
            source = AssistantCapabilitySource.NotificationListener,
            setupResponse = "Enable Notification Listener Access so GemmaControl can read WhatsApp notifications and active reply actions."
        )

        private fun postNotifications() = ToolCapabilityRequirement(
            source = AssistantCapabilitySource.PostNotifications,
            setupResponse = "Grant Reminder Notifications so GemmaControl can show the scheduled reminder alert."
        )

        private fun localStorage() = ToolCapabilityRequirement(
            source = AssistantCapabilitySource.LocalEncryptedStorage,
            setupResponse = "Enable encrypted local inbox storage before using stored WhatsApp message tools."
        )

        private fun localReminderScheduler() = ToolCapabilityRequirement(
            source = AssistantCapabilitySource.LocalReminderScheduler,
            setupResponse = "Local reminder scheduling is unavailable on this device state."
        )

        private fun capturePreferences() = ToolCapabilityRequirement(
            source = AssistantCapabilitySource.CapturePreferences,
            setupResponse = "GemmaControl capture preferences are unavailable right now."
        )

        private fun packageVisibility() = ToolCapabilityRequirement(
            source = AssistantCapabilitySource.PackageVisibility,
            setupResponse = "WhatsApp cannot be resolved from this app state. Install WhatsApp or check package visibility."
        )

        private fun whatsAppIntent() = ToolCapabilityRequirement(
            source = AssistantCapabilitySource.WhatsAppIntent,
            setupResponse = "Install WhatsApp before opening a WhatsApp draft."
        )

        private fun activeNotificationRemoteInput() = ToolCapabilityRequirement(
            source = AssistantCapabilitySource.ActiveNotificationRemoteInput,
            setupResponse = "The active WhatsApp notification no longer exposes a reply action. Open a WhatsApp draft instead."
        )
    }
}
