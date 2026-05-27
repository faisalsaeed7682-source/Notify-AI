package com.example.ai.embedding

interface EmbeddingService {
    suspend fun getEmbedding(text: String): List<Float>
    val dimensions: Int
}
