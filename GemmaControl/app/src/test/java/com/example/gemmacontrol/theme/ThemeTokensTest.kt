package com.example.gemmacontrol.theme

import androidx.compose.ui.graphics.Color
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import org.junit.Test

class ThemeTokensTest {
    @Test
    fun lightSchemeUsesGemmaControlBrandRolesInsteadOfTemplatePurple() {
        assertEquals(GemmaBlue40, GemmaControlLightColorScheme.primary)
        assertEquals(GemmaTeal40, GemmaControlLightColorScheme.secondary)
        assertEquals(GemmaGreen40, GemmaControlLightColorScheme.tertiary)
        assertFalse(GemmaControlLightColorScheme.primary == Color(0xFF6650A4))
    }

    @Test
    fun appTypographyUsesZeroLetterSpacingForPrimaryTextStyles() {
        assertEquals(0f, Typography.bodyLarge.letterSpacing.value)
        assertEquals(0f, Typography.titleLarge.letterSpacing.value)
        assertEquals(0f, Typography.labelMedium.letterSpacing.value)
    }
}
