package com.kptgames.vocabuildary.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A60),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7FFF2),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF4D5F91),
    tertiary = Color(0xFF7B5870),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE4E8F0),
    onBackground = Color(0xFF151C24),
    onSurface = Color(0xFF151C24),
    onSurfaceVariant = Color(0xFF444B57)
)

@Composable
fun VocabuildaryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
