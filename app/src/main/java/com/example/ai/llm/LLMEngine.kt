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
        delay(300)
        
        val response = if (prompt.contains("summarize", ignoreCase = true)) {
            "I've analyzed your recent notifications. Most are related to work and messaging. No high-priority alerts were found."
        } else {
            "Based on the available notification logs, I can confirm that your recent alerts mention '$prompt'. No critical issues detected."
        }

        val tokens = response.split(" ")
        for (token in tokens) {
            emit("$token ")
            delay(50)
        }
        onLog("llama_decode: success (18.5 tokens/sec)")
    }
}
