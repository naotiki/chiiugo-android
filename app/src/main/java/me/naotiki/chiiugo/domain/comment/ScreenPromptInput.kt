package me.naotiki.chiiugo.domain.comment

enum class ScreenPromptSourceType {
    IMAGE,
    OCR_TEXT,
    ACCESSIBILITY_TEXT
}

data class ScreenPromptInput(
    val sourceType: ScreenPromptSourceType,
    val imageJpegBytes: ByteArray? = null,
    val ocrText: String? = null,
    val appName: String? = null,
    val packageName: String? = null,
    val activityName: String? = null
)
