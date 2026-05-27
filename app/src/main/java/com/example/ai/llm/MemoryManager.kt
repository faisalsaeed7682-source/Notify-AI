package com.example.ai.llm

import com.example.ai.rag.SemanticCluster

/**
 * Hierarchical Memory System
 * Manages long-term compression of notification history into rolling summaries.
 */
object MemoryManager {
    private val rollingSummaries = mutableListOf<String>()

    fun updateMemory(clusters: List<SemanticCluster>) {
        if (clusters.isEmpty()) return

        val summary = StringBuilder("Summary (${System.currentTimeMillis()}): ")
        clusters.take(3).forEach { cluster ->
            summary.append("${cluster.topicName} [${cluster.priority}]; ")
        }
        
        rollingSummaries.add(0, summary.toString())
        
        // Keep only top 10 historical memory points (pruning)
        if (rollingSummaries.size > 10) {
            rollingSummaries.removeAt(10)
        }
    }

    fun getMemoryContext(): String {
        return if (rollingSummaries.isEmpty()) {
            "No historical context available."
        } else {
            "Historical Trends:\n" + rollingSummaries.joinToString("\n")
        }
    }
}
