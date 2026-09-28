package com.justeye.progressivesimulator

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import kotlin.math.max
import kotlin.math.min

/*
 * Visual prototype of progressive-lens usable-field zones.
 * The clear corridor is intentionally illustrative, not an optical model.
 */
@Composable
fun ProgressiveOverlay(design: Int, modifier: Modifier = Modifier) {
    val strength = when (design) {
        1 -> 0.72f
        2 -> 0.48f
        else -> 0.28f
    }
    val widths = when (design) {
        1 -> floatArrayOf(.48f, .30f, .44f)
        2 -> floatArrayOf(.62f, .40f, .58f)
        else -> floatArrayOf(.76f, .52f, .72f)
    }

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // Darkened/blur-like peripheral zones. The center corridor remains clearer.
        val top = h * .10f
        val mid = h * .50f
        val bot = h * .90f
        val pts = listOf(
            Offset(w*(.5f-widths[0]/2), top),
            Offset(w*(.5f-widths[1]/2), mid),
            Offset(w*(.5f-widths[2]/2), bot),
            Offset(w*(.5f+widths[2]/2), bot),
            Offset(w*(.5f+widths[1]/2), mid),
            Offset(w*(.5f+widths[0]/2), top)
        )
        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawRect(Color.Black.copy(alpha = strength * .55f))
        drawPath(path, Color.Transparent, blendMode = BlendMode.Clear)
        // Soft boundary rings for an educational visual cue.
        drawPath(path, Color(0xFFD7A84A).copy(alpha = .22f), style = Stroke(width = 2f))
    }
}

@Composable
fun CompareOverlay(mode: String, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val left = w / 2f
        drawRect(Color.Black.copy(alpha = .58f))
        // Two side-by-side illustrative clear zones.
        val leftPath = progressivePath(0f, left, h, if (mode.contains("1")) .30f else .42f)
        val rightPath = progressivePath(left, w, h, if (mode.contains("3")) .72f else .55f)
        drawPath(leftPath, Color.Transparent, blendMode = BlendMode.Clear)
        drawPath(rightPath, Color.Transparent, blendMode = BlendMode.Clear)
        drawLine(Color(0xFFD7A84A), Offset(left, 0f), Offset(left, h), 2f)
    }
}

private fun progressivePath(x0: Float, x1: Float, h: Float, widthFactor: Float): Path {
    val center = (x0 + x1) / 2f
    val full = x1 - x0
    val topW = full * widthFactor
    val midW = full * widthFactor * .62f
    val botW = full * widthFactor * .88f
    return Path().apply {
        moveTo(center-topW/2, h*.08f)
        lineTo(center-midW/2, h*.52f)
        lineTo(center-botW/2, h*.92f)
        lineTo(center+botW/2, h*.92f)
        lineTo(center+midW/2, h*.52f)
        lineTo(center+topW/2, h*.08f)
        close()
    }
}
