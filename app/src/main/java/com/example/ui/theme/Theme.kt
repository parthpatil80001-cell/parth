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

private val DarkColorScheme =
  darkColorScheme(
    primary = HighDensityAccent,
    onPrimary = HighDensityOnAccent,
    background = HardDarkBackground,
    onBackground = TextPrimary,
    surface = HighDensitySurface,
    onSurface = TextPrimary,
    surfaceVariant = HighDensitySurface,
    onSurfaceVariant = TextSecondary,
    outline = HighDensityOutline
  )

private val LightColorScheme = DarkColorScheme // Both light/dark adopt high density dark style for pro look

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling system-wide dynamic color override to preserve specific designed brand colors
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme


  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
