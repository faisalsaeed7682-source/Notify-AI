package com.example.ai.intelligence

import com.example.data.local.NotificationRecord

data class IntelligenceResult(
    val priority: Float,
    val isOtp: Boolean,
    val isSpam: Boolean,
    val isUrgent: Boolean,
    val category: String,
    val tags: List<String>,
    val reasoning: String
)

object NotificationIntelligence {

    fun analyze(appName: String, title: String, content: String): IntelligenceResult {
        val text = "$appName $title $content".lowercase()
        var score = 0.25f // baseline
        var reason = "Normal notification."

        val isSpam = isSpam(text)
        val isOtp = isOtp(text, content)
        val isUrgent = isUrgent(text)
        val isFinancial = isFinancial(text)

        if (isSpam) {
            score = 0.05f
            reason = "Detected promotional or duplicate keywords."
        }
        if (isFinancial) {
            score += 0.35f
            reason = "Sensitive financial transaction or banking alert."
        }
        if (isUrgent) {
            score += 0.45f
            reason = "Contains urgent time-sensitive keywords."
        }
        if (isOtp) {
            score = 0.95f
            reason = "Critical authentication code detected."
        }
        
        val category = when {
            isOtp -> "Security"
            isSpam -> "Promotional"
            isFinancial -> "Finance"
            text.contains("whatsapp") || text.contains("chat") || text.contains("messenger") -> "Social"
            text.contains("zoom") || text.contains("slack") || text.contains("meeting") || text.contains("email") -> "Work"
            text.contains("system") || text.contains("battery") || text.contains("update") -> "System"
            else -> "Personal"
        }

        return IntelligenceResult(
            priority = score.coerceIn(0f, 1f),
            isOtp = isOtp,
            isSpam = isSpam,
            isUrgent = isUrgent,
            category = category,
            tags = extractTags(text),
            reasoning = reason
        )
    }

    private fun isSpam(text: String): Boolean {
        val spamIndicators = listOf("discount", "promo", "deal", "coupon", "exclusive reward", "won", "giftcard", "subscribe", "shop now", "sale", "off on your next")
        return spamIndicators.any { text.contains(it) }
    }

    private fun isOtp(text: String, content: String): Boolean {
        val regexOtp = Regex("(?i)\\b([A-Z0-9]{1,4}-[A-Z0-9]{3,8})\\b|\\b(\\d{4,8})\\b")
        val securityKeywords = listOf("code", "otp", "verify", "login", "mfa", "2fa", "authentication", "password")
        return regexOtp.containsMatchIn(content) && securityKeywords.any { text.contains(it) }
    }

    private fun isUrgent(text: String): Boolean {
        val urgentKeywords = listOf("critical", "urgent", "emergency", "immediately", "hazard", "warning", "important action", "required now")
        return urgentKeywords.any { text.contains(it) }
    }

    private fun isFinancial(text: String): Boolean {
        val financeKeywords = listOf("bank", "payment", "card", "transfer", "transaction", "balance", "credit", "debit", "withdrawal")
        return financeKeywords.any { text.contains(it) }
    }

    private fun extractTags(text: String): List<String> {
        val tags = mutableListOf<String>()
        if (text.contains("meeting") || text.contains("zoom") || text.contains("teams")) tags.add("Meeting")
        if (text.contains("deadline") || text.contains("due")) tags.add("Deadline")
        if (text.contains("otp") || text.contains("code")) tags.add("Verification")
        return tags
    }
}
