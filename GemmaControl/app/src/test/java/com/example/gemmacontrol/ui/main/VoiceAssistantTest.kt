package com.example.gemmacontrol.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceAssistantTest {

    @Test
    fun testParserRecognisesReadCommands() {
        val cmd1 = VoiceCommandParser.parse("read my latest messages")
        assertEquals(VoiceCommand.ReadLatestMessages, cmd1)

        val cmd2 = VoiceCommandParser.parse("read current messages")
        assertEquals(VoiceCommand.ReadLatestMessages, cmd2)

        val cmd3 = VoiceCommandParser.parse("  Read My Notifications?  ")
        assertEquals(VoiceCommand.ReadLatestMessages, cmd3)

        val cmd4 = VoiceCommandParser.parse("read")
        assertEquals(VoiceCommand.ReadLatestMessages, cmd4)

        val cmd5 = VoiceCommandParser.parse("read messages")
        assertEquals(VoiceCommand.ReadLatestMessages, cmd5)

        val cmd6 = VoiceCommandParser.parse("show my latest WhatsApp messages")
        assertEquals(VoiceCommand.ReadLatestMessages, cmd6)

        val cmd7 = VoiceCommandParser.parse("show me latest WhatsApp message")
        assertEquals(VoiceCommand.ReadLatestMessages, cmd7)

        val cmd8 = VoiceCommandParser.parse("tell me my recent WhatsApp notifications")
        assertEquals(VoiceCommand.ReadLatestMessages, cmd8)
    }

    @Test
    fun testParserRecognisesAdaptiveReadCommands() {
        assertEquals(VoiceCommand.ContinueReadingMessages, VoiceCommandParser.parse("continue"))
        assertEquals(VoiceCommand.ContinueReadingMessages, VoiceCommandParser.parse("read more"))
        assertEquals(VoiceCommand.SummarizeWhatsAppMessages, VoiceCommandParser.parse("summarize WhatsApp"))
        assertEquals(VoiceCommand.ReadImportantMessages, VoiceCommandParser.parse("read only important WhatsApp messages"))

        val chatCommand = VoiceCommandParser.parse("read messages from Mom")
        assertTrue(chatCommand is VoiceCommand.ReadMessagesFromConversation)
        assertEquals("Mom", (chatCommand as VoiceCommand.ReadMessagesFromConversation).conversationName)
    }

    @Test
    fun testParserRecognisesReplyCommands() {
        val cmd1 = VoiceCommandParser.parse("reply to the latest message: I am in a meeting")
        assertTrue(cmd1 is VoiceCommand.ReplyToLatestActiveMessage)
        assertEquals("I am in a meeting", (cmd1 as VoiceCommand.ReplyToLatestActiveMessage).replyText)

        val cmd2 = VoiceCommandParser.parse("reply latest message I will call later")
        assertTrue(cmd2 is VoiceCommand.ReplyToLatestActiveMessage)
        assertEquals("I will call later", (cmd2 as VoiceCommand.ReplyToLatestActiveMessage).replyText)

        val cmd3 = VoiceCommandParser.parse("send reply to latest message: hello!")
        assertTrue(cmd3 is VoiceCommand.ReplyToLatestActiveMessage)
        assertEquals("hello!", (cmd3 as VoiceCommand.ReplyToLatestActiveMessage).replyText)

        val cmd4 = VoiceCommandParser.parse("send message that I am in a meeting")
        assertTrue(cmd4 is VoiceCommand.ReplyToLatestActiveMessage)
        assertEquals("I am in a meeting", (cmd4 as VoiceCommand.ReplyToLatestActiveMessage).replyText)

        val cmd5 = VoiceCommandParser.parse("reply I'm busy")
        assertTrue(cmd5 is VoiceCommand.ReplyToLatestActiveMessage)
        assertEquals("I'm busy", (cmd5 as VoiceCommand.ReplyToLatestActiveMessage).replyText)

        val cmd6 = VoiceCommandParser.parse("say I am in a meeting")
        assertTrue(cmd6 is VoiceCommand.ReplyToLatestActiveMessage)
        assertEquals("I am in a meeting", (cmd6 as VoiceCommand.ReplyToLatestActiveMessage).replyText)

        val cmd7 = VoiceCommandParser.parse("tell them that I'll be late")
        assertTrue(cmd7 is VoiceCommand.ReplyToLatestActiveMessage)
        assertEquals("I'll be late", (cmd7 as VoiceCommand.ReplyToLatestActiveMessage).replyText)
    }

    @Test
    fun testParserRejectsEmptyReplyContent() {
        val cmd = VoiceCommandParser.parse("reply to the latest message: ")
        assertTrue(cmd is VoiceCommand.Unsupported)
        assertEquals("Reply text cannot be empty.", (cmd as VoiceCommand.Unsupported).reason)
    }

    @Test
    fun testParserRejectsUnsupportedNewMessageToContact() {
        val cmd1 = VoiceCommandParser.parse("send a message to Mom: I am late")
        assertTrue(cmd1 is VoiceCommand.Unsupported)
        assertEquals("Starting a new WhatsApp conversation needs FunctionGemma and a verified E.164 phone number. I can reply to an active notification.", (cmd1 as VoiceCommand.Unsupported).reason)

        val cmd2 = VoiceCommandParser.parse("send message to Dad hello")
        assertTrue(cmd2 is VoiceCommand.Unsupported)
        assertEquals("Starting a new WhatsApp conversation needs FunctionGemma and a verified E.164 phone number. I can reply to an active notification.", (cmd2 as VoiceCommand.Unsupported).reason)
    }

    @Test
    fun testParserRejectsVoiceNoteOrAudioMessage() {
        val cmd1 = VoiceCommandParser.parse("send a voice message: hi")
        assertTrue(cmd1 is VoiceCommand.Unsupported)
        assertEquals("Sending WhatsApp audio voice notes is not supported yet. You can dictate a text reply instead.", (cmd1 as VoiceCommand.Unsupported).reason)

        val cmd2 = VoiceCommandParser.parse("send an audio message")
        assertTrue(cmd2 is VoiceCommand.Unsupported)
        assertEquals("Sending WhatsApp audio voice notes is not supported yet. You can dictate a text reply instead.", (cmd2 as VoiceCommand.Unsupported).reason)

        val cmd3 = VoiceCommandParser.parse("send a voice note hello")
        assertTrue(cmd3 is VoiceCommand.Unsupported)
        assertEquals("Sending WhatsApp audio voice notes is not supported yet. You can dictate a text reply instead.", (cmd3 as VoiceCommand.Unsupported).reason)
    }

    @Test
    fun testParserReturnsDefaultUnsupportedForArbitraryText() {
        val cmd = VoiceCommandParser.parse("hello can you turn on the flashlight")
        assertTrue(cmd is VoiceCommand.Unsupported)
        assertEquals("I can currently read captured messages or reply to the latest active WhatsApp notification.", (cmd as VoiceCommand.Unsupported).reason)
    }

    @Test
    fun testRmsDbToAmplitudeIsClampedAndScaled() {
        assertEquals(0, convertRmsDbToAmplitude(-20f))
        assertEquals(0, convertRmsDbToAmplitude(-2f))
        assertEquals(65535, convertRmsDbToAmplitude(100f))
        assertEquals(65535, convertRmsDbToAmplitude(120f))
        assertTrue(convertRmsDbToAmplitude(49f) in 32000..33000)
    }
}
