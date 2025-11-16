cmake_minimum_required(VERSION 3.22.1)

project("edge_detection_native")

# Set C++ standard
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Enable verbose output for debugging
set(CMAKE_VERBOSE_MAKEFILE ON)

# OpenCV configuration
set(OpenCV_DIR "${CMAKE_SOURCE_DIR}/../../../opencv-sdk/native/jni")

message(STATUS "======================================")
message(STATUS "Edge Detection Native Build Configuration")
message(STATUS "======================================")
message(STATUS "CMake Version: ${CMAKE_VERSION}")
message(STATUS "Android ABI: ${ANDROID_ABI}")
message(STATUS "Android Platform: ${ANDROID_PLATFORM}")
message(STATUS "OpenCV_DIR: ${OpenCV_DIR}")
message(STATUS "CMAKE_SOURCE_DIR: ${CMAKE_SOURCE_DIR}")

# Find OpenCV
find_package(OpenCV REQUIRED)

if(OpenCV_FOUND)
    message(STATUS "✓ OpenCV Found Successfully!")
    message(STATUS "  OpenCV Version: ${OpenCV_VERSION}")
    message(STATUS "  OpenCV Libraries: ${OpenCV_LIBS}")
    message(STATUS "  OpenCV Include Dirs: ${OpenCV_INCLUDE_DIRS}")
else()
    message(FATAL_ERROR "✗ OpenCV NOT FOUND! Please check:")
    message(FATAL_ERROR "  1. OpenCV SDK downloaded from https://opencv.org/releases/")
    message(FATAL_ERROR "  2. Extracted to: android-app/opencv-sdk/")
    message(FATAL_ERROR "  3. File exists: ${OpenCV_DIR}/OpenCVConfig.cmake")
endif()

message(STATUS "======================================")

# Include directories
include_directories(${OpenCV_INCLUDE_DIRS})

# Source files
add_library(
        edge_detection_native
        SHARED
        opencv_processor.cpp
        jni_bridge.cpp
)

# Link libraries
target_link_libraries(
        edge_detection_native
        ${OpenCV_LIBS}
        GLESv2
        EGL
        log
        android
        jnigraphics
)

# Compiler flags
target_compile_options(
        edge_detection_native
        PRIVATE
        -Wall
        -Wextra
        -O3
        -ffast-math
        -fvisibility=hidden
)

# Post-build information
message(STATUS "")
message(STATUS "Native library configuration complete!")
message(STATUS "Target: edge_detection_native.so")
message(STATUS "")
