package de.pquiel.workout.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import de.pquiel.workout.model.Exercise
import de.pquiel.workout.model.Prepare
import de.pquiel.workout.model.Rest
import de.pquiel.workout.model.Segment
import de.pquiel.workout.model.Work

val Navy = Color(0xFF0B2A4A)
val NavyLight = Color(0xFF1F5A96)
val RestGreen = Color(0xFF2E9E44)
val RestGreenLight = Color(0xFFD5EED7)
val Surface = Color(0xFFF2F5F9)

val Exercise.color: Color get() = Color(colorArgb)

fun Segment.accentColor(): Color = when (this) {
    is Work -> exercise.color
    is Rest -> RestGreen
    is Prepare -> NavyLight
}

private val colors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = NavyLight,
    background = Surface,
    surface = Color.White,
    surfaceVariant = Color(0xFFE6ECF3),
)

@Composable
fun WorkoutTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
