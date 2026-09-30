package com.eduapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun EduAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1B4F9C),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD8E4FF),
            onPrimaryContainer = Color(0xFF001A41),
            secondary = Color(0xFF3F6F9F),
            background = Color(0xFFF7F9FC),
            surface = Color.White,
            surfaceVariant = Color(0xFFEAEFF7),
            error = Color(0xFFB3261E)
        ),
        content = content
    )
}
