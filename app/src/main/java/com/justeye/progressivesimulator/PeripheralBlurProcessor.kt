package com.justeye.progressivesimulator

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Makes a small, softened copy of a CameraX frame for the simulated peripheral blur.
 * The original camera preview remains live and sharp underneath the clear corridor.
 */
fun ImageProxy.toPeripheralBlurBitmap(): Bitmap? {
    return try {
        val imageWidth = width
        val imageHeight = height
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val nv21 = ByteArray(imageWidth * imageHeight * 3 / 2)
        val yBuffer = yPlane.buffer.duplicate()
        val uBuffer = uPlane.buffer.duplicate()
        val vBuffer = vPlane.buffer.duplicate()
        val yStart = yBuffer.position()
        val uStart = uBuffer.position()
        val vStart = vBuffer.position()

        var outputIndex = 0
        for (row in 0 until imageHeight) {
            for (col in 0 until imageWidth) {
                nv21[outputIndex++] = yBuffer.get(
                    yStart + row * yPlane.rowStride + col * yPlane.pixelStride
                )
            }
        }

        for (row in 0 until imageHeight / 2) {
            for (col in 0 until imageWidth / 2) {
                val vIndex = vStart + row * vPlane.rowStride + col * vPlane.pixelStride
                val uIndex = uStart + row * uPlane.rowStride + col * uPlane.pixelStride
                nv21[outputIndex++] = vBuffer.get(vIndex)
                nv21[outputIndex++] = uBuffer.get(uIndex)
            }
        }

        val jpegBytes = ByteArrayOutputStream().use { output ->
            YuvImage(nv21, ImageFormat.NV21, imageWidth, imageHeight, null)
                .compressToJpeg(Rect(0, 0, imageWidth, imageHeight), 65, output)
            output.toByteArray()
        }

        val decoded = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            ?: return null

        val rotation = imageInfo.rotationDegrees
        val upright = if (rotation == 0) {
            decoded
        } else {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                .also { if (it !== decoded) decoded.recycle() }
        }

        // Blur a reduced copy to keep real-time processing light on the phone.
        val smallWidth = max(1, upright.width / 3)
        val smallHeight = max(1, upright.height / 3)
        val small = Bitmap.createScaledBitmap(upright, smallWidth, smallHeight, true)
        if (small !== upright) upright.recycle()

        val softened = boxBlur(small, radius = 7)
        if (softened !== small) small.recycle()

        Bitmap.createScaledBitmap(softened, max(1, imageWidthAfterRotation(softened, rotation, imageWidth, imageHeight)), max(1, imageHeightAfterRotation(softened, rotation, imageWidth, imageHeight)), true)
            .also { if (it !== softened) softened.recycle() }
    } catch (_: Exception) {
        null
    } finally {
        close()
    }
}

private fun imageWidthAfterRotation(bitmap: Bitmap, rotation: Int, originalWidth: Int, originalHeight: Int): Int =
    if (rotation == 90 || rotation == 270) originalHeight else originalWidth

private fun imageHeightAfterRotation(bitmap: Bitmap, rotation: Int, originalWidth: Int, originalHeight: Int): Int =
    if (rotation == 90 || rotation == 270) originalWidth else originalHeight

private fun boxBlur(source: Bitmap, radius: Int): Bitmap {
    val width = source.width
    val height = source.height
    val input = IntArray(width * height)
    val horizontal = IntArray(width * height)
    val output = IntArray(width * height)
    source.getPixels(input, 0, width, 0, 0, width, height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            var red = 0
            var green = 0
            var blue = 0
            var count = 0
            for (dx in -radius..radius) {
                val px = (x + dx).coerceIn(0, width - 1)
                val color = input[y * width + px]
                red += (color shr 16) and 0xff
                green += (color shr 8) and 0xff
                blue += color and 0xff
                count++
            }
            horizontal[y * width + x] =
                (0xff shl 24) or
                ((red / count) shl 16) or
                ((green / count) shl 8) or
                (blue / count)
        }
    }

    for (y in 0 until height) {
        for (x in 0 until width) {
            var red = 0
            var green = 0
            var blue = 0
            var count = 0
            for (dy in -radius..radius) {
                val py = (y + dy).coerceIn(0, height - 1)
                val color = horizontal[py * width + x]
                red += (color shr 16) and 0xff
                green += (color shr 8) and 0xff
                blue += color and 0xff
                count++
            }
            output[y * width + x] =
                (0xff shl 24) or
                ((red / count) shl 16) or
                ((green / count) shl 8) or
                (blue / count)
        }
    }

    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(output, 0, width, 0, 0, width, height)
    }
}
