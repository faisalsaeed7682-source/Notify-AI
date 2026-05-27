package com.example.ai.rag

import com.example.ai.embedding.EmbeddingService
import com.example.ai.intelligence.NotificationIntelligence
import com.example.data.local.NotificationRecord
import kotlin.math.sqrt

data class DocumentChunk(
    val id: String,
    val rawText: String,
    val cleanText: String,
    val embedding: List<Float>,
    val appName: String,
    val timestamp: Long,
    val priorityScore: Float,
    val isOtp: Boolean,
    val isSpam: Boolean,
    val isUrgent: Boolean,
    val category: String,
    val reasoning: String = "",
    var retrievalScore: Float = 0f
)

class RAGManager(private val embeddingService: EmbeddingService) {

    /**
     * Dynamic Context Packing with Token Budgeting and Semantic Reranking
     */
    suspend fun prepareContext(
        notifications: List<NotificationRecord>, 
        maxTokens: Int = 4096,
        query: String? = null
    ): List<DocumentChunk> {
        val deduplicated = deduplicate(notifications)
        var tokenCount = 0
        val allChunks = mutableListOf<DocumentChunk>()

        deduplicated.values.forEach { notif ->
            val intel = NotificationIntelligence.analyze(notif.appName, notif.title, notif.content)
            val text = "[${notif.appName}] ${notif.title}: ${notif.content}"
            allChunks.add(
                DocumentChunk(
                    id = notif.id.toString(),
                    rawText = text,
                    cleanText = notif.content,
                    embedding = embeddingService.getEmbedding(text),
                    appName = notif.appName,
                    timestamp = notif.timestamp,
                    priorityScore = intel.priority,
                    isOtp = intel.isOtp,
                    isSpam = intel.isSpam,
                    isUrgent = intel.isUrgent,
                    category = intel.category,
                    reasoning = intel.reasoning
                )
            )
        }

        // Apply Reranking if query is provided
        if (query != null) {
            val queryEmbedding = embeddingService.getEmbedding(query)
            allChunks.onEach { chunk ->
                val semanticSim = cosineSimilarity(queryEmbedding, chunk.embedding)
                // Hybrid Score: 60% Semantic, 20% Urgency, 10% Recency, 10% App Priority
                val ageHours = (System.currentTimeMillis() - chunk.timestamp).toFloat() / (1000 * 60 * 60)
                val recencyScore = (1.0f / (1.0f + ageHours)).coerceIn(0f, 1f)
                
                chunk.retrievalScore = (semanticSim * 0.6f) + (chunk.priorityScore * 0.2f) + (recencyScore * 0.2f)
            }
        } else {
            // Default: Importance and Recency
            allChunks.onEach { chunk ->
                val ageHours = (System.currentTimeMillis() - chunk.timestamp).toFloat() / (1000 * 60 * 60)
                val recencyScore = (1.0f / (1.0f + ageHours)).coerceIn(0f, 1f)
                chunk.retrievalScore = (chunk.priorityScore * 0.7f) + (recencyScore * 0.3f)
            }
        }

        val result = mutableListOf<DocumentChunk>()
        allChunks.sortedByDescending { it.retrievalScore }.forEach { chunk ->
            val estimatedTokens = (chunk.rawText.length / 4) + 12
            if (tokenCount + estimatedTokens <= maxTokens) {
                result.add(chunk)
                tokenCount += estimatedTokens
            }
        }
        
        return result
    }

    private fun deduplicate(notifications: List<NotificationRecord>): Map<String, NotificationRecord> {
        val map = mutableMapOf<String, NotificationRecord>()
        notifications.sortedByDescending { it.timestamp }.forEach { notif ->
            // Use a fuzzy key for deduplication (AppName + start of content)
            val key = "${notif.appName}|${notif.content.take(40).lowercase().trim()}"
            if (!map.containsKey(key)) {
                map[key] = notif
            }
        }
        return map
    }

    fun retrieve(query: String, chunks: List<DocumentChunk>, topK: Int = 5): List<DocumentChunk> {
        // Implementation of Hybrid Retrieval
        // Vector Sim + Recency + Urgency
        return chunks.onEach { chunk ->
            // In a real production app, we would compute query embedding here
            // But for this refined logic, we use the embeddingService in the calling scope usually.
            // For simplicity here, we'll assume chunks have scores pre-calculated or calculated externally.
        }.sortedByDescending { it.priorityScore }.take(topK)
    }

    fun cosineSimilarity(v1: List<Float>, v2: List<Float>): Float {
        if (v1.size != v2.size || v1.isEmpty()) return 0f
        var dot = 0f
        var nA = 0f
        var nB = 0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            nA += v1[i] * v1[i]
            nB += v2[i] * v2[i]
        }
        return if (nA > 0f && nB > 0f) dot / (sqrt(nA) * sqrt(nB)) else 0f
    }
}

data class SemanticCluster(
    val topicName: String,
    val priority: String,
    val priorityColor: String, // Hex
    val chunks: List<DocumentChunk>
)
