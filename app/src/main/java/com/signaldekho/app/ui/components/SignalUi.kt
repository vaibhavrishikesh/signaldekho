package com.signaldekho.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.signaldekho.app.R
import com.signaldekho.app.domain.Grade
import com.signaldekho.app.ui.theme.GradeExcellent
import com.signaldekho.app.ui.theme.GradeGood
import com.signaldekho.app.ui.theme.GradeVeryWeak
import com.signaldekho.app.ui.theme.GradeWeak

fun gradeColor(g: Grade): Color = when (g) {
    Grade.EXCELLENT -> GradeExcellent
    Grade.GOOD -> GradeGood
    Grade.WEAK -> GradeWeak
    Grade.VERY_WEAK -> GradeVeryWeak
}

@Composable
fun gradeLabel(g: Grade): String = stringResource(
    when (g) {
        Grade.EXCELLENT -> R.string.grade_excellent
        Grade.GOOD -> R.string.grade_good
        Grade.WEAK -> R.string.grade_weak
        Grade.VERY_WEAK -> R.string.grade_very_weak
    }
)

/** Segmented strength meter: lit segments scale with [fraction]. */
@Composable
fun SegmentBar(
    fraction: Float,
    color: Color,
    segments: Int = 4,
    modifier: Modifier = Modifier,
) {
    val lit = (fraction * segments).toInt().coerceIn(if (fraction > 0f) 1 else 0, segments)
    val dim = MaterialTheme.colorScheme.surfaceVariant
    Row(modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(segments) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (index < lit) color else dim, RoundedCornerShape(2.dp))
            )
        }
    }
}

/** Continuous bar whose filled width is [fraction] of the track. */
@Composable
fun FillBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier
            .height(7.dp)
            .background(track, RoundedCornerShape(3.dp))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(color, RoundedCornerShape(3.dp))
        )
    }
}
