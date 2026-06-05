package com.example.gemmacontrol.ui.main

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

        assertEquals(AssistantPlan.ReadCommand(VoiceCommand.ReadLatestMessages), voicePlan)
        assertEquals(voicePlan, typedPlan)
    }

    @Test
    fun plan_returnsSameReplyActionForVoiceAndTypedInput() {
        val input = "reply to the latest message: I am in a meeting"
        val voicePlan = planner.plan(input, AssistantInputSource.Voice)
        val typedPlan = planner.plan(input, AssistantInputSource.Typed)

        assertEquals(
            AssistantPlan.ReplyCommand(VoiceCommand.ReplyToLatestActiveMessage("I am in a meeting")),
            voicePlan
        )
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
                "I can help with locally stored WhatsApp messages and active notification replies. Try: read my latest stored messages, summarize WhatsApp, read messages from a chat, or reply to the latest message."
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
