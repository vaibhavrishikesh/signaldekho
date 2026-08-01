package com.signaldekho.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GradeExcellent = Color(0xFF1D9E75)
val GradeGood = Color(0xFF639922)
val GradeWeak = Color(0xFFEF9F27)
val GradeVeryWeak = Color(0xFFE24B4A)
val HeroTint = Color(0xFFE1F5EE)
val HeroText = Color(0xFF04342C)
val HeroSubtext = Color(0xFF0F6E56)

@Composable
fun SignalDekhoTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = scheme, content = content)
}
