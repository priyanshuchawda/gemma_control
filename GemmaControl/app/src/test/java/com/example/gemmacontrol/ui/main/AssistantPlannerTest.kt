package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.tools.WhatsAppToolAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantPlannerTest {
    private val planner = AssistantPlanner()

    @Test
    fun plan_returnsSameReadActionForVoiceAndTypedInput() {
        val voicePlan = planner.plan(
            text = "read my latest stored messages",
            source = AssistantInputSource.Voice
        )
        val typedPlan = planner.plan(
            text = "read my latest stored messages",
            source = AssistantInputSource.Typed
        )

        assertEquals(AssistantPlan.ReadCommand(VoiceCommand.ReadStoredMessages), voicePlan)
        assertEquals(voicePlan, typedPlan)
    }

    @Test
    fun plan_routesLatestMessagesToActiveNotificationRead() {
        val plan = planner.plan("read my latest WhatsApp messages", AssistantInputSource.Voice)

        assertEquals(AssistantPlan.ReadCommand(VoiceCommand.ReadLatestMessages), plan)
    }

    @Test
    fun plan_routesNaturalSummaryRequestsLocallyWithoutModelProposal() {
        val voicePlan = planner.plan("summarize message", AssistantInputSource.Voice)
        val typedPlan = planner.plan("summary of WhatsApp messages", AssistantInputSource.Typed)
        val catchUpPlan = planner.plan("catch me up on WhatsApp", AssistantInputSource.Voice)
        val whatHappenedPlan = planner.plan("what happened in WhatsApp", AssistantInputSource.Typed)
        val fillerWordPlan = planner.plan("summarize the WhatsApp messages", AssistantInputSource.Voice)
        val britishSpellingPlan = planner.plan("summarise WhatsApp messages", AssistantInputSource.Voice)
        val asrSuffixPlan = planner.plan("summarize WhatsApp message is", AssistantInputSource.Voice)

        assertEquals(AssistantPlan.ReadCommand(VoiceCommand.SummarizeWhatsAppMessages), voicePlan)
        assertEquals(AssistantPlan.ReadCommand(VoiceCommand.SummarizeWhatsAppMessages), typedPlan)
        assertEquals(AssistantPlan.ReadCommand(VoiceCommand.SummarizeWhatsAppMessages), catchUpPlan)
        assertEquals(AssistantPlan.ReadCommand(VoiceCommand.SummarizeWhatsAppMessages), whatHappenedPlan)
        assertEquals(AssistantPlan.ReadCommand(VoiceCommand.SummarizeWhatsAppMessages), fillerWordPlan)
        assertEquals(AssistantPlan.ReadCommand(VoiceCommand.SummarizeWhatsAppMessages), britishSpellingPlan)
        assertEquals(AssistantPlan.ReadCommand(VoiceCommand.SummarizeWhatsAppMessages), asrSuffixPlan)
    }

    @Test
    fun plan_routesChatScopedSummaryRequestsLocallyWithoutModelProposal() {
        val plan = planner.plan("catch me up on messages from Mom", AssistantInputSource.Voice)

        assertEquals(
            AssistantPlan.ReadCommand(VoiceCommand.SummarizeMessagesFromConversation("Mom")),
            plan
        )
    }

    @Test
    fun plan_returnsSameReplyActionForVoiceAndTypedInput() {
        val input = "reply to the latest message: I am in a meeting"
        val voicePlan = planner.plan(input, AssistantInputSource.Voice)
        val typedPlan = planner.plan(input, AssistantInputSource.Typed)

        assertEquals(
            AssistantPlan.ReplyCommand(
                VoiceCommand.ReplyToLatestActiveMessage(
                    replyText = "I am in a meeting",
                    explicitLatest = true
                )
            ),
            voicePlan
        )
        assertEquals(voicePlan, typedPlan)
    }

    @Test
    fun plan_returnsSameNamedReplyActionForVoiceAndTypedInput() {
        val input = "reply to Mom: I am leaving now"
        val voicePlan = planner.plan(input, AssistantInputSource.Voice)
        val typedPlan = planner.plan(input, AssistantInputSource.Typed)

        assertEquals(
            AssistantPlan.NamedReplyCommand(
                VoiceCommand.ReplyToConversation("Mom", "I am leaving now")
            ),
            voicePlan
        )
        assertEquals(voicePlan, typedPlan)
    }

    @Test
    fun plan_returnsSameLocalToolActionForVoiceAndTypedInput() {
        val input = "search WhatsApp for payment"
        val expected = AssistantPlan.LocalToolCommand(
            VoiceCommand.LocalToolAction(
                WhatsAppToolAction.SearchMessages(
                    query = "payment",
                    conversationName = null
                )
            )
        )

        val voicePlan = planner.plan(input, AssistantInputSource.Voice)
        val typedPlan = planner.plan(input, AssistantInputSource.Typed)

        assertEquals(expected, voicePlan)
        assertEquals(voicePlan, typedPlan)
    }

    @Test
    fun plan_requestsModelProposalWithClarificationFallbackForUnsupportedInput() {
        val plan = planner.plan("can you find the dinner message from yesterday", AssistantInputSource.Typed)

        assertTrue(plan is AssistantPlan.RequestModelProposal)
        val request = plan as AssistantPlan.RequestModelProposal
        assertEquals("can you find the dinner message from yesterday", request.transcript)
        assertEquals(
            VoiceAssistantState.ClarificationRequired(
                "I can help with locally stored WhatsApp messages and active notification replies. Try: read my latest stored messages, search WhatsApp for payment, show pending follow ups, or reply to the latest message."
            ),
            request.fallbackState
        )
    }

    @Test
    fun plan_asksClarificationForIncompleteReplyWithoutCallingModel() {
        val plan = planner.plan("reply to the latest message:", AssistantInputSource.Voice)

        assertEquals(
            AssistantPlan.AskClarification("Reply text cannot be empty."),
            plan
        )
    }
}
