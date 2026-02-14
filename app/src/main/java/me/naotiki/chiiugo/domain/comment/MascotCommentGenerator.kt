package me.naotiki.chiiugo.domain.comment

import me.naotiki.chiiugo.data.llm.LlmSettings
import me.naotiki.chiiugo.domain.context.MascotContextSnapshot

interface MascotCommentGenerator {
    suspend fun generate(snapshot: MascotContextSnapshot, settings: LlmSettings): String
    suspend fun generateScreenComment(input: ScreenPromptInput, settings: LlmSettings): String
}
