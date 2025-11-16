package com.edgedetection.viewer.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 2.0 Renderer
 *
 * Responsibilities:
 * - Initialize OpenGL context and shaders
 * - Create textured quad for full-screen rendering
 * - Update texture with processed frame data
 * - Render at 60 FPS
 */
class GLRenderer : GLSurfaceView.Renderer {

    private var program: Int = 0
    private var textureId: Int = 0

    private var positionHandle: Int = 0
    private var texCoordHandle: Int = 0
    private var textureHandle: Int = 0

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer

    private var frameWidth: Int = 0
    private var frameHeight: Int = 0
    private var frameData: ByteBuffer? = null
    private val frameLock = Object()

    companion object {
        private const val TAG = "GLRenderer"

        // Full-screen quad vertices (NDC: -1 to 1)
        private val VERTEX_COORDS = floatArrayOf(
            -1.0f,  1.0f,  // Top-left
            -1.0f, -1.0f,  // Bottom-left
            1.0f,  1.0f,  // Top-right
            1.0f, -1.0f   // Bottom-right
        )

        // Texture coordinates (0 to 1, flipped vertically)
        private val TEXTURE_COORDS = floatArrayOf(
            0.0f, 0.0f,  // Top-left
            0.0f, 1.0f,  // Bottom-left
            1.0f, 0.0f,  // Top-right
            1.0f, 1.0f   // Bottom-right
        )

        // Vertex shader
        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        // Fragment shader
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.i(TAG, "onSurfaceCreated")

        // Set clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Initialize buffers
        vertexBuffer = ByteBuffer.allocateDirect(VERTEX_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(VERTEX_COORDS)
                position(0)
            }

        texCoordBuffer = ByteBuffer.allocateDirect(TEXTURE_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(TEXTURE_COORDS)
                position(0)
            }

        // Create shader program
        program = ShaderUtils.createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (program == 0) {
            Log.e(TAG, "Failed to create shader program")
            return
        }

        // Get attribute/uniform locations
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")

        // Create texture
        textureId = GLTextureHandler.createTexture()

        Log.i(TAG, "OpenGL initialized successfully")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.i(TAG, "onSurfaceChanged: ${width}x${height}")
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Check if we have frame data
        synchronized(frameLock) {
            if (frameData != null && frameWidth > 0 && frameHeight > 0) {
                // Upload texture
                GLTextureHandler.updateTexture(
                    textureId,
                    frameData!!,
                    frameWidth,
                    frameHeight
                )
            }
        }

        // Use shader program
        GLES20.glUseProgram(program)

        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureHandle, 0)

        // Set vertex positions
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            2,  // 2 components per vertex (x, y)
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )

        // Set texture coordinates
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            2,  // 2 components per texcoord (u, v)
            GLES20.GL_FLOAT,
            false,
            0,
            texCoordBuffer
        )

        // Draw quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Disable attributes
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    /**
     * Update texture with new frame data
     * Thread-safe method called from camera callback
     */
    fun updateTexture(data: ByteArray, width: Int, height: Int) {
        synchronized(frameLock) {
            frameWidth = width
            frameHeight = height

            // Allocate or reuse buffer
            if (frameData == null || frameData!!.capacity() != data.size) {
                frameData = ByteBuffer.allocateDirect(data.size)
                    .order(ByteOrder.nativeOrder())
            }

            frameData!!.clear()
            frameData!!.put(data)
            frameData!!.position(0)
        }
    }
}
