package com.beecareanywhere.model

data class ModelConfig(
    val modelPath: String,
    val systemPrompt: String,
    val maxTokens: Int = 1024,
    val temperature: Float = 0.4f,
    val topK: Int = 40,
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT: String =
            "You are BeeCare, an offline diagnostic assistant for smallholder beekeepers. " +
                "Give region-aware, actionable advice. If you are uncertain, say so and recommend " +
                "contacting a local extension officer."
    }
}
