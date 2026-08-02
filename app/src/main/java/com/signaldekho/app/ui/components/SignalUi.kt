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

/**
 * Lit segments for [grade]. Driven by the grade rather than the raw fraction so the
 * meter always agrees with the word beside it — at −55 dBm the fraction is only 0.58,
 * which would light 2 of 4 segments next to the word "Excellent".
 */
fun litSegments(grade: Grade, segments: Int): Int = when (grade) {
    Grade.EXCELLENT -> segments
    Grade.GOOD -> maxOf(1, segments * 3 / 4)
    Grade.WEAK -> maxOf(1, segments / 2)
    Grade.VERY_WEAK -> 1
}

/** Segmented strength meter whose lit count matches [grade]. */
@Composable
fun SegmentBar(
    grade: Grade,
    color: Color,
    segments: Int = 4,
    modifier: Modifier = Modifier,
) {
    val lit = litSegments(grade, segments)
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
