package me.naotiki.chiiugo.data.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmSettingsRepositoryImplTest {

    @Test
    fun `legacy OFF analysis mode disables screen analysis and normalizes mode`() {
        val (enabled, mode) = normalizeScreenAnalysisSettings(
            persistedScreenAnalysisEnabled = true,
            persistedAnalysisMode = "OFF",
            defaultMode = ScreenAnalysisMode.MULTIMODAL_ONLY
        )

        assertFalse(enabled)
        assertEquals(ScreenAnalysisMode.MULTIMODAL_ONLY, mode)
    }

    @Test
    fun `unknown analysis mode falls back to default but keeps enabled flag`() {
        val (enabled, mode) = normalizeScreenAnalysisSettings(
            persistedScreenAnalysisEnabled = true,
            persistedAnalysisMode = "UNKNOWN_MODE",
            defaultMode = ScreenAnalysisMode.MULTIMODAL_ONLY
        )

        assertTrue(enabled)
        assertEquals(ScreenAnalysisMode.MULTIMODAL_ONLY, mode)
    }

    @Test
    fun `valid analysis mode is restored as is`() {
        val (enabled, mode) = normalizeScreenAnalysisSettings(
            persistedScreenAnalysisEnabled = true,
            persistedAnalysisMode = ScreenAnalysisMode.ACCESSIBILITY_ONLY.name,
            defaultMode = ScreenAnalysisMode.MULTIMODAL_ONLY
        )

        assertTrue(enabled)
        assertEquals(ScreenAnalysisMode.ACCESSIBILITY_ONLY, mode)
    }
}
