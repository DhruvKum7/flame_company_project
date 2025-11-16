#ifndef OPENCV_PROCESSOR_H
#define OPENCV_PROCESSOR_H

#include <opencv2/opencv.hpp>
#include <vector>

/**
 * OpenCV Image Processor
 *
 * Provides high-performance image processing operations using OpenCV C++
 * Supports multiple processing modes: raw, grayscale, and Canny edge detection
 */
class OpenCVProcessor {
public:
    enum ProcessingMode {
        MODE_RAW = 0,        // Pass-through, no processing
        MODE_GRAYSCALE = 1,  // Grayscale conversion
        MODE_CANNY = 2       // Canny edge detection
    };

    OpenCVProcessor();
    ~OpenCVProcessor();

    /**
     * Initialize processor with frame dimensions
     * @param width Frame width in pixels
     * @param height Frame height in pixels
     * @return true if initialization successful
     */
    bool init(int width, int height);

    /**
     * Process YUV frame data
     * @param yuvData Input YUV_420_888 data
     * @param yuvSize Size of YUV data
     * @param outputRgba Output RGBA buffer (must be pre-allocated: width * height * 4)
     * @param mode Processing mode (RAW, GRAYSCALE, CANNY)
     * @return true if processing successful
     */
    bool processFrame(const uint8_t* yuvData, size_t yuvSize,
                      uint8_t* outputRgba, ProcessingMode mode);

    /**
     * Set Canny edge detection thresholds
     * @param low Low threshold (default: 50)
     * @param high High threshold (default: 150)
     */
    void setCannyThresholds(double low, double high);

    /**
     * Release resources
     */
    void release();

private:
    int frameWidth;
    int frameHeight;
    double cannyLowThreshold;
    double cannyHighThreshold;

    // OpenCV matrices (reused for performance)
    cv::Mat yuvMat;
    cv::Mat rgbaMat;
    cv::Mat grayMat;
    cv::Mat edgesMat;
    cv::Mat tempMat;

    bool initialized;

    // Convert YUV_420_888 to RGBA
    void yuvToRgba(const uint8_t* yuvData, cv::Mat& output);

    // Apply grayscale filter
    void applyGrayscale(const cv::Mat& input, cv::Mat& output);

    // Apply Canny edge detection
    void applyCanny(const cv::Mat& input, cv::Mat& output);
};

#endif // OPENCV_PROCESSOR_H
