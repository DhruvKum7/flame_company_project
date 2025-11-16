#include <jni.h>
#include <android/log.h>
#include "opencv_processor.h"

#define LOG_TAG "JNI_Bridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global processor instance
static OpenCVProcessor* g_processor = nullptr;

extern "C" {

/**
 * Initialize native processor
 * @param width Frame width
 * @param height Frame height
 * @return true if successful
 */
JNIEXPORT jboolean JNICALL
Java_com_edgedetection_viewer_FrameProcessor_nativeInit(
        JNIEnv* env, jobject /* this */, jint width, jint height) {

    LOGI("nativeInit called: %dx%d", width, height);

    // Clean up existing processor
    if (g_processor != nullptr) {
        delete g_processor;
        g_processor = nullptr;
    }

    // Create new processor
    g_processor = new OpenCVProcessor();
    if (!g_processor) {
        LOGE("Failed to create OpenCVProcessor");
        return JNI_FALSE;
    }

    // Initialize processor
    if (!g_processor->init(width, height)) {
        LOGE("Failed to initialize processor");
        delete g_processor;
        g_processor = nullptr;
        return JNI_FALSE;
    }

    LOGI("Native processor initialized successfully");
    return JNI_TRUE;
}

/**
 * Process frame with OpenCV
 * @param input YUV frame data
 * @param output RGBA output buffer
 * @param width Frame width
 * @param height Frame height
 * @param mode Processing mode (0=raw, 1=grayscale, 2=canny)
 * @return true if successful
 */
JNIEXPORT jboolean JNICALL
Java_com_edgedetection_viewer_FrameProcessor_nativeProcessFrame(
        JNIEnv* env, jobject /* this */,
        jbyteArray input, jbyteArray output,
        jint width, jint height, jint mode) {

    if (g_processor == nullptr) {
        LOGE("Processor not initialized");
        return JNI_FALSE;
    }

    // Get input data
    jbyte* inputBytes = env->GetByteArrayElements(input, nullptr);
    if (!inputBytes) {
        LOGE("Failed to get input bytes");
        return JNI_FALSE;
    }

    // Get output buffer
    jbyte* outputBytes = env->GetByteArrayElements(output, nullptr);
    if (!outputBytes) {
        LOGE("Failed to get output bytes");
        env->ReleaseByteArrayElements(input, inputBytes, JNI_ABORT);
        return JNI_FALSE;
    }

    // Get array sizes for validation
    jsize inputSize = env->GetArrayLength(input);
    jsize outputSize = env->GetArrayLength(output);

    // Validate sizes
    jsize expectedInputSize = width * height * 3 / 2; // YUV_420_888
    jsize expectedOutputSize = width * height * 4;    // RGBA

    if (inputSize < expectedInputSize) {
        LOGE("Input size mismatch: expected %d, got %d", expectedInputSize, inputSize);
        env->ReleaseByteArrayElements(input, inputBytes, JNI_ABORT);
        env->ReleaseByteArrayElements(output, outputBytes, JNI_ABORT);
        return JNI_FALSE;
    }

    if (outputSize < expectedOutputSize) {
        LOGE("Output size mismatch: expected %d, got %d", expectedOutputSize, outputSize);
        env->ReleaseByteArrayElements(input, inputBytes, JNI_ABORT);
        env->ReleaseByteArrayElements(output, outputBytes, JNI_ABORT);
        return JNI_FALSE;
    }

    // Process frame
    OpenCVProcessor::ProcessingMode processingMode =
            static_cast<OpenCVProcessor::ProcessingMode>(mode);

    bool success = g_processor->processFrame(
            reinterpret_cast<const uint8_t*>(inputBytes),
            inputSize,
            reinterpret_cast<uint8_t*>(outputBytes),
            processingMode
    );

    // Release arrays
    env->ReleaseByteArrayElements(input, inputBytes, JNI_ABORT);
    env->ReleaseByteArrayElements(output, outputBytes, 0); // 0 = copy back and free

    if (!success) {
        LOGE("Frame processing failed");
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/**
 * Set Canny edge detection thresholds
 * @param lowThreshold Low threshold (e.g., 50)
 * @param highThreshold High threshold (e.g., 150)
 */
JNIEXPORT void JNICALL
Java_com_edgedetection_viewer_FrameProcessor_nativeSetCannyThresholds(
        JNIEnv* env, jobject /* this */, jdouble lowThreshold, jdouble highThreshold) {

if (g_processor != nullptr) {
g_processor->setCannyThresholds(lowThreshold, highThreshold);
} else {
LOGE("Cannot set thresholds: processor not initialized");
}
}

/**
 * Release native resources
 */
JNIEXPORT void JNICALL
Java_com_edgedetection_viewer_FrameProcessor_nativeRelease(
        JNIEnv* env, jobject /* this */) {

LOGI("nativeRelease called");

if (g_processor != nullptr) {
g_processor->release();
delete g_processor;
g_processor = nullptr;
LOGI("Native processor released");
}
}

} // extern "C"
