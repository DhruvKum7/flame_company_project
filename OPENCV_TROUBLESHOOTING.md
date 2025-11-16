# OpenCV Integration Troubleshooting Guide

## Common OpenCV Issues & Solutions

This guide helps diagnose and fix OpenCV integration problems in the Edge Detection Viewer project.

---

## 📋 Quick Diagnosis Checklist

Run through this checklist to identify your issue:

- [ ] **OpenCV SDK downloaded?** → See [Section 1](#1-opencv-sdk-setup)
- [ ] **Correct SDK location?** → See [Section 2](#2-verify-opencv-path)
- [ ] **Gradle sync fails?** → See [Section 3](#3-gradle-configuration-issues)
- [ ] **CMake build fails?** → See [Section 4](#4-cmake-build-issues)
- [ ] **Runtime crashes?** → See [Section 5](#5-runtime-errors)
- [ ] **Native library not found?** → See [Section 6](#6-native-library-loading)

---

## 1. OpenCV SDK Setup

### 1.1 Download OpenCV Android SDK

**Required Version:** OpenCV 4.8.0+ (recommended: 4.9.0)

```bash
# Download from official website
https://opencv.org/releases/

# Or use direct link (4.9.0):
https://github.com/opencv/opencv/releases/download/4.9.0/opencv-4.9.0-android-sdk.zip
```

### 1.2 Extract and Place SDK

**Correct directory structure:**

```
your-project/
├── android-app/
│   ├── app/
│   ├── opencv-sdk/          ← Extract here
│   │   ├── native/
│   │   │   └── jni/
│   │   │       ├── abi-armeabi-v7a/
│   │   │       ├── abi-arm64-v8a/
│   │   │       ├── abi-x86/
│   │   │       ├── abi-x86_64/
│   │   │       └── OpenCVConfig.cmake
│   │   └── java/
│   │       ├── src/
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   └── settings.gradle.kts
```

**Command to extract:**
```bash
cd your-project/
unzip opencv-4.9.0-android-sdk.zip
mv OpenCV-android-sdk android-app/opencv-sdk
```

### 1.3 Verify SDK Structure

Run this command from `android-app/` directory:

```bash
# Check if OpenCV native libraries exist
ls opencv-sdk/native/jni/abi-arm64-v8a/

# Expected output:
# libopencv_core.a
# libopencv_imgproc.a
# libopencv_java4.so
# ...

# Check Java module
ls opencv-sdk/java/

# Expected output:
# src/  res/  AndroidManifest.xml
```

---

## 2. Verify OpenCV Path

### 2.1 Path Configuration Check

Your project uses **relative paths**. Verify paths match your structure:

**In `settings.gradle.kts`:**
```kotlin
project(":opencv").projectDir = File(settingsDir, "../opencv-sdk/java")
//                                                   ^^^^^^^^^^^^^^
//                                                   Relative to android-app/
```

**In `build.gradle.kts`:**
```kotlin
"-DOpenCV_DIR=\${project.rootDir}/../opencv-sdk/native/jni"
//                                  ^^^^^^^^^^^^
//                                  Relative to android-app/app/
```

**In `CMakeLists.txt`:**
```cmake
set(OpenCV_DIR "${CMAKE_SOURCE_DIR}/../../../opencv-sdk/native/jni")
#                                   ^^^^^^^^^^^
#                                   Relative to android-app/app/src/main/cpp/
```

### 2.2 Fix Path Issues

If OpenCV is in a different location, update **all three files**:

**Example: OpenCV in project root**
```
your-project/
├── opencv-sdk/          ← SDK here instead
└── android-app/
```

Update paths:

**settings.gradle.kts:**
```kotlin
project(":opencv").projectDir = File(settingsDir, "../../opencv-sdk/java")
```

**build.gradle.kts:**
```kotlin
"-DOpenCV_DIR=\${project.rootDir}/../../opencv-sdk/native/jni"
```

**CMakeLists.txt:**
```cmake
set(OpenCV_DIR "${CMAKE_SOURCE_DIR}/../../../../opencv-sdk/native/jni")
```

### 2.3 Use Absolute Paths (Alternative)

If relative paths are problematic, use absolute paths:

**CMakeLists.txt:**
```cmake
# Replace with your actual path
set(OpenCV_DIR "/Users/yourname/Projects/edge-detection/opencv-sdk/native/jni")
```

---

## 3. Gradle Configuration Issues

### 3.1 Error: "OpenCV module not found"

**Symptom:**
```
Could not determine the dependencies of task ':app:compileDebugJavaWithJavac'.
> Could not resolve project :opencv.
```

**Solution:**

1. Verify `settings.gradle.kts` includes OpenCV:
```kotlin
include(":opencv")
project(":opencv").projectDir = File(settingsDir, "../opencv-sdk/java")
```

2. Check OpenCV `build.gradle` exists:
```bash
ls opencv-sdk/java/build.gradle
```

3. If missing, create `opencv-sdk/java/build.gradle`:
```groovy
plugins {
    id 'com.android.library'
}

android {
    namespace 'org.opencv'
    compileSdk 34

    defaultConfig {
        minSdk 24
        targetSdk 34
    }

    buildTypes {
        release {
            minifyEnabled false
        }
    }
}
```

### 3.2 Error: "NDK not configured"

**Symptom:**
```
No version of NDK matched the requested version
```

**Solution:**

Install NDK via Android Studio:
1. **Tools** → **SDK Manager**
2. **SDK Tools** tab
3. Check **NDK (Side by side)** → version 25.1.8937393+
4. Apply changes

Or specify NDK version in `build.gradle.kts`:
```kotlin
android {
    ndkVersion = "25.1.8937393"
}
```

### 3.3 Error: "Duplicate classes" or "Multiple dex files"

**Symptom:**
```
Duplicate class org.opencv.core.Core found in modules
```

**Solution:**

Check you're not importing OpenCV multiple ways. Use **only one** method:

**Method 1: Project dependency (Recommended)**
```kotlin
dependencies {
    implementation(project(":opencv"))
}
```

**Method 2: AAR file**
```kotlin
dependencies {
    implementation(fileTree(mapOf("dir" to "../opencv-sdk/java", "include" to listOf("*.aar"))))
}
```

---

## 4. CMake Build Issues

### 4.1 Error: "Could NOT find OpenCV"

**Symptom:**
```
CMake Error at CMakeLists.txt:11 (find_package):
  Could not find a package configuration file provided by "OpenCV"
```

**Solution:**

1. Verify `OpenCVConfig.cmake` exists:
```bash
ls android-app/opencv-sdk/native/jni/OpenCVConfig.cmake
```

2. If missing, you have incorrect SDK version. **Re-download** from opencv.org

3. Check CMake can find the path:
```cmake
message(STATUS "OpenCV_DIR: ${OpenCV_DIR}")
message(STATUS "OpenCV_FOUND: ${OpenCV_FOUND}")
message(STATUS "OpenCV_LIBS: ${OpenCV_LIBS}")
```

### 4.2 Error: "undefined reference to cv::..."

**Symptom:**
```
undefined reference to `cv::Canny(cv::_InputArray const&, ...)`
```

**Solution:**

1. Ensure OpenCV libraries are linked:
```cmake
target_link_libraries(
    edge_detection_native
    ${OpenCV_LIBS}  # ← Must be present
    log
)
```

2. Check ABI match. Only build for ABIs you need:
```kotlin
ndk {
    abiFilters += listOf("arm64-v8a", "armeabi-v7a")  // Remove x86 if not needed
}
```

### 4.3 Error: "cannot find -lopencv_core"

**Symptom:**
```
ld: error: cannot find -lopencv_core
```

**Solution:**

1. Verify native libraries exist for your target ABI:
```bash
ls opencv-sdk/native/jni/abi-arm64-v8a/libopencv_*.a
```

2. Ensure OpenCV version matches. Delete `build/` and rebuild:
```bash
cd android-app
./gradlew clean
./gradlew assembleDebug
```

### 4.4 Debugging CMake

Add verbose logging to `CMakeLists.txt`:

```cmake
# Add after find_package(OpenCV REQUIRED)
message(STATUS "===== OpenCV Configuration =====")
message(STATUS "OpenCV_DIR: ${OpenCV_DIR}")
message(STATUS "OpenCV_FOUND: ${OpenCV_FOUND}")
message(STATUS "OpenCV_VERSION: ${OpenCV_VERSION}")
message(STATUS "OpenCV_LIBS: ${OpenCV_LIBS}")
message(STATUS "OpenCV_INCLUDE_DIRS: ${OpenCV_INCLUDE_DIRS}")
message(STATUS "================================")
```

Check output in **Build** window in Android Studio.

---

## 5. Runtime Errors

### 5.1 Error: "UnsatisfiedLinkError"

**Symptom:**
```
java.lang.UnsatisfiedLinkError: dlopen failed: library "libopencv_java4.so" not found
```

**Solution:**

1. Ensure OpenCV library is packaged in APK:

**Check APK contents:**
```bash
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep libopencv
```

Should show:
```
lib/arm64-v8a/libopencv_java4.so
lib/armeabi-v7a/libopencv_java4.so
```

2. Load OpenCV before using native library:

**In MainActivity.kt:**
```kotlin
companion object {
    init {
        System.loadLibrary("opencv_java4")  // ← Must load first
        System.loadLibrary("edge_detection_native")
    }
}
```

### 5.2 Error: "No implementation found for native method"

**Symptom:**
```
java.lang.UnsatisfiedLinkError: No implementation found for boolean 
com.edgedetection.viewer.FrameProcessor.nativeInit(int, int)
```

**Solution:**

1. Verify JNI method signatures match exactly:

**Kotlin declaration:**
```kotlin
external fun nativeInit(width: Int, height: Int): Boolean
```

**C++ implementation:**
```cpp
extern "C" JNIEXPORT jboolean JNICALL
Java_com_edgedetection_viewer_FrameProcessor_nativeInit(
    JNIEnv* env, jobject thiz, jint width, jint height)
```

2. Check package name matches:
- Java package: `com.edgedetection.viewer`
- JNI prefix: `Java_com_edgedetection_viewer_`

3. Rebuild native libraries:
```bash
./gradlew clean
./gradlew assembleDebug
```

### 5.3 Error: "SIGSEGV" (Segmentation Fault)

**Symptom:**
```
Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR)
```

**Solution:**

Common causes in OpenCV code:

1. **Null pointer access:**
```cpp
// BAD
cv::Mat mat;
mat.data[0] = 255;  // Crash! Mat not initialized

// GOOD
cv::Mat mat(height, width, CV_8UC4);
mat.data[0] = 255;
```

2. **Invalid array access:**
```cpp
// BAD
memcpy(output, rgbaMat.data, width * height * 4);  // Size mismatch?

// GOOD
size_t dataSize = width * height * 4;
if (rgbaMat.total() * rgbaMat.elemSize() == dataSize) {
    memcpy(output, rgbaMat.data, dataSize);
}
```

3. **YUV buffer size mismatch:**
```cpp
// For YUV_420_888: size = width * height * 3 / 2
if (yuvSize != frameWidth * frameHeight * 3 / 2) {
    LOGE("Invalid YUV size: expected %d, got %zu", 
         frameWidth * frameHeight * 3 / 2, yuvSize);
    return false;
}
```

---

## 6. Native Library Loading

### 6.1 Check Library Load Order

**Correct order in MainActivity.kt:**

```kotlin
companion object {
    init {
        // 1. Load OpenCV first
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed")
        }
        
        // OR use static loading:
        System.loadLibrary("opencv_java4")
        
        // 2. Then load your native library
        System.loadLibrary("edge_detection_native")
    }
}
```

### 6.2 Use OpenCVLoader (Recommended)

Add to `onCreate()` in MainActivity:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    if (!OpenCVLoader.initDebug()) {
        Log.e(TAG, "OpenCV initialization failed!")
        Toast.makeText(this, "OpenCV Init Failed", Toast.LENGTH_LONG).show()
        finish()
        return
    }
    
    Log.i(TAG, "OpenCV loaded successfully: ${Core.VERSION}")
    
    // ... rest of initialization
}
```

### 6.3 Verify Library Architecture

Ensure device ABI matches built libraries:

```kotlin
// Check device ABI
val supportedAbis = Build.SUPPORTED_ABIS
Log.d(TAG, "Device ABIs: ${supportedAbis.joinToString()}")

// Check loaded library
val libPath = System.getProperty("java.library.path")
Log.d(TAG, "Library path: $libPath")
```

---

## 7. Build Configuration Fixes

### 7.1 Optimized build.gradle.kts

```kotlin
android {
    // ... other configs
    
    defaultConfig {
        // Build only for ABIs you need (speeds up build)
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        
        externalNativeBuild {
            cmake {
                // C++ flags
                cppFlags += listOf("-std=c++14", "-frtti", "-fexceptions")
                
                // CMake arguments
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_PLATFORM=android-24",
                    "-DOpenCV_DIR=\${project.rootDir}/../opencv-sdk/native/jni"
                )
                
                // Optional: Speed up builds
                // arguments += "-DCMAKE_BUILD_TYPE=Release"
            }
        }
    }
    
    // Packaging options (prevent conflicts)
    packagingOptions {
        jniLibs {
            pickFirsts += listOf("lib/*/libopencv_java4.so")
        }
    }
}
```

### 7.2 Optimized CMakeLists.txt

```cmake
cmake_minimum_required(VERSION 3.22.1)
project("edge_detection_native")

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Verbose output for debugging
set(CMAKE_VERBOSE_MAKEFILE ON)

# OpenCV configuration
set(OpenCV_DIR "${CMAKE_SOURCE_DIR}/../../../opencv-sdk/native/jni")
find_package(OpenCV REQUIRED)

# Debug: Print OpenCV info
message(STATUS "OpenCV_VERSION: ${OpenCV_VERSION}")
message(STATUS "OpenCV_LIBS: ${OpenCV_LIBS}")

# Include directories
include_directories(${OpenCV_INCLUDE_DIRS})

# Source files
add_library(edge_detection_native SHARED
    opencv_processor.cpp
    jni_bridge.cpp
)

# Link libraries
target_link_libraries(edge_detection_native
    ${OpenCV_LIBS}
    GLESv2
    EGL
    log
    android
    jnigraphics
)

# Optimization flags
target_compile_options(edge_detection_native PRIVATE
    -Wall
    -Wextra
    -O3
    -ffast-math
    -fvisibility=hidden
)
```

---

## 8. Testing & Verification

### 8.1 Minimal Test Code

Add to `jni_bridge.cpp` for testing:

```cpp
extern "C" JNIEXPORT jstring JNICALL
Java_com_edgedetection_viewer_FrameProcessor_testOpenCV(JNIEnv* env, jobject) {
    std::string version = cv::getVersionString();
    LOGI("OpenCV version: %s", version.c_str());
    return env->NewStringUTF(version.c_str());
}
```

In FrameProcessor.kt:
```kotlin
external fun testOpenCV(): String
```

In MainActivity:
```kotlin
val opencvVersion = FrameProcessor.testOpenCV()
Log.d(TAG, "OpenCV is working! Version: $opencvVersion")
```

### 8.2 Gradle Commands

```bash
# Clean build
./gradlew clean

# Build with verbose CMake output
./gradlew assembleDebug --info

# Check APK contents
./gradlew assembleDebug
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep lib

# Check native build logs
cat app/.cxx/Debug/*/build_output.txt
```

---

## 9. Alternative: Maven/AAR Method

If project module approach fails, use AAR directly:

### 9.1 Convert OpenCV to AAR

```bash
cd opencv-sdk/java
./gradlew assembleRelease
cp build/outputs/aar/opencv-release.aar ../../app/libs/
```

### 9.2 Update build.gradle.kts

```kotlin
dependencies {
    // Remove: implementation(project(":opencv"))
    
    // Add:
    implementation(fileTree(mapOf(
        "dir" to "libs",
        "include" to listOf("*.aar")
    )))
}
```

### 9.3 Remove from settings.gradle.kts

```kotlin
// Comment out or remove:
// include(":opencv")
// project(":opencv").projectDir = ...
```

---

## 10. Quick Fix Checklist

If nothing works, try this step-by-step reset:

```bash
# 1. Delete all build artifacts
cd android-app
rm -rf .gradle/
rm -rf app/build/
rm -rf app/.cxx/
rm -rf build/

# 2. Verify OpenCV SDK exists
ls opencv-sdk/native/jni/OpenCVConfig.cmake
ls opencv-sdk/java/src/

# 3. Clean Gradle cache
./gradlew clean --refresh-dependencies

# 4. Sync Gradle
# File → Sync Project with Gradle Files in Android Studio

# 5. Rebuild project
./gradlew assembleDebug --stacktrace --info
```

---

## 11. Getting Help

If you're still stuck, gather this information:

1. **OpenCV version:**
```bash
cat opencv-sdk/native/jni/OpenCVConfig.cmake | grep VERSION
```

2. **Android Studio version:**
- Help → About Android Studio

3. **NDK version:**
```bash
cat app/build.gradle.kts | grep ndkVersion
```

4. **Build error (full stacktrace):**
```bash
./gradlew assembleDebug --stacktrace > build_error.txt 2>&1
```

5. **CMake output:**
```bash
cat app/.cxx/Debug/*/build_output.txt
```

---

## 12. Success Indicators

Your OpenCV integration is working when you see:

✅ **Gradle sync completes without errors**
✅ **CMake finds OpenCV**: "OpenCV_VERSION: 4.x.x" in build log
✅ **Native library loads**: No UnsatisfiedLinkError at runtime
✅ **Test method works**: `testOpenCV()` returns version string
✅ **Frame processing works**: Camera frames are processed and displayed

---

## Additional Resources

- **OpenCV Android Docs**: https://docs.opencv.org/4.x/d5/df8/tutorial_dev_with_OCV_on_Android.html
- **NDK Documentation**: https://developer.android.com/ndk/guides
- **CMake Android Guide**: https://developer.android.com/ndk/guides/cmake

---

**Still having issues? Check the project README.md Section 8 (Troubleshooting) for more solutions!**
