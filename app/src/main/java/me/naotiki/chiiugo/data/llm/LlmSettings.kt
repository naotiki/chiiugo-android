package me.naotiki.chiiugo.data.llm

data class LlmSettings(
    val enabled: Boolean = true,
    val baseUrl: String = "http://127.0.0.1:1234/v1",
    val model: String = "gpt-4o-mini",
    val cooldownSec: Int = 20,
    val maxTokens: Int = 1024,
    val temperature: Float = 0.9f,
    val personaStyle: String = "現行ノリを継承"
)
