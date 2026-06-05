package com.example.gemmacontrol.ai.semantic

import com.example.gemmacontrol.ai.tools.LocalWhatsAppMessage
import com.example.gemmacontrol.notifications.promptBodyText
import java.util.Locale
import kotlin.math.sqrt

data class EmbeddingVector(val values: List<Float>) {
    val dimension: Int = values.size

    init {
        require(values.isNotEmpty()) { "Embedding vector must not be empty." }
        require(values.all { it.isFinite() }) { "Embedding vector values must be finite." }
    }
}

data class EmbeddingText(
    val messageId: String?,
    val promptName: String,
    val promptedText: String
)

data class MessageEmbeddingRecord(
    val message: LocalWhatsAppMessage,
    val modelId: String,
    val vector: EmbeddingVector,
    val createdAtEpochMillis: Long
)

data class MessageSemanticSearchResult(
    val message: LocalWhatsAppMessage,
    val score: Double
)

interface MessageEmbeddingProvider {
    val modelId: String

    suspend fun embed(texts: List<EmbeddingText>): List<EmbeddingVector>
}

class EmbeddingGemmaPromptFormatter {
    fun query(query: String): EmbeddingText {
        val normalizedQuery = normalizePromptField(query)
        require(normalizedQuery.isNotBlank()) { "Semantic query must not be blank." }
        return EmbeddingText(
            messageId = null,
            promptName = QUERY_PROMPT_NAME,
            promptedText = "$QUERY_PREFIX$normalizedQuery"
        )
    }

    fun document(message: LocalWhatsAppMessage): EmbeddingText {
        val title = normalizePromptField(message.conversationName).ifBlank { "none" }
        val sender = message.senderName
            ?.let(::normalizePromptField)
            ?.takeIf { it.isNotBlank() }
            ?: "unknown"
        val body = normalizePromptField(message.contentKind.promptBodyText(message.text))
        val content = listOf(
            "chat: $title",
            "sender: $sender",
            "kind: ${message.contentKind.name.lowercase(Locale.ROOT)}",
            "priority: ${message.priority.lowercase(Locale.ROOT)}",
            "posted_at_ms: ${message.postedAt}",
            "message: $body"
        ).joinToString(separator = "; ")

        return EmbeddingText(
            messageId = message.id,
            promptName = DOCUMENT_PROMPT_NAME,
            promptedText = "$DOCUMENT_PREFIX$title | text: $content"
        )
    }

    private fun normalizePromptField(value: String): String {
        return value
            .replace(Regex("\\p{Cntrl}+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    companion object {
        const val QUERY_PROMPT_NAME = "Retrieval-query"
        const val DOCUMENT_PROMPT_NAME = "Retrieval-document"
        const val QUERY_PREFIX = "task: search result | query: "
        const val DOCUMENT_PREFIX = "title: "
    }
}

class ExactMessageEmbeddingIndex(
    private val records: List<MessageEmbeddingRecord>
) {
    fun search(
        queryVector: EmbeddingVector,
        limit: Int = DEFAULT_LIMIT,
        minimumScore: Double? = null
    ): List<MessageSemanticSearchResult> {
        val safeLimit = limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
        return records.asSequence()
            .filter { it.vector.dimension == queryVector.dimension }
            .map { record ->
                MessageSemanticSearchResult(
                    message = record.message,
                    score = cosineSimilarity(queryVector, record.vector)
                )
            }
            .filter { result -> minimumScore == null || result.score >= minimumScore }
            .sortedWith(
                compareByDescending<MessageSemanticSearchResult> { it.score }
                    .thenByDescending { it.message.postedAt }
                    .thenBy { it.message.id }
            )
            .take(safeLimit)
            .toList()
    }

    companion object {
        const val DEFAULT_LIMIT = 5
        const val MIN_LIMIT = 1
        const val MAX_LIMIT = 20

        fun cosineSimilarity(left: EmbeddingVector, right: EmbeddingVector): Double {
            require(left.dimension == right.dimension) {
                "Embedding dimensions must match. left=${left.dimension}, right=${right.dimension}"
            }
            var dot = 0.0
            var leftNorm = 0.0
            var rightNorm = 0.0
            for (index in 0 until left.dimension) {
                val leftValue = left.values[index].toDouble()
                val rightValue = right.values[index].toDouble()
                dot += leftValue * rightValue
                leftNorm += leftValue * leftValue
                rightNorm += rightValue * rightValue
            }
            if (leftNorm == 0.0 || rightNorm == 0.0) {
                return 0.0
            }
            return dot / (sqrt(leftNorm) * sqrt(rightNorm))
        }
    }
}

class SemanticMessageRetrievalPrototype(
    private val embeddingProvider: MessageEmbeddingProvider,
    private val promptFormatter: EmbeddingGemmaPromptFormatter = EmbeddingGemmaPromptFormatter(),
    private val nowProvider: () -> Long = System::currentTimeMillis
) {
    suspend fun index(messages: List<LocalWhatsAppMessage>): List<MessageEmbeddingRecord> {
        val embeddingTexts = messages.map(promptFormatter::document)
        val vectors = embeddingProvider.embed(embeddingTexts)
        require(vectors.size == messages.size) {
            "Embedding provider returned ${vectors.size} vectors for ${messages.size} messages."
        }
        return messages.zip(vectors).map { (message, vector) ->
            MessageEmbeddingRecord(
                message = message,
                modelId = embeddingProvider.modelId,
                vector = vector,
                createdAtEpochMillis = nowProvider()
            )
        }
    }

    suspend fun search(
        query: String,
        messages: List<LocalWhatsAppMessage>,
        limit: Int = ExactMessageEmbeddingIndex.DEFAULT_LIMIT,
        minimumScore: Double? = null
    ): List<MessageSemanticSearchResult> {
        if (messages.isEmpty()) {
            return emptyList()
        }
        val records = index(messages)
        val queryVector = embeddingProvider.embed(listOf(promptFormatter.query(query))).single()
        return ExactMessageEmbeddingIndex(records).search(
            queryVector = queryVector,
            limit = limit,
            minimumScore = minimumScore
        )
    }
}
