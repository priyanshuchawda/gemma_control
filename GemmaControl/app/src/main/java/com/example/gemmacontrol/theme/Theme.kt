package com.example.gemmacontrol.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

internal val GemmaControlDarkColorScheme =
  darkColorScheme(
    primary = GemmaBlue80,
    secondary = GemmaTeal80,
    tertiary = GemmaGreen80,
    background = GemmaDarkBackground,
    surface = GemmaDarkSurface,
    surfaceVariant = GemmaDarkSurfaceVariant,
    primaryContainer = GemmaBlue40,
    secondaryContainer = GemmaTeal40,
    tertiaryContainer = GemmaOrange40,
  )

internal val GemmaControlLightColorScheme =
  lightColorScheme(
    primary = GemmaBlue40,
    secondary = GemmaTeal40,
    tertiary = GemmaGreen40,
    background = GemmaLightBackground,
    surface = GemmaLightSurface,
    surfaceVariant = GemmaLightSurfaceVariant,
    primaryContainer = GemmaBlue80,
    secondaryContainer = GemmaTeal80,
    tertiaryContainer = GemmaOrange80,
  )

@Composable
fun GemmaControlTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> GemmaControlDarkColorScheme
      else -> GemmaControlLightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
