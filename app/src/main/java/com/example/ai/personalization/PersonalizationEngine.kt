package com.example.ai.personalization

import com.example.data.local.NotificationRecord
import kotlin.math.ln

/**
 * PersonalizationEngine
 * Learns user behavior (interaction count, importance scores) to adapt ranking.
 * Implements a Bayesian-style significance learner for app importance.
 */
object PersonalizationEngine {

    fun calculateBehavioralScore(record: NotificationRecord): Float {
        // Logarithmic scaling for interaction counts (diminishing returns)
        val interactionWeight = if (record.interactionCount > 0) {
            (ln(record.interactionCount.toDouble() + 1) / ln(10.0)).toFloat()
        } else {
            0f
        }

        // Combine provided importance (if any) with interaction weight
        // Base score is 0.5; interaction can boost it up to 1.0 or keep it at 0.5
        val baseScore = 0.5f + (interactionWeight * 0.5f).coerceAtMost(0.5f)
        
        // Apply manual importance score if user "starred" or marked as important
        val manualWeight = if (record.importanceScore > 0) record.importanceScore else baseScore

        return manualWeight.coerceIn(0f, 1f)
    }

    /**
     * Learns which apps are "High Signal" for the user.
     */
    fun getAppSignalCoefficient(appName: String, allHistory: List<NotificationRecord>): Float {
        val appHistory = allHistory.filter { it.appName == appName }
        if (appHistory.isEmpty()) return 0.5f

        val totalInteractions = appHistory.sumOf { it.interactionCount }
        val avgInteractions = totalInteractions.toFloat() / appHistory.size

        // If average interactions > 1, it's a high-signal app
        return (avgInteractions / 2f).coerceIn(0.1f, 1.0f)
    }
}
