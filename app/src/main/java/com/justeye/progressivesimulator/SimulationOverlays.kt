package com.justeye.progressivesimulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun ProgressiveOverlay(
    design: Int,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {

        val width = size.width
        val height = size.height

        val narrowness = when (design) {
            1 -> 0.48f
            2 -> 0.32f
            else -> 0.20f
        }

        val center = width / 2f
        val clearWidth = width * (1f - narrowness)

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.72f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.72f)
                ),
                startX = center - clearWidth / 2f,
                endX = center + clearWidth / 2f
            )
        )

        val lowerShade = when (design) {
            1 -> 0.28f
            2 -> 0.18f
            else -> 0.10f
        }

        drawRect(
            color = Color.Black.copy(alpha = lowerShade),
            topLeft = Offset(0f, height * 0.70f),
            size = androidx.compose.ui.geometry.Size(
                width,
                height * 0.30f
            )
        )
    }
}

@Composable
fun CompareOverlay(
    mode: String,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {

        val width = size.width
        val height = size.height
        val half = width / 2f

        drawRect(
            color = Color.Black.copy(alpha = 0.48f),
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(
                half,
                height
            )
        )

        drawRect(
            color = Color.Black.copy(alpha = 0.18f),
            topLeft = Offset(half, 0f),
            size = androidx.compose.ui.geometry.Size(
                half,
                height
            )
        )

        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = Offset(half, 0f),
            end = Offset(half, height),
            strokeWidth = 3f
        )
    }
}
