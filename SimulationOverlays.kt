package com.justeye.progressivesimulator

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun ProgressiveOverlay(
    design: Int,
    modifier: Modifier = Modifier
) {
    val clearHalfWidth = when (design) {
        1 -> 0.20f
        2 -> 0.28f
        else -> 0.35f
    }

    Canvas(modifier = modifier) {

        drawProgressivePanel(
            left = 0f,
            right = size.width,
            clearHalfWidth = clearHalfWidth,
            height = size.height
        )
    }
}

@Composable
fun CompareOverlay(
    mode: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {

        when (mode) {

            "1 vs 2" -> {

                drawProgressivePanel(
                    left = 0f,
                    right = size.width / 2f,
                    clearHalfWidth = 0.20f,
                    height = size.height
                )

                drawProgressivePanel(
                    left = size.width / 2f,
                    right = size.width,
                    clearHalfWidth = 0.28f,
                    height = size.height
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }

            "1 vs 3" -> {

                drawProgressivePanel(
                    left = 0f,
                    right = size.width / 2f,
                    clearHalfWidth = 0.20f,
                    height = size.height
                )

                drawProgressivePanel(
                    left = size.width / 2f,
                    right = size.width,
                    clearHalfWidth = 0.35f,
                    height = size.height
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }

            "2 vs 3" -> {

                drawProgressivePanel(
                    left = 0f,
                    right = size.width / 2f,
                    clearHalfWidth = 0.28f,
                    height = size.height
                )

                drawProgressivePanel(
                    left = size.width / 2f,
                    right = size.width,
                    clearHalfWidth = 0.35f,
                    height = size.height
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }

            "1 vs 2 vs 3" -> {

                val third = size.width / 3f

                drawProgressivePanel(
                    left = 0f,
                    right = third,
                    clearHalfWidth = 0.20f,
                    height = size.height
                )

                drawProgressivePanel(
                    left = third,
                    right = third * 2f,
                    clearHalfWidth = 0.28f,
                    height = size.height
                )

                drawProgressivePanel(
                    left = third * 2f,
                    right = size.width,
                    clearHalfWidth = 0.35f,
                    height = size.height
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(third, 0f),
                    end = Offset(third, size.height),
                    strokeWidth = 1.dp.toPx()
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(third * 2f, 0f),
                    end = Offset(third * 2f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProgressivePanel(
    left: Float,
    right: Float,
    clearHalfWidth: Float,
    height: Float
) {

    val panelWidth = right - left
    val center = (left + right) / 2f

    val leftBoundary = center - panelWidth * clearHalfWidth
    val rightBoundary = center + panelWidth * clearHalfWidth

    val dottedEffect = PathEffect.dashPathEffect(
        floatArrayOf(
            14.dp.toPx(),
            10.dp.toPx()
        ),
        0f
    )

    val boundaryStroke = Stroke(
        width = 2.dp.toPx(),
        pathEffect = dottedEffect,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )

    val boundaryColor = Color.White.copy(alpha = 0.90f)

    val leftPath = Path()

    for (i in 0..40) {

        val t = i / 40f
        val y = height * t

        val curve =
            panelWidth *
                0.035f *
                ((abs(t - 0.52f) * 1.8f) *
                    (abs(t - 0.52f) * 1.8f))

        val x = leftBoundary + curve

        if (i == 0) {
            leftPath.moveTo(x, y)
        } else {
            leftPath.lineTo(x, y)
        }
    }

    val rightPath = Path()

    for (i in 0..40) {

        val t = i / 40f
        val y = height * t

        val curve =
            panelWidth *
                0.035f *
                ((abs(t - 0.52f) * 1.8f) *
                    (abs(t - 0.52f) * 1.8f))

        val x = rightBoundary - curve

        if (i == 0) {
            rightPath.moveTo(x, y)
        } else {
            rightPath.lineTo(x, y)
        }
    }

    drawPath(
        path = leftPath,
        color = boundaryColor,
        style = boundaryStroke
    )

    drawPath(
        path = rightPath,
        color = boundaryColor,
        style = boundaryStroke
    )

    val indicatorY = height * 0.52f

    drawLine(
        color = Color.White.copy(alpha = 0.90f),
        start = Offset(
            leftBoundary,
            indicatorY
        ),
        end = Offset(
            rightBoundary,
            indicatorY
        ),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
}
