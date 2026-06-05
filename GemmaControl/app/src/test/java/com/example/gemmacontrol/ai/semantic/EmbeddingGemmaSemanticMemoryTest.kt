package com.example.gemmacontrol.ai.semantic

import com.example.gemmacontrol.ai.tools.LocalWhatsAppMessage
import com.example.gemmacontrol.notifications.WhatsAppContentKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddingGemmaSemanticMemoryTest {

    private val formatter = EmbeddingGemmaPromptFormatter()

    @Test
    fun formatterUsesEmbeddingGemmaRetrievalPrompts() {
        val query = formatter.query("show me the payment message")
        val document = formatter.document(
            LocalWhatsAppMessage(
                id = "message-1",
                conversationName = "Office",
                senderName = "Asha",
                text = "Client meeting moved to 4 PM",
                postedAt = 1000L,
                priority = "HIGH"
            )
        )

        assertEquals("Retrieval-query", query.promptName)
        assertEquals("task: search result | query: show me the payment message", query.promptedText)
        assertEquals("Retrieval-document", document.promptName)
        assertTrue(document.promptedText.startsWith("title: Office | text:"))
        assertTrue(document.promptedText.contains("sender: Asha"))
        assertTrue(document.promptedText.contains("priority: high"))
        assertTrue(document.promptedText.contains("message: Client meeting moved to 4 PM"))
    }

    @Test
    fun formatterKeepsMediaPlaceholdersTruthful() {
        val document = formatter.document(
            LocalWhatsAppMessage(
                id = "message-photo",
                conversationName = "Family",
                senderName = "Mom",
                text = "Photo",
                postedAt = 1000L,
                priority = "NORMAL",
                contentKind = WhatsAppContentKind.PHOTO
            )
        )

        assertTrue(document.promptedText.contains("kind: photo"))
        assertTrue(document.promptedText.contains("Photo attachment (contents not inspected)"))
        assertFalse(document.promptedText.contains("message: Photo;"))
    }

    @Test
    fun prototypeRetrievesRelevantMessageWithoutRealModelDownload() = runTest {
        val prototype = SemanticMessageRetrievalPrototype(
            embeddingProvider = KeywordCategoryEmbeddingProvider(),
            nowProvider = { 42L }
        )

        val results = prototype.search(
            query = "show me that payment message",
            messages = sampleMessages(),
            limit = 2
        )

        assertEquals("payment-1", results.first().message.id)
        assertTrue(results.first().score > results[1].score)
    }

    @Test
    fun exactIndexSortsTiesByRecency() {
        val index = ExactMessageEmbeddingIndex(
            records = listOf(
                record(id = "older", postedAt = 1000L),
                record(id = "newer", postedAt = 2000L)
            )
        )

        val results = index.search(EmbeddingVector(listOf(1f, 0f)), limit = 2)

        assertEquals(listOf("newer", "older"), results.map { it.message.id })
    }

    @Test
    fun exactIndexIgnoresRecordsWithDifferentDimensions() {
        val index = ExactMessageEmbeddingIndex(
            records = listOf(
                record(id = "valid", vector = EmbeddingVector(listOf(1f, 0f))),
                record(id = "wrong-dimension", vector = EmbeddingVector(listOf(1f, 0f, 0f)))
            )
        )

        val results = index.search(EmbeddingVector(listOf(1f, 0f)), limit = 5)

        assertEquals(listOf("valid"), results.map { it.message.id })
    }

    private fun sampleMessages(): List<LocalWhatsAppMessage> {
        return listOf(
            LocalWhatsAppMessage(
                id = "family-1",
                conversationName = "Mom",
                senderName = "Mom",
                text = "Dinner is at 7 tonight",
                postedAt = 1000L,
                priority = "NORMAL"
            ),
            LocalWhatsAppMessage(
                id = "work-1",
                conversationName = "Office",
                senderName = "Asha",
                text = "Client meeting agenda is ready",
                postedAt = 2000L,
                priority = "NORMAL"
            ),
            LocalWhatsAppMessage(
                id = "payment-1",
                conversationName = "Bank Alerts",
                senderName = "Bank",
                text = "UPI payment received for invoice 4821",
                postedAt = 3000L,
                priority = "HIGH"
            )
        )
    }

    private fun record(
        id: String,
        postedAt: Long = 1000L,
        vector: EmbeddingVector = EmbeddingVector(listOf(1f, 0f))
    ): MessageEmbeddingRecord {
        return MessageEmbeddingRecord(
            message = LocalWhatsAppMessage(
                id = id,
                conversationName = "Chat",
                senderName = null,
                text = "Message",
                postedAt = postedAt,
                priority = "NORMAL"
            ),
            modelId = "fake-embedding-provider",
            vector = vector,
            createdAtEpochMillis = 1L
        )
    }
}

private class KeywordCategoryEmbeddingProvider : MessageEmbeddingProvider {
    override val modelId: String = "test-keyword-category-embedder"

    override suspend fun embed(texts: List<EmbeddingText>): List<EmbeddingVector> {
        return texts.map { text ->
            val normalized = text.promptedText.lowercase()
            EmbeddingVector(
                listOf(
                    if (normalized.containsAny("payment", "upi", "invoice", "bank")) 1f else 0f,
                    if (normalized.containsAny("meeting", "client", "office", "agenda")) 1f else 0f,
                    if (normalized.containsAny("mom", "dinner", "family")) 1f else 0f
                )
            )
        }
    }

    private fun String.containsAny(vararg needles: String): Boolean {
        return needles.any(::contains)
    }
}
