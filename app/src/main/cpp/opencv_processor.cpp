#include "opencv_processor.h"
#include <android/log.h>

#define LOG_TAG "OpenCVProcessor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

OpenCVProcessor::OpenCVProcessor()
        : frameWidth(0)
        , frameHeight(0)
        , cannyLowThreshold(50.0)
        , cannyHighThreshold(150.0)
        , initialized(false) {
    LOGI("OpenCVProcessor created");
}

OpenCVProcessor::~OpenCVProcessor() {
    release();
}

bool OpenCVProcessor::init(int width, int height) {
    if (width <= 0 || height <= 0) {
        LOGE("Invalid dimensions: %dx%d", width, height);
        return false;
    }

    frameWidth = width;
    frameHeight = height;

    // Pre-allocate matrices
    yuvMat = cv::Mat(height + height / 2, width, CV_8UC1);
    rgbaMat = cv::Mat(height, width, CV_8UC4);
    grayMat = cv::Mat(height, width, CV_8UC1);
    edgesMat = cv::Mat(height, width, CV_8UC1);
    tempMat = cv::Mat(height, width, CV_8UC4);

    initialized = true;
    LOGI("Initialized with dimensions: %dx%d", width, height);
    return true;
}

bool OpenCVProcessor::processFrame(const uint8_t* yuvData, size_t yuvSize,
                                   uint8_t* outputRgba, ProcessingMode mode) {
    if (!initialized) {
        LOGE("Processor not initialized");
        return false;
    }

    if (!yuvData || !outputRgba) {
        LOGE("Null input/output pointers");
        return false;
    }

    try {
        // Convert YUV to RGBA
        yuvToRgba(yuvData, rgbaMat);

        // Apply processing based on mode
        switch (mode) {
            case MODE_RAW:
                // Pass-through - just copy RGBA data
                memcpy(outputRgba, rgbaMat.data, frameWidth * frameHeight * 4);
                break;

            case MODE_GRAYSCALE:
                applyGrayscale(rgbaMat, tempMat);
                memcpy(outputRgba, tempMat.data, frameWidth * frameHeight * 4);
                break;

            case MODE_CANNY:
                applyCanny(rgbaMat, tempMat);
                memcpy(outputRgba, tempMat.data, frameWidth * frameHeight * 4);
                break;

            default:
                LOGE("Unknown processing mode: %d", mode);
                return false;
        }

        return true;

    } catch (const cv::Exception& e) {
        LOGE("OpenCV exception: %s", e.what());
        return false;
    }
}

void OpenCVProcessor::yuvToRgba(const uint8_t* yuvData, cv::Mat& output) {
    // Copy YUV data to matrix
    memcpy(yuvMat.data, yuvData, frameWidth * frameHeight * 3 / 2);

    // Convert YUV_NV21 to RGBA
    cv::cvtColor(yuvMat, output, cv::COLOR_YUV2RGBA_NV21);
}

void OpenCVProcessor::applyGrayscale(const cv::Mat& input, cv::Mat& output) {
    // Convert to grayscale
    cv::cvtColor(input, grayMat, cv::COLOR_RGBA2GRAY);

    // Convert back to RGBA for rendering
    cv::cvtColor(grayMat, output, cv::COLOR_GRAY2RGBA);
}

void OpenCVProcessor::applyCanny(const cv::Mat& input, cv::Mat& output) {
    // Convert to grayscale first
    cv::cvtColor(input, grayMat, cv::COLOR_RGBA2GRAY);

    // Apply Gaussian blur to reduce noise
    cv::GaussianBlur(grayMat, grayMat, cv::Size(5, 5), 1.5);

    // Apply Canny edge detection
    cv::Canny(grayMat, edgesMat, cannyLowThreshold, cannyHighThreshold, 3);

    // Convert edges to RGBA (edges are white on black background)
    cv::cvtColor(edgesMat, output, cv::COLOR_GRAY2RGBA);
}

void OpenCVProcessor::setCannyThresholds(double low, double high) {
    cannyLowThreshold = low;
    cannyHighThreshold = high;
    LOGI("Canny thresholds updated: %.1f / %.1f", low, high);
}

void OpenCVProcessor::release() {
    if (initialized) {
        yuvMat.release();
        rgbaMat.release();
        grayMat.release();
        edgesMat.release();
        tempMat.release();
        initialized = false;
        LOGI("Resources released");
    }
}
