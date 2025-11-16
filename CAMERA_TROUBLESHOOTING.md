# Camera Troubleshooting Guide - Edge Detection Viewer

## 📋 Table of Contents

1. [Quick Diagnostic Checklist](#quick-diagnostic-checklist)
2. [Common Issues & Solutions](#common-issues--solutions)
3. [Permission Issues](#permission-issues)
4. [Hardware & Compatibility](#hardware--compatibility)
5. [Initialization Problems](#initialization-problems)
6. [Runtime Errors](#runtime-errors)
7. [Performance Issues](#performance-issues)
8. [Debugging Tools](#debugging-tools)

---

## 🔍 Quick Diagnostic Checklist

Before diving into detailed troubleshooting, run through this checklist:

```bash
# 1. Check Logcat for errors (filter by TAG)
adb logcat -s CameraController:* EdgeDetectionViewer:* AndroidRuntime:E

# 2. Verify camera permission in Settings
adb shell pm list permissions -g | grep CAMERA

# 3. Check if camera is accessible
adb shell "dumpsys media.camera"

# 4. Verify device has camera
adb shell "getprop ro.camera.count"
```

### ✅ Basic Requirements

- [ ] Android device/emulator with API 24+ (Android 7.0+)
- [ ] Physical camera hardware (rear camera)
- [ ] Camera permission granted in app settings
- [ ] TextureView properly initialized in layout
- [ ] No other apps using camera simultaneously

---

## 🚨 Common Issues & Solutions

### Issue 1: **Camera Permission Denied**

**Symptoms:**
- App crashes immediately on launch
- Logcat shows: `SecurityException: Camera permission not granted`
- Permission dialog never appears

**Root Causes:**
```kotlin
// ❌ WRONG: Missing permission check
cameraController.startCamera() // Immediate crash!

// ✅ CORRECT: Check permission first
if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
    == PackageManager.PERMISSION_GRANTED) {
    cameraController.startCamera()
}
```

**Solutions:**

#### A. Verify AndroidManifest.xml
```xml
<!-- Must have BOTH permission AND feature -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="true" />
```

#### B. Request Permission at Runtime
```kotlin
private fun checkCameraPermission() {
    when {
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED -> {
            // Permission granted - start camera
            initializeCamera()
        }
        
        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
            // Show explanation dialog
            showPermissionRationale()
        }
        
        else -> {
            // Request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }
}
```

#### C. Handle Permission Result
```kotlin
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    
    if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
        when {
            grantResults.isEmpty() -> {
                // User cancelled dialog
                Log.w(TAG, "Permission request cancelled")
            }
            
            grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                // Permission granted
                initializeCamera()
            }
            
            else -> {
                // Permission denied
                showPermissionDeniedDialog()
            }
        }
    }
}
```

#### D. Manual Permission Grant (Testing)
```bash
# Grant permission via ADB
adb shell pm grant com.edgedetection.viewer android.permission.CAMERA

# Revoke permission (for testing)
adb shell pm revoke com.edgedetection.viewer android.permission.CAMERA

# Check current permission status
adb shell dumpsys package com.edgedetection.viewer | grep CAMERA
```

---

### Issue 2: **"No Suitable Camera Found"**

**Symptoms:**
- Logcat shows: `CameraController: No suitable camera found`
- Camera never opens, app stays on blank screen
- No error dialog shown

**Root Causes:**
```kotlin
// Problem: getCameraId() returns null
private fun getCameraId(cameraManager: CameraManager): String? {
    return cameraManager.cameraIdList.find { id ->
        val characteristics = cameraManager.getCameraCharacteristics(id)
        val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
        facing == CameraCharacteristics.LENS_FACING_BACK  // Only searches for BACK camera
    }
}
```

**Solutions:**

#### A. Check Available Cameras
```kotlin
private fun debugCameraInfo(cameraManager: CameraManager) {
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
        
        Log.d(TAG, "Camera $id: facing=$facingName")
    }
}
```

#### B. Enhanced Camera Selection
```kotlin
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
        Log.d(TAG, "Using front camera (fallback): $frontCamera")
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
```

#### C. Check Device Cameras via ADB
```bash
# List all cameras
adb shell dumpsys media.camera | grep "Camera ID"

# Check camera characteristics
adb shell dumpsys media.camera | grep -A 20 "Device 0"
```

---

### Issue 3: **TextureView Not Ready**

**Symptoms:**
- `NullPointerException` when accessing `textureView.surfaceTexture`
- Logcat shows: `createCaptureSession: surfaceTexture is null`
- Camera opens but no preview visible

**Root Causes:**
```kotlin
// ❌ PROBLEM: Starting camera before TextureView is ready
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    cameraController.startCamera() // TextureView not ready yet!
}
```

**Solutions:**

#### A. Wait for Surface Available
```kotlin
init {
    textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            Log.d(TAG, "Surface available: ${width}x${height}")
            // NOW it's safe to start camera
            startCamera()
        }

        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            Log.d(TAG, "Surface size changed: ${width}x${height}")
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            Log.d(TAG, "Surface destroyed")
            closeCamera()
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // Called every frame - avoid heavy operations
        }
    }
}
```

#### B. Check Surface Availability
```kotlin
fun startCamera() {
    if (!textureView.isAvailable) {
        Log.w(TAG, "TextureView not available yet, waiting...")
        // Surface listener will trigger startCamera() when ready
        return
    }
    
    startBackgroundThread()
    openCamera()
}
```

#### C. Improved openCamera with Surface Check
```kotlin
private fun openCamera() {
    // Ensure surface is ready
    val surfaceTexture = textureView.surfaceTexture
    if (surfaceTexture == null) {
        Log.e(TAG, "SurfaceTexture not available, cannot open camera")
        return
    }
    
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    
    try {
        val cameraId = getCameraId(cameraManager) ?: run {
            Log.e(TAG, "No suitable camera found")
            showUserError("No camera available on this device")
            return
        }
        
        // ... rest of camera opening logic
    } catch (e: Exception) {
        Log.e(TAG, "Error opening camera", e)
        showUserError("Failed to open camera: ${e.message}")
    }
}
```

---

### Issue 4: **Camera Error Codes**

**Symptoms:**
- `onError()` callback triggered with error codes
- Camera fails to open despite permissions granted

**Error Code Reference:**

```kotlin
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
    
    Log.e(TAG, errorMsg)
    camera.close()
    cameraDevice = null
    
    // Notify user
    showUserError(errorMsg)
}
```

**Solutions by Error Code:**

#### ERROR_CAMERA_IN_USE (1)
```bash
# Find which app is using camera
adb shell dumpsys media.camera | grep "Client PID"

# Force stop other camera apps
adb shell am force-stop <package.name>
```

#### ERROR_MAX_CAMERAS_IN_USE (2)
- Close other apps using cameras
- Device policy limitation (some devices limit concurrent camera access)

#### ERROR_CAMERA_DISABLED (3)
```bash
# Check if camera is disabled by policy
adb shell pm list features | grep camera

# Check device restrictions
adb shell dpm list-owners
```

#### ERROR_CAMERA_DEVICE (4) / ERROR_CAMERA_SERVICE (5)
- Fatal hardware/service error
- Reboot device
- May indicate hardware failure

---

### Issue 5: **Capture Session Creation Fails**

**Symptoms:**
- `onConfigureFailed()` callback triggered
- Logcat: `Failed to configure camera`
- Camera opens but no frames received

**Root Causes:**
- Invalid surface configuration
- Resolution not supported
- Multiple incompatible surfaces

**Solutions:**

#### A. Validate Resolution
```kotlin
private fun getSupportedResolution(cameraManager: CameraManager, cameraId: String): Size {
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        ?: throw RuntimeException("Cannot get stream configuration map")
    
    // Get supported output sizes for YUV_420_888
    val sizes = map.getOutputSizes(ImageFormat.YUV_420_888)
    
    Log.d(TAG, "Supported YUV sizes:")
    sizes.forEach { size ->
        Log.d(TAG, "  ${size.width}x${size.height}")
    }
    
    // Find closest match to target (1280x720)
    val targetWidth = 1280
    val targetHeight = 720
    
    return sizes.minByOrNull { size ->
        val widthDiff = abs(size.width - targetWidth)
        val heightDiff = abs(size.height - targetHeight)
        widthDiff + heightDiff
    } ?: sizes[0] // Fallback to first size
}
```

#### B. Enhanced Session Creation with Validation
```kotlin
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
            return
        }
        
        surfaceTexture.setDefaultBufferSize(targetWidth, targetHeight)
        val previewSurface = Surface(surfaceTexture)
        
        // Validate surfaces
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
            
            // Auto white balance
            set(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_AUTO
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
                        Log.d(TAG, "Repeating request started")
                    } catch (e: CameraAccessException) {
                        Log.e(TAG, "Failed to start camera preview", e)
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Session closed before starting preview", e)
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Failed to configure camera session")
                    showUserError("Failed to configure camera session")
                }
                
                override fun onClosed(session: CameraCaptureSession) {
                    Log.d(TAG, "Capture session closed")
                }
            },
            backgroundHandler
        )

    } catch (e: CameraAccessException) {
        Log.e(TAG, "Failed to create capture session", e)
    } catch (e: IllegalArgumentException) {
        Log.e(TAG, "Invalid surface configuration", e)
    } catch (e: IllegalStateException) {
        Log.e(TAG, "Camera device closed", e)
    }
}
```

---

### Issue 6: **No Frames Received (ImageReader)**

**Symptoms:**
- Camera opens and preview visible (if TextureView used)
- `onImageAvailable` callback never triggered
- Frame callback not invoked

**Solutions:**

#### A. Debug ImageReader
```kotlin
imageReader = ImageReader.newInstance(
    targetWidth,
    targetHeight,
    ImageFormat.YUV_420_888,
    2
).apply {
    setOnImageAvailableListener({ reader ->
        Log.v(TAG, "Image available callback triggered")
        
        val image = reader.acquireLatestImage()
        if (image == null) {
            Log.w(TAG, "acquireLatestImage() returned null")
            return@setOnImageAvailableListener
        }
        
        try {
            Log.v(TAG, "Processing image: ${image.width}x${image.height}, format=${image.format}")
            processImage(image)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
        } finally {
            image.close()
        }
    }, backgroundHandler)
    
    Log.d(TAG, "ImageReader created: ${width}x${height}, format=$imageFormat")
}
```

#### B. Verify Handler Thread
```kotlin
private fun startBackgroundThread() {
    if (backgroundThread != null) {
        Log.w(TAG, "Background thread already running")
        return
    }
    
    backgroundThread = HandlerThread("CameraBackground").also { 
        it.start()
        Log.d(TAG, "Background thread started: ${it.threadId}")
    }
    backgroundHandler = Handler(backgroundThread!!.looper)
    Log.d(TAG, "Background handler created")
}
```

#### C. Check Frame Callback
```kotlin
private fun processImage(image: Image) {
    if (frameCallback == null) {
        Log.w(TAG, "Frame callback is null, skipping frame")
        return
    }
    
    try {
        val yuvBytes = imageToByteArray(image)
        Log.v(TAG, "Converted to byte array: ${yuvBytes.size} bytes")
        
        frameCallback?.invoke(yuvBytes, image.width, image.height)
    } catch (e: Exception) {
        Log.e(TAG, "Error in processImage", e)
    }
}
```

---

## 🔧 Performance Issues

### Issue: Low FPS / Dropped Frames

**Symptoms:**
- FPS counter shows <10 FPS
- Preview stutters
- High CPU usage

**Solutions:**

#### A. Reduce Resolution
```kotlin
// Instead of 1280x720, try:
private val targetWidth = 960
private val targetHeight = 540
// Or even lower for testing:
// private val targetWidth = 640
// private val targetHeight = 480
```

#### B. Skip Frames
```kotlin
private var frameCounter = 0
private val frameSkip = 2 // Process every 3rd frame

imageReader?.setOnImageAvailableListener({ reader ->
    if (frameCounter++ % frameSkip != 0) {
        reader.acquireLatestImage()?.close() // Discard frame
        return@setOnImageAvailableListener
    }
    
    val image = reader.acquireLatestImage()
    image?.let {
        processImage(it)
        it.close()
    }
}, backgroundHandler)
```

#### C. Optimize Processing
```kotlin
// Use acquireLatestImage() instead of acquireNextImage()
val image = reader.acquireLatestImage() // Skips old frames in buffer
```

---

## 🛠️ Debugging Tools

### Logcat Filtering
```bash
# Camera errors only
adb logcat -s CameraController:E EdgeDetectionViewer:E

# Verbose camera logs
adb logcat -s CameraController:V

# Clear logs and start fresh
adb logcat -c && adb logcat -s CameraController:* EdgeDetectionViewer:*
```

### Camera Service Diagnostics
```bash
# Dump camera service state
adb shell dumpsys media.camera

# Check active camera clients
adb shell dumpsys media.camera | grep -A 10 "Camera module API"

# Monitor camera events in real-time
adb shell dumpsys media.camera | grep -A 5 "Device 0"
```

### Permission Check
```bash
# List all app permissions
adb shell dumpsys package com.edgedetection.viewer | grep permission

# Grant camera permission
adb shell pm grant com.edgedetection.viewer android.permission.CAMERA
```

---

## 📱 Device-Specific Issues

### Emulator vs Real Device

**Android Emulator:**
- May not have camera support (check AVD settings)
- Virtual camera may have different characteristics
- Performance significantly worse than physical devices

**Solution:**
```bash
# Create AVD with camera
# In AVD Manager: Edit AVD → Show Advanced Settings → Camera:
#   - Front camera: Emulated / Webcam
#   - Back camera: Emulated / VirtualScene
```

### Manufacturer-Specific Problems

**Samsung Devices:**
- Some models restrict Camera2 API features
- Try legacy Camera API if Camera2 fails

**Xiaomi/MIUI:**
- Permission dialogs may be hidden/delayed
- Check MIUI security settings

**Huawei/EMUI:**
- Aggressive battery optimization may kill background threads
- Disable battery optimization for app

---

## 📋 Complete Diagnostic Log Pattern

Add this to your `CameraController` for comprehensive logging:

```kotlin
companion object {
    private const val TAG = "CameraController"
    private const val DEBUG = true // Enable verbose logging
    
    private fun log(level: String, msg: String) {
        if (!DEBUG && level == "V") return
        
        when (level) {
            "V" -> Log.v(TAG, msg)
            "D" -> Log.d(TAG, msg)
            "I" -> Log.i(TAG, msg)
            "W" -> Log.w(TAG, msg)
            "E" -> Log.e(TAG, msg)
        }
    }
}

// Usage in code:
log("D", "Starting camera initialization")
log("E", "Camera open failed: ${e.message}")
log("V", "Frame received: ${image.width}x${image.height}")
```

---

## ✅ Troubleshooting Checklist

Before reporting issues, verify:

- [ ] Permissions granted in app settings
- [ ] TextureView is visible in layout
- [ ] Device has working camera
- [ ] Android version is 7.0+ (API 24+)
- [ ] No other app using camera
- [ ] Logcat shows no SecurityException
- [ ] Background thread started successfully
- [ ] Camera ID found (not null)
- [ ] Capture session configured (no onConfigureFailed)
- [ ] ImageReader callback triggered
- [ ] Frame data not null/empty

---

## 🚀 Next Steps

If camera still doesn't work after following this guide:

1. **Collect Logs**: Run `adb logcat > camera_logs.txt` while reproducing issue
2. **Check Manifest**: Verify all required permissions and features
3. **Test on Different Device**: Rule out device-specific issues
4. **Simplify Code**: Test with minimal camera example
5. **Check Stack Overflow**: Search for specific error messages

---

## 📚 Additional Resources

- [Android Camera2 API Guide](https://developer.android.com/training/camera2)
- [Camera Permissions Best Practices](https://developer.android.com/training/permissions/requesting)
- [TextureView Documentation](https://developer.android.com/reference/android/view/TextureView)
- [Debugging Camera Issues](https://source.android.com/docs/core/camera/camera3_debugging)
