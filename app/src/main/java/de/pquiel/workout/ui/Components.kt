package de.pquiel.workout.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pquiel.workout.R
import de.pquiel.workout.model.Exercise

/** Farbiger Kreis mit der Übungsnummer, wie auf dem Plan. */
@Composable
fun NumberBadge(number: Int, color: Color, size: Dp = 36.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.5f).sp,
        )
    }
}

/** Kreisförmiger Fortschrittsring; [fraction] ist der verbleibende Anteil von 0 bis 1. */
@Composable
fun CountdownRing(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = color.copy(alpha = 0.18f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

/** Illustration einer Übung, sofern es eine gibt (bisher für die Running-Mobilisation). */
@get:DrawableRes
private val Exercise.illustration: Int?
    get() = when (name) {
        "Banded Arm Circle" -> R.drawable.mobility_banded_arm_circle
        "Twisted Warrior" -> R.drawable.mobility_twisted_warrior
        "Side-Lying Hip Circle" -> R.drawable.mobility_side_lying_hip_circle
        "Deep Squat Rock" -> R.drawable.mobility_deep_squat_rock
        "Pigeon Hip Extension" -> R.drawable.mobility_pigeon_hip_extension
        "Directional Leg Swing" -> R.drawable.mobility_directional_leg_swing
        else -> null
    }

/** Zeigt die Illustration zu [exercise] in voller Breite; ohne Illustration wird nichts angezeigt. */
@Composable
fun ExerciseIllustration(exercise: Exercise, modifier: Modifier = Modifier) {
    val res = exercise.illustration ?: return
    Image(
        painter = painterResource(res),
        contentDescription = "Illustration: ${exercise.germanName}",
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .aspectRatio(240f / 160f),
    )
}
