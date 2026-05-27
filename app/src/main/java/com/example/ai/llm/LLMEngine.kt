package com.example.ai.llm

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface LLMEngine {
    fun generate(prompt: String, context: String): Flow<String>
}

class MockLLMEngine(
    private val onLog: (String) -> Unit
) : LLMEngine {

    override fun generate(prompt: String, context: String): Flow<String> = flow {
        onLog("llama_decode: start decoding with KV cache reuse enabled")
        onLog("speculative_decoding: branching draft model (TinyLlama-15M) for prefix path...")
        delay(300)
        
        val response = if (prompt.contains("summarize", ignoreCase = true)) {
            "I've analyzed your recent notifications. Most are related to work and messaging. No high-priority alerts were found."
        } else {
            "Based on the available notification logs, I can confirm that your recent alerts mention '$prompt'. No critical issues detected."
        }

        val tokens = response.split(" ")
        for (i in tokens.indices) {
            val token = tokens[i]
            
            // Simulating Speculative Decoding: occasionally emit multiple tokens if draft model matched
            if (i % 4 == 0 && i + 1 < tokens.size) {
                emit("$token ${tokens[i+1]} ")
                onLog("speculative_decoding: hit! (path_match=true, accepted=2)")
                delay(20) // faster for spec hits
            } else if (i % 4 != 1) { // skip if we just spec-emitted the next one
                emit("$token ")
                delay(60)
            }
            
            // Simulating interruption recovery check
            if (i == 10) {
                onLog("llama_context: rotating KV cache window to maintain rolling context...")
            }
        }
        onLog("llama_decode: success (24.2 tokens/sec peak with speculation)")
    }
}
