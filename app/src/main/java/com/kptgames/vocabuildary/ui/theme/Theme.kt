package com.kptgames.vocabuildary.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    secondary = Color(0xFF9A5B3F),
    tertiary = Color(0xFFB58A1B),
    background = Color(0xFFF7F2E8),
    surface = Color(0xFFFFFCF6),
    surfaceVariant = Color(0xFFE7DED1),
    onBackground = Color(0xFF172026),
    onSurface = Color(0xFF172026)
)

@Composable
fun VocabuildaryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
