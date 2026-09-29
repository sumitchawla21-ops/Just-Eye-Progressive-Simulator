package com.justeye.progressivesimulator

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Stroke

/*
 * Visual simulator overlay for progressive-lens usable-field zones.
 *
 * Design 1 = narrowest corridor
 * Design 2 = medium corridor
 * Design 3 = widest corridor
 *
 * This is an educational visual simulation and not an optical calculation.
 */

@Composable
fun ProgressiveOverlay(
    design: Int,
    modifier: Modifier = Modifier
) {
    val widths = when (design) {
        1 -> floatArrayOf(0.48f, 0.30f, 0.44f)
        2 -> floatArrayOf(0.62f, 0.40f, 0.58f)
        else -> floatArrayOf(0.76f, 0.52f, 0.72f)
    }

    val darkness = when (design) {
        1 -> 0.42f
        2 -> 0.34f
        else -> 0.26f
    }

    Canvas(modifier = modifier) {

        val w = size.width
        val h = size.height

        val topY = h * 0.10f
        val midY = h * 0.50f
        val bottomY = h * 0.90f

        val centerX = w / 2f

        val topHalf = w * widths[0] / 2f
        val midHalf = w * widths[1] / 2f
        val bottomHalf = w * widths[2] / 2f

        val topLeft = centerX - topHalf
        val topRight = centerX + topHalf

        val midLeft = centerX - midHalf
        val midRight = centerX + midHalf

        val bottomLeft = centerX - bottomHalf
        val bottomRight = centerX + bottomHalf

        val shade = Color.Black.copy(alpha = darkness)

        // TOP peripheral area
        drawRect(
            color = shade,
            topLeft = Offset(0f, 0f),
            size = Size(w, topY)
        )

        // LEFT peripheral area
        val leftPath = Path().apply {
            moveTo(0f, topY)
            lineTo(topLeft, topY)
            lineTo(midLeft, midY)
            lineTo(bottomLeft, bottomY)
            lineTo(0f, bottomY)
            close()
        }

        drawPath(
            path = leftPath,
            color = shade
        )

        // RIGHT peripheral area
        val rightPath = Path().apply {
            moveTo(topRight, topY)
            lineTo(w, topY)
            lineTo(w, bottomY)
            lineTo(bottomRight, bottomY)
            lineTo(midRight, midY)
            close()
        }

        drawPath(
            path = rightPath,
            color = shade
        )

        // BOTTOM peripheral area
        drawRect(
            color = shade,
            topLeft = Offset(0f, bottomY),
            size = Size(w, h - bottomY)
        )

        // Golden boundary around the usable corridor
        val corridorPath = Path().apply {
            moveTo(topLeft, topY)
            lineTo(midLeft, midY)
            lineTo(bottomLeft, bottomY)
            lineTo(bottomRight, bottomY)
            lineTo(midRight, midY)
            lineTo(topRight, topY)
            close()
        }

        drawPath(
            path = corridorPath,
            color = Color(0xFFD7A84A).copy(alpha = 0.30f),
            style = Stroke(width = 2f)
        )
    }
}


/*
 * Side-by-side comparison overlay.
 *
 * Left side  = narrower usable field
 * Right side = wider usable field
 */
@Composable
fun CompareOverlay(
    mode: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {

        val w = size.width
        val h = size.height
        val center = w / 2f

        val leftWidth =
            if (mode.contains("1")) 0.30f else 0.42f

        val rightWidth =
            if (mode.contains("3")) 0.72f else 0.55f

        val shade = Color.Black.copy(alpha = 0.45f)

        drawComparisonZone(
            x0 = 0f,
            x1 = center,
            h = h,
            widthFactor = leftWidth,
            shade = shade
        )

        drawComparisonZone(
            x0 = center,
            x1 = w,
            h = h,
            widthFactor = rightWidth,
            shade = shade
        )

        // Center divider
        drawLine(
            color = Color(0xFFD7A84A),
            start = Offset(center, 0f),
            end = Offset(center, h),
            strokeWidth = 2f
        )
    }
}


/*
 * Draws the peripheral shaded areas for one side of the comparison.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawComparisonZone(
    x0: Float,
    x1: Float,
    h: Float,
    widthFactor: Float,
    shade: Color
) {
    val fullWidth = x1 - x0
    val center = (x0 + x1) / 2f

    val topY = h * 0.08f
    val midY = h * 0.52f
    val bottomY = h * 0.92f

    val topHalf = fullWidth * widthFactor / 2f
    val midHalf = fullWidth * widthFactor * 0.62f / 2f
    val bottomHalf = fullWidth * widthFactor * 0.88f / 2f

    val topLeft = center - topHalf
    val topRight = center + topHalf

    val midLeft = center - midHalf
    val midRight = center + midHalf

    val bottomLeft = center - bottomHalf
    val bottomRight = center + bottomHalf

    // Top
    drawRect(
        color = shade,
        topLeft = Offset(x0, 0f),
        size = Size(fullWidth, topY)
    )

    // Left
    val leftPath = Path().apply {
        moveTo(x0, topY)
        lineTo(topLeft, topY)
        lineTo(midLeft, midY)
        lineTo(bottomLeft, bottomY)
        lineTo(x0, bottomY)
        close()
    }

    drawPath(
        path = leftPath,
        color = shade
    )

    // Right
    val rightPath = Path().apply {
        moveTo(topRight, topY)
        lineTo(x1, topY)
        lineTo(x1, bottomY)
        lineTo(bottomRight, bottomY)
        lineTo(midRight, midY)
        close()
    }

    drawPath(
        path = rightPath,
        color = shade
    )

    // Bottom
    drawRect(
        color = shade,
        topLeft = Offset(x0, bottomY),
        size = Size(fullWidth, h - bottomY)
    )

    // Golden corridor outline
    val corridorPath = Path().apply {
        moveTo(topLeft, topY)
        lineTo(midLeft, midY)
        lineTo(bottomLeft, bottomY)
        lineTo(bottomRight, bottomY)
        lineTo(midRight, midY)
        lineTo(topRight, topY)
        close()
    }

    drawPath(
        path = corridorPath,
        color = Color(0xFFD7A84A).copy(alpha = 0.25f),
        style = Stroke(width = 2f)
    )
}
