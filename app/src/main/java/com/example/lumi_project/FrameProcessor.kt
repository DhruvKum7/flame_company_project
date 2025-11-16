package com.edgedetection.viewer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.edgedetection.viewer.databinding.ActivityMainBinding
import com.edgedetection.viewer.gl.GLRenderer

/**
 * Main Activity for Edge Detection Viewer
 * Manages camera permissions, frame processing pipeline, and UI
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraController: CameraController
    private lateinit var frameProcessor: FrameProcessor
    private lateinit var glRenderer: GLRenderer

    private var isProcessingEnabled = true
    private var fps: Double = 0.0

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val TAG = "EdgeDetectionViewer"

        init {
            // Load native library
            System.loadLibrary("edge_detection_native")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkCameraPermission()
    }

    private fun setupUI() {
        // Toggle button for processing mode
        binding.toggleProcessing.setOnClickListener {
            isProcessingEnabled = !isProcessingEnabled
            updateToggleButton()
            frameProcessor.setProcessingMode(
                if (isProcessingEnabled) FrameProcessor.MODE_CANNY
                else FrameProcessor.MODE_RAW
            )
        }

        // FPS display
        binding.fpsText.text = "FPS: --"
    }

    private fun updateToggleButton() {
        binding.toggleProcessing.text = if (isProcessingEnabled) {
            "Processing: ON"
        } else {
            "Processing: OFF"
        }
    }

    private fun checkCameraPermission() {
        Log.d(TAG, "Checking camera permission...")

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Camera permission already granted")
                initializeCamera()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
                // Show explanation dialog
                Log.d(TAG, "Showing permission rationale")
                showPermissionRationale()
            }

            else -> {
                Log.d(TAG, "Requesting camera permission")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Required")
            .setMessage("This app needs camera access to capture and process video frames for edge detection. Please grant camera permission to continue.")
            .setPositiveButton("OK") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            when {
                grantResults.isEmpty() -> {
                    // User cancelled permission dialog
                    Log.w(TAG, "Permission request cancelled by user")
                    showPermissionDeniedDialog("Permission request was cancelled")
                }

                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Camera permission granted")
                    initializeCamera()
                }

                else -> {
                    Log.w(TAG, "Camera permission denied")
                    showPermissionDeniedDialog("Camera permission was denied")
                }
            }
        }
    }

    private fun showPermissionDeniedDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("$message. This app requires camera access to function. Please grant camera permission in Settings.")
            .setPositiveButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun initializeCamera() {
        Log.d(TAG, "Initializing camera components...")

        try {
            // Initialize OpenGL renderer
            glRenderer = GLRenderer()
            binding.glSurfaceView.setEGLContextClientVersion(2)
            binding.glSurfaceView.setRenderer(glRenderer)
            Log.d(TAG, "OpenGL renderer initialized")

            // Initialize frame processor
            frameProcessor = FrameProcessor()
            Log.d(TAG, "Frame processor initialized")

            // Initialize camera controller
            cameraController = CameraController(this, binding.textureView)

            // Set error callback to show user-friendly messages
            cameraController.setErrorCallback { errorMessage ->
                runOnUiThread {
                    showCameraError(errorMessage)
                }
            }

            cameraController.setFrameCallback { imageData, width, height ->
                processFrame(imageData, width, height)
            }
            Log.d(TAG, "Camera controller initialized")

            cameraController.startCamera()
            Log.d(TAG, "Camera start requested")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing camera", e)
            showCameraError("Failed to initialize camera: ${e.message}")
        }
    }

    private fun showCameraError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Camera Error")
            .setMessage(message)
            .setPositiveButton("Retry") { _, _ ->
                // Retry camera initialization
                if (::cameraController.isInitialized) {
                    cameraController.close()
                }
                initializeCamera()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun processFrame(imageData: ByteArray, width: Int, height: Int) {
        val startTime = System.nanoTime()

        try {
            // Process frame through JNI
            val processedData = frameProcessor.processFrame(imageData, width, height)

            if (processedData != null) {
                // Update OpenGL texture
                glRenderer.updateTexture(processedData, width, height)

                // Calculate FPS
                val endTime = System.nanoTime()
                val frameTime = (endTime - startTime) / 1_000_000.0 // ms
                fps = 1000.0 / frameTime

                // Update UI (throttle updates to every 10 frames)
                if (System.currentTimeMillis() % 10 == 0L) {
                    runOnUiThread {
                        binding.fpsText.text = String.format("FPS: %.1f", fps)
                        binding.resolutionText.text = "${width}x${height}"
                    }
                }
            } else {
                Log.w(TAG, "Frame processing returned null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame", e)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.glSurfaceView.onResume()
        if (::cameraController.isInitialized) {
            cameraController.startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.glSurfaceView.onPause()
        if (::cameraController.isInitialized) {
            cameraController.stopCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::frameProcessor.isInitialized) {
            frameProcessor.release()
        }
        if (::cameraController.isInitialized) {
            cameraController.close()
        }
    }
}
