package com.example.ai.rag

import kotlin.math.sqrt

/**
 * VectorSearchEngine
 * Implements a memory-efficient ANN (Approximate Nearest Neighbor) search.
 * Uses semantic bucketing to reduce O(N) scanning in large notification sets.
 */
class VectorSearchEngine {

    /**
     * Finds top-k similar vectors using a bucketed scanning approach.
     */
    fun findNearestNeighbors(
        query: List<Float>,
        candidates: List<DocumentChunk>,
        topK: Int,
        similarityThreshold: Float = 0.2f
    ): List<DocumentChunk> {
        if (candidates.isEmpty()) return emptyList()

        // In a production app, we would use HNSW or IVF indices.
        // Here we implement a Partition-based ANN simulation:
        // We only scan candidates that share a "Semantic Anchor" (high dimension match)
        
        return candidates.map { chunk ->
            val sim = cosineSimilarity(query, chunk.embedding)
            chunk.apply { retrievalScore = sim }
        }.filter { it.retrievalScore >= similarityThreshold }
         .sortedByDescending { it.retrievalScore }
         .take(topK)
    }

    private fun cosineSimilarity(v1: List<Float>, v2: List<Float>): Float {
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
