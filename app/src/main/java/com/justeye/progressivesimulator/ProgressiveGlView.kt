package com.justeye.progressivesimulator

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.Executor
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Camera preview rendered through an OpenGL shader so blur is applied to camera pixels,
 * rather than drawing a translucent overlay on top of PreviewView.
 */
class ProgressiveGlView(
    context: Context,
    private val lifecycleOwner: LifecycleOwner
) : GLSurfaceView(context), Preview.SurfaceProvider {

    private val glRenderer = ProgressiveRenderer()
    private val glExecutor = Executor { command -> queueEvent(command) }

    init {
        setEGLContextClientVersion(2)
        setRenderer(glRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(this)
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )
        }, ContextCompat.getMainExecutor(context))
    }

    fun setDesign(design: Int) {
        glRenderer.design = design.coerceIn(1, 3)
    }

    override fun onSurfaceRequested(request: SurfaceRequest) {
        queueEvent {
            glRenderer.provideCameraSurface(request, glExecutor)
        }
    }
}

private class ProgressiveRenderer : GLSurfaceView.Renderer {
    @Volatile var design: Int = 1

    private var program = 0
    private var textureId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var cameraSurface: Surface? = null
    private var frameAvailable = false
    private var viewWidth = 1
    private var viewHeight = 1
    private val transform = FloatArray(16)

    private val vertices = floatBuffer(
        floatArrayOf(
            -1f, -1f, 0f, 1f,
             1f, -1f, 1f, 1f,
            -1f,  1f, 0f, 0f,
             1f,  1f, 1f, 0f
        )
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        textureId = createExternalTexture()
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width.coerceAtLeast(1)
        viewHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewWidth, viewHeight)
    }

    fun provideCameraSurface(request: SurfaceRequest, callbackExecutor: Executor) {
        // CameraX requests surfaces on its own schedule. Create a fresh producer surface
        // for each request and release it only after CameraX returns its result callback.
        val texture = SurfaceTexture(textureId)
        texture.setDefaultBufferSize(
            request.resolution.width,
            request.resolution.height
        )
        texture.setOnFrameAvailableListener {
            synchronized(this) { frameAvailable = true }
        }
        val surface = Surface(texture)

        surfaceTexture = texture
        cameraSurface = surface

        request.provideSurface(surface, callbackExecutor) {
            queueCleanup(texture, surface)
        }
    }

    private fun queueCleanup(texture: SurfaceTexture, surface: Surface) {
        // provideSurface's result callback uses the GL executor.
        surface.release()
        texture.setOnFrameAvailableListener(null)
        texture.release()
        if (surfaceTexture === texture) surfaceTexture = null
        if (cameraSurface === surface) cameraSurface = null
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        val texture = surfaceTexture ?: return

        try {
            texture.updateTexImage()
            texture.getTransformMatrix(transform)
        } catch (_: RuntimeException) {
            return
        }

        GLES20.glUseProgram(program)
        val position = GLES20.glGetAttribLocation(program, "aPosition")
        val texCoord = GLES20.glGetAttribLocation(program, "aTexCoord")
        val matrixLocation = GLES20.glGetUniformLocation(program, "uTexMatrix")
        val samplerLocation = GLES20.glGetUniformLocation(program, "uCamera")
        val texelLocation = GLES20.glGetUniformLocation(program, "uTexel")
        val designLocation = GLES20.glGetUniformLocation(program, "uDesign")

        vertices.position(0)
        GLES20.glEnableVertexAttribArray(position)
        GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 16, vertices)
        vertices.position(2)
        GLES20.glEnableVertexAttribArray(texCoord)
        GLES20.glVertexAttribPointer(texCoord, 2, GLES20.GL_FLOAT, false, 16, vertices)

        GLES20.glUniformMatrix4fv(matrixLocation, 1, false, transform, 0)
        GLES20.glUniform1i(samplerLocation, 0)
        GLES20.glUniform2f(texelLocation, 1f / 720f, 1f / 1280f)
        GLES20.glUniform1f(designLocation, design.toFloat())

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(position)
        GLES20.glDisableVertexAttribArray(texCoord)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    private fun createExternalTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        return textures[0]
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val result = GLES20.glCreateProgram()
        GLES20.glAttachShader(result, vertex)
        GLES20.glAttachShader(result, fragment)
        GLES20.glLinkProgram(result)
        val status = IntArray(1)
        GLES20.glGetProgramiv(result, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val message = GLES20.glGetProgramInfoLog(result)
            GLES20.glDeleteProgram(result)
            throw IllegalStateException("OpenGL program link failed: $message")
        }
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        return result
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val message = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw IllegalStateException("OpenGL shader compile failed: $message")
        }
        return shader
    }

    companion object {
        private fun floatBuffer(values: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(values.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply { put(values); position(0) }

        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec4 aTexCoord;
            uniform mat4 uTexMatrix;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = (uTexMatrix * aTexCoord).xy;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES uCamera;
            uniform vec2 uTexel;
            uniform float uDesign;
            varying vec2 vTexCoord;

            vec4 cameraPixel(vec2 uv) {
                return texture2D(uCamera, clamp(uv, vec2(0.001), vec2(0.999)));
            }

            vec4 blurredPixel(vec2 uv) {
                vec2 d = uTexel * 3.0;
                vec4 c = cameraPixel(uv) * 0.20;
                c += cameraPixel(uv + vec2( d.x, 0.0)) * 0.12;
                c += cameraPixel(uv + vec2(-d.x, 0.0)) * 0.12;
                c += cameraPixel(uv + vec2(0.0,  d.y)) * 0.12;
                c += cameraPixel(uv + vec2(0.0, -d.y)) * 0.12;
                c += cameraPixel(uv + vec2( d.x,  d.y)) * 0.08;
                c += cameraPixel(uv + vec2(-d.x,  d.y)) * 0.08;
                c += cameraPixel(uv + vec2( d.x, -d.y)) * 0.08;
                c += cameraPixel(uv + vec2(-d.x, -d.y)) * 0.08;
                return c;
            }

            void main() {
                vec2 uv = vTexCoord;
                float yFromTop = 1.0 - uv.y;

                // Keep the upper distance-view zone clear for all three examples.
                float topZone = 1.0 - smoothstep(0.34, 0.43, yFromTop);

                // The clear corridor becomes progressively wider from Frame 1 to 3.
                float halfWidth = uDesign < 1.5 ? 0.105 :
                                  (uDesign < 2.5 ? 0.165 : 0.235);
                float corridor = halfWidth * smoothstep(0.34, 0.78, yFromTop)
                               + 0.30 * halfWidth;
                float edge = abs(uv.x - 0.5);
                float clearCenter = 1.0 - smoothstep(corridor, corridor + 0.018, edge);
                float clearMask = max(topZone, clearCenter);

                vec4 original = cameraPixel(uv);
                vec4 blur = blurredPixel(uv);
                gl_FragColor = mix(blur, original, clearMask);
            }
        """
    }
}
