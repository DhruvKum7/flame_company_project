package com.edgedetection.viewer

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import java.nio.ByteBuffer

/**
 * CameraController - Manages Camera2 API for frame capture
 *
 * Responsibilities:
 * - Open/close camera
 * - Configure capture session
 * - Provide YUV frames via callback
 */
class CameraController(
    private val context: Context,
    private val textureView: TextureView
) {
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var frameCallback: ((ByteArray, Int, Int) -> Unit)? = null

    // Target resolution (adjust based on performance needs)
    private val targetWidth = 1280
    private val targetHeight = 720

    companion object {
        private const val TAG = "CameraController"
        private const val DEBUG = true // Enable verbose logging
    }

    // Error callback for user notifications
    private var errorCallback: ((String) -> Unit)? = null

    fun setErrorCallback(callback: (String) -> Unit) {
        errorCallback = callback
    }

    init {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                // Surface ready - camera initialization happens in startCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    fun setFrameCallback(callback: (ByteArray, Int, Int) -> Unit) {
        frameCallback = callback
    }

    fun startCamera() {
        startBackgroundThread()
        openCamera()
    }

    fun stopCamera() {
        closeCamera()
        stopBackgroundThread()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    private fun openCamera() {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Debug: List all available cameras
        if (DEBUG) {
            Log.d(TAG, "Available cameras: ${cameraManager.cameraIdList.size}")
            cameraManager.cameraIdList.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val facingName = when (facing) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
                    CameraCharacteristics.LENS_FACING_BACK -> "BACK"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
                    else -> "UNKNOWN"
                }
                Log.d(TAG, "  Camera $id: facing=$facingName")
            }
        }

        try {
            val cameraId = getCameraId(cameraManager) ?: run {
                val errorMsg = "No suitable camera found on this device"
                Log.e(TAG, errorMsg)
                errorCallback?.invoke(errorMsg)
                return
            }

            Log.d(TAG, "Selected camera ID: $cameraId")

            // Setup ImageReader for frame capture
            imageReader = ImageReader.newInstance(
                targetWidth,
                targetHeight,
                ImageFormat.YUV_420_888,
                2
            ).apply {
                setOnImageAvailableListener({ reader ->
                    if (DEBUG) Log.v(TAG, "Image available callback triggered")

                    val image = reader.acquireLatestImage()
                    if (image == null) {
                        Log.w(TAG, "acquireLatestImage() returned null")
                        return@setOnImageAvailableListener
                    }

                    try {
                        if (DEBUG) Log.v(TAG, "Processing image: ${image.width}x${image.height}")
                        processImage(image)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing image", e)
                    } finally {
                        image.close()
                    }
                }, backgroundHandler)
            }

            Log.d(TAG, "ImageReader created: ${targetWidth}x${targetHeight}")

            Log.d(TAG, "Opening camera...")
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d(TAG, "Camera opened successfully: ${camera.id}")
                    cameraDevice = camera
                    createCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "Camera disconnected: ${camera.id}")
                    camera.close()
                    cameraDevice = null
                    errorCallback?.invoke("Camera disconnected")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    val errorMsg = when (error) {
                        CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> {
                            "Camera is in use by another app"
                        }
                        CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> {
                            "Maximum number of cameras in use"
                        }
                        CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> {
                            "Camera disabled by device policy"
                        }
                        CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> {
                            "Fatal camera device error"
                        }
                        CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> {
                            "Fatal camera service error"
                        }
                        else -> "Unknown camera error: $error"
                    }

                    Log.e(TAG, "Camera error: $errorMsg")
                    camera.close()
                    cameraDevice = null
                    errorCallback?.invoke(errorMsg)
                }
            }, backgroundHandler)

        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission not granted", e)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Cannot access camera", e)
        }
    }

    private fun getCameraId(cameraManager: CameraManager): String? {
        // Try to find back camera first
        val backCamera = cameraManager.cameraIdList.find { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            facing == CameraCharacteristics.LENS_FACING_BACK
        }

        if (backCamera != null) {
            Log.d(TAG, "Using back camera: $backCamera")
            return backCamera
        }

        // Fallback: use front camera
        val frontCamera = cameraManager.cameraIdList.find { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            facing == CameraCharacteristics.LENS_FACING_FRONT
        }

        if (frontCamera != null) {
            Log.w(TAG, "Using front camera (fallback): $frontCamera")
            return frontCamera
        }

        // Last resort: use first available camera
        val firstCamera = cameraManager.cameraIdList.firstOrNull()
        if (firstCamera != null) {
            Log.w(TAG, "Using first available camera: $firstCamera")
            return firstCamera
        }

        Log.e(TAG, "No cameras found on device!")
        return null
    }

    private fun createCaptureSession() {
        val camera = cameraDevice ?: run {
            Log.e(TAG, "Camera device is null")
            return
        }

        val reader = imageReader ?: run {
            Log.e(TAG, "ImageReader is null")
            return
        }

        try {
            val surfaceTexture = textureView.surfaceTexture ?: run {
                Log.e(TAG, "SurfaceTexture is null")
                errorCallback?.invoke("Camera preview not ready")
                return
            }

            surfaceTexture.setDefaultBufferSize(targetWidth, targetHeight)
            val previewSurface = Surface(surfaceTexture)

            // Validate surface
            if (!previewSurface.isValid) {
                Log.e(TAG, "Preview surface is not valid")
                return
            }

            Log.d(TAG, "Creating capture session with surfaces:")
            Log.d(TAG, "  - Preview: ${targetWidth}x${targetHeight}")
            Log.d(TAG, "  - ImageReader: ${reader.width}x${reader.height}")

            val captureRequestBuilder = camera.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            ).apply {
                addTarget(previewSurface)
                addTarget(reader.surface)

                // Auto-focus
                set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )

                // Auto-exposure
                set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
            }

            camera.createCaptureSession(
                listOf(previewSurface, reader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d(TAG, "Capture session configured successfully")
                        captureSession = session
                        try {
                            session.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                backgroundHandler
                            )
                            Log.d(TAG, "Repeating request started - camera should be active")
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "Failed to start camera preview", e)
                            errorCallback?.invoke("Failed to start camera preview")
                        } catch (e: IllegalStateException) {
                            Log.e(TAG, "Session closed before starting preview", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        val errorMsg = "Failed to configure camera session"
                        Log.e(TAG, errorMsg)
                        errorCallback?.invoke(errorMsg)
                    }

                    override fun onClosed(session: CameraCaptureSession) {
                        Log.d(TAG, "Capture session closed")
                    }
                },
                backgroundHandler
            )

        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create capture session", e)
            errorCallback?.invoke("Failed to create capture session: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid surface configuration", e)
            errorCallback?.invoke("Invalid camera configuration")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Camera device closed", e)
            errorCallback?.invoke("Camera device closed unexpectedly")
        }
    }

    private fun processImage(image: Image) {
        if (frameCallback == null) {
            Log.w(TAG, "Frame callback is null, skipping frame")
            return
        }

        try {
            // Convert YUV_420_888 to byte array
            val yuvBytes = imageToByteArray(image)

            if (DEBUG) Log.v(TAG, "Converted to byte array: ${yuvBytes.size} bytes")

            frameCallback?.invoke(yuvBytes, image.width, image.height)
        } catch (e: Exception) {
            Log.e(TAG, "Error in processImage", e)
        }
    }

    private fun imageToByteArray(image: Image): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copy Y plane
        yBuffer.get(nv21, 0, ySize)

        // Copy UV planes
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }

    fun closeCamera() {
        captureSession?.close()
        captureSession = null

        cameraDevice?.close()
        cameraDevice = null

        imageReader?.close()
        imageReader = null
    }

    fun close() {
        stopCamera()
    }
}
