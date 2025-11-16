package com.edgedetection.viewer.gl

import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer

/**
 * OpenGL Texture Handler
 *
 * Utilities for creating and updating OpenGL textures
 */
object GLTextureHandler {

    private const val TAG = "GLTextureHandler"

    /**
     * Create a new OpenGL texture
     * @return Texture ID
     */
    fun createTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)

        val textureId = textures[0]
        if (textureId == 0) {
            Log.e(TAG, "Failed to generate texture")
            return 0
        }

        // Bind texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // Set texture parameters
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )

        // Unbind
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        Log.i(TAG, "Texture created: $textureId")
        return textureId
    }

    /**
     * Update texture with new frame data
     * @param textureId OpenGL texture ID
     * @param data RGBA pixel data
     * @param width Frame width
     * @param height Frame height
     */
    fun updateTexture(textureId: Int, data: ByteBuffer, width: Int, height: Int) {
        if (textureId == 0) {
            Log.e(TAG, "Invalid texture ID")
            return
        }

        // Bind texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // Upload pixel data
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,                          // Mipmap level
            GLES20.GL_RGBA,             // Internal format
            width,
            height,
            0,                          // Border (must be 0)
            GLES20.GL_RGBA,             // Format
            GLES20.GL_UNSIGNED_BYTE,    // Type
            data
        )

        // Check for errors
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "glTexImage2D error: $error")
        }

        // Unbind
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    /**
     * Delete texture
     * @param textureId Texture ID to delete
     */
    fun deleteTexture(textureId: Int) {
        if (textureId != 0) {
            val textures = intArrayOf(textureId)
            GLES20.glDeleteTextures(1, textures, 0)
            Log.i(TAG, "Texture deleted: $textureId")
        }
    }
}
