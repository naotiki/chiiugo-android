package me.naotiki.chiiugo.domain.comment

data class ScreenPromptInput(
    val imageJpegBytes: ByteArray? = null,
    val ocrText: String? = null
)
