package com.example.ai.intelligence

import com.example.data.local.NotificationRecord

data class IntelligenceResult(
    val priority: Float,
    val isOtp: Boolean,
    val isSpam: Boolean,
    val isUrgent: Boolean,
    val category: String,
    val tags: List<String>
)

object NotificationIntelligence {

    fun analyze(appName: String, title: String, content: String): IntelligenceResult {
        val text = "$appName $title $content".lowercase()
        var score = 0.25f // baseline

        val isSpam = isSpam(text)
        val isOtp = isOtp(text, content)
        val isUrgent = isUrgent(text)

        if (isSpam) score = 0.05f
        if (isOtp) score += 0.6f
        if (isUrgent) score += 0.5f
        
        val category = when {
            isOtp -> "Security"
            isSpam -> "Promotional"
            text.contains("bank") || text.contains("payment") -> "Finance"
            text.contains("whatsapp") || text.contains("chat") -> "Messaging"
            text.contains("system") || text.contains("battery") -> "System"
            else -> "Other"
        }

        return IntelligenceResult(
            priority = score.coerceIn(0f, 1f),
            isOtp = isOtp,
            isSpam = isSpam,
            isUrgent = isUrgent,
            category = category,
            tags = extractTags(text)
        )
    }

    private fun isSpam(text: String): Boolean {
        val spamIndicators = listOf("discount", "promo", "deal", "coupon", "exclusive reward", "won", "giftcard", "subscribe", "shop now", "sale")
        return spamIndicators.any { text.contains(it) }
    }

    private fun isOtp(text: String, content: String): Boolean {
        val regexOtp = Regex("(?i)\\b([A-Z0-9]{1,4}-[A-Z0-9]{3,8})\\b|\\b(\\d{4,8})\\b")
        return regexOtp.containsMatchIn(content) && (text.contains("code") || text.contains("otp") || text.contains("verify") || text.contains("login") || text.contains("mfa"))
    }

    private fun isUrgent(text: String): Boolean {
        return text.contains("critical") || text.contains("urgent") || text.contains("emergency") || text.contains("immediately")
    }

    private fun extractTags(text: String): List<String> {
        val tags = mutableListOf<String>()
        if (text.contains("meeting") || text.contains("zoom")) tags.add("Meeting")
        if (text.contains("deadline")) tags.add("Deadline")
        return tags
    }
}
