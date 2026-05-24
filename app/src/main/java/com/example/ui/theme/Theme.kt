package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
      primary = Color(0xFF64B5F6),
      onPrimary = Color(0xFF003258),
      primaryContainer = Color(0xFF00497D),
      onPrimaryContainer = Color(0xFFD1E4FF),
      secondary = Color(0xFFEF5350),
      onSecondary = Color(0xFF410002),
      secondaryContainer = Color(0xFF93000A),
      onSecondaryContainer = Color(0xFFFFDAD6),
      background = Color(0xFF1A1C1E),
      onBackground = Color(0xFFE2E2E5),
      surface = Color(0xFF1A1C1E),
      onSurface = Color(0xFFE2E2E5),
      surfaceVariant = Color(0xFF43474E),
      onSurfaceVariant = Color(0xFFC3C7CF)
  )

private val LightColorScheme =
  lightColorScheme(
      primary = Color(0xFF6750A4),
      onPrimary = Color(0xFFFFFFFF),
      primaryContainer = Color(0xFFEADDFF),
      onPrimaryContainer = Color(0xFF21005D),
      secondary = Color(0xFF625B71),
      onSecondary = Color(0xFFFFFFFF),
      secondaryContainer = Color(0xFFE8DEF8),
      onSecondaryContainer = Color(0xFF1D192B),
      background = Color(0xFFFFFFFF),
      onBackground = Color(0xFF1C1B1F),
      surface = Color(0xFFFFFFFF),
      onSurface = Color(0xFF1C1B1F),
      surfaceVariant = Color(0xFFE7E0EC),
      onSurfaceVariant = Color(0xFF49454F)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Turn off dynamic color so we enforce our red/blue theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
