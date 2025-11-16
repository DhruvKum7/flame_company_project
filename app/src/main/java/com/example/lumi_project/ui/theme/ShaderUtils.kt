package com.edgedetection.viewer.gl

import android.opengl.GLES20
import android.util.Log

/**
 * OpenGL Shader Utilities
 *
 * Helper functions for compiling and linking GLSL shaders
 */
object ShaderUtils {

    private const val TAG = "ShaderUtils"

    /**
     * Load and compile a shader
     * @param type Shader type (GL_VERTEX_SHADER or GL_FRAGMENT_SHADER)
     * @param shaderCode GLSL source code
     * @return Shader ID, or 0 if failed
     */
    fun loadShader(type: Int, shaderCode: String): Int {
        // Create shader object
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            Log.e(TAG, "Failed to create shader")
            return 0
        }

        // Load shader source
        GLES20.glShaderSource(shader, shaderCode)

        // Compile shader
        GLES20.glCompileShader(shader)

        // Check compilation status
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)

        if (compiled[0] == 0) {
            val info = GLES20.glGetShaderInfoLog(shader)
            Log.e(TAG, "Shader compilation failed:")
            Log.e(TAG, info)
            GLES20.glDeleteShader(shader)
            return 0
        }

        Log.i(TAG, "Shader compiled successfully: $shader")
        return shader
    }

    /**
     * Create shader program from vertex and fragment shaders
     * @param vertexShaderCode Vertex shader source
     * @param fragmentShaderCode Fragment shader source
     * @return Program ID, or 0 if failed
     */
    fun createProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
        // Compile shaders
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        if (vertexShader == 0) {
            return 0
        }

        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        if (fragmentShader == 0) {
            GLES20.glDeleteShader(vertexShader)
            return 0
        }

        // Create program
        val program = GLES20.glCreateProgram()
        if (program == 0) {
            Log.e(TAG, "Failed to create program")
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            return 0
        }

        // Attach shaders
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)

        // Link program
        GLES20.glLinkProgram(program)

        // Check link status
        val linked = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)

        if (linked[0] == 0) {
            val info = GLES20.glGetProgramInfoLog(program)
            Log.e(TAG, "Program linking failed:")
            Log.e(TAG, info)
            GLES20.glDeleteProgram(program)
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            return 0
        }

        // Clean up shaders (no longer needed after linking)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        Log.i(TAG, "Program linked successfully: $program")
        return program
    }

    /**
     * Validate shader program
     * @param program Program ID
     * @return true if valid
     */
    fun validateProgram(program: Int): Boolean {
        GLES20.glValidateProgram(program)

        val validated = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_VALIDATE_STATUS, validated, 0)

        if (validated[0] == 0) {
            val info = GLES20.glGetProgramInfoLog(program)
            Log.e(TAG, "Program validation failed:")
            Log.e(TAG, info)
            return false
        }

        Log.i(TAG, "Program validated successfully")
        return true
    }
}
