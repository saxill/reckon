package com.sahilchanna.reckon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sahilchanna.reckon.ui.theme.palette
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Sparkline of the last rounds' average solve time. Lower is better, so the
 * scale is flipped: a rising line means you're getting faster.
 *
 * Swift Charts draws this with `.monotone` interpolation; the segments here are
 * monotone cubic Hermite (Fritsch–Carlson), which is what that method means —
 * a plain Catmull-Rom would overshoot between two close rounds and invent a dip
 * that never happened.
 */
@Composable
fun PaceChart(pace: List<Double>, modifier: Modifier = Modifier) {
    val amber = palette.amber
    val good = palette.good

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clipToBounds(),
    ) {
        if (pace.size < 2) return@Canvas

        val (low, high) = bounds(pace)
        val span = (high - low).takeIf { it > 0 } ?: 1.0

        // Mirror the value inside the domain so faster rounds sit higher.
        fun flip(seconds: Double): Double = low + high - seconds

        // Breathing room so the marker on the newest round isn't half cut off.
        val xLow = -0.35
        val xHigh = max(pace.size - 1, 1) + 0.35
        val xSpan = xHigh - xLow

        fun xOf(index: Int): Float = (((index - xLow) / xSpan) * size.width).toFloat()
        fun yOf(seconds: Double): Float =
            (((high - flip(seconds)) / span) * size.height).toFloat()

        val points = pace.mapIndexed { index, seconds -> Offset(xOf(index), yOf(seconds)) }

        val line = monotonePath(points)
        val area = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }

        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                colors = listOf(amber.copy(alpha = 0.01f), amber.copy(alpha = 0.28f)),
                startY = 0f,
                endY = size.height,
            ),
        )
        drawPath(
            path = line,
            color = amber,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        )

        val best = pace.minOrNull() ?: 0.0
        drawCircle(color = good, radius = 4.dp.toPx(), center = Offset(xOf(pace.indexOf(best)), yOf(best)))
    }
}

/** `pad` mirrors the Swift `max(0.15, (hi - lo) * 0.35)`. */
private fun bounds(pace: List<Double>): Pair<Double, Double> {
    val lo = pace.minOrNull() ?: 0.0
    val hi = pace.maxOrNull() ?: 1.0
    val pad = max(0.15, (hi - lo) * 0.35)
    return max(0.0, lo - pad) to (hi + pad)
}

/**
 * Builds a monotone cubic spline through [points]. The tangents are limited so
 * the curve can never leave the corridor between neighbouring values.
 */
private fun monotonePath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path

    val n = points.size
    val dx = FloatArray(n - 1) { points[it + 1].x - points[it].x }
    val slope = FloatArray(n - 1) {
        if (dx[it] == 0f) 0f else (points[it + 1].y - points[it].y) / dx[it]
    }

    val m = FloatArray(n)
    m[0] = slope[0]
    m[n - 1] = slope[n - 2]
    for (i in 1 until n - 1) {
        m[i] = if (slope[i - 1] * slope[i] <= 0f) 0f else (slope[i - 1] + slope[i]) / 2f
    }
    for (i in 0 until n - 1) {
        if (slope[i] == 0f) {
            m[i] = 0f
            m[i + 1] = 0f
            continue
        }
        val a = m[i] / slope[i]
        val b = m[i + 1] / slope[i]
        val s = a * a + b * b
        if (s > 9f) {
            val t = 3f / sqrt(s)
            m[i] = t * a * slope[i]
            m[i + 1] = t * b * slope[i]
        }
    }

    for (i in 0 until n - 1) {
        path.cubicTo(
            points[i].x + dx[i] / 3f,
            points[i].y + m[i] * dx[i] / 3f,
            points[i + 1].x - dx[i] / 3f,
            points[i + 1].y - m[i + 1] * dx[i] / 3f,
            points[i + 1].x,
            points[i + 1].y,
        )
    }
    return path
}
