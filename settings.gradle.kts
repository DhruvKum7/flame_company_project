pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "EdgeDetectionViewer"
include(":app")

// OpenCV module - Adjust path as needed
include(":opencv")
project(":opencv").projectDir = File(settingsDir, "../opencv-sdk/java")
