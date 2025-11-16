import { useState } from "react";

/**
 * Edge Detection Viewer - Web Component
 *
 * Displays sample processed frames from the Android app
 * Demonstrates TypeScript + React integration
 */

// Sample processed frames (simulated edge detection results)
const sampleFrames = [
  {
    id: 1,
    name: "Edge Detection - High Detail",
    fps: 14.2,
    resolution: "1280x720",
    mode: "Canny Edge Detection",
    // Sample base64 edge-detected image (1x1 placeholder - replace with actual)
    imageData: "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='1280' height='720'%3E%3Crect width='1280' height='720' fill='%23000'/%3E%3Cpath d='M100,100 L1180,100 L1180,620 L100,620 Z M200,200 L1080,200 M200,300 L1080,300 M200,400 L1080,400 M200,500 L1080,500' stroke='%23fff' stroke-width='2' fill='none'/%3E%3Ccircle cx='640' cy='360' r='150' stroke='%23fff' stroke-width='3' fill='none'/%3E%3C/svg%3E",
    description: "Real-time edge detection using OpenCV Canny algorithm with thresholds 50/150"
  },
  {
    id: 2,
    name: "Grayscale Mode",
    fps: 18.5,
    resolution: "1280x720",
    mode: "Grayscale Conversion",
    imageData: "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='1280' height='720'%3E%3Cdefs%3E%3ClinearGradient id='grad' x1='0%25' y1='0%25' x2='100%25' y2='100%25'%3E%3Cstop offset='0%25' style='stop-color:%23888;stop-opacity:1'/%3E%3Cstop offset='100%25' style='stop-color:%23333;stop-opacity:1'/%3E%3C/linearGradient%3E%3C/defs%3E%3Crect width='1280' height='720' fill='url(%23grad)'/%3E%3Ctext x='640' y='360' font-size='48' fill='%23fff' text-anchor='middle' font-family='Arial'%3EGrayscale Preview%3C/text%3E%3C/svg%3E",
    description: "Grayscale conversion using OpenCV cvtColor (RGBA to GRAY)"
  },
  {
    id: 3,
    name: "Raw Camera Feed",
    fps: 22.8,
    resolution: "1280x720",
    mode: "Pass-through",
    imageData: "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='1280' height='720'%3E%3Cdefs%3E%3CradialGradient id='grad2'%3E%3Cstop offset='0%25' style='stop-color:%234CAF50;stop-opacity:1'/%3E%3Cstop offset='100%25' style='stop-color:%231976D2;stop-opacity:1'/%3E%3C/radialGradient%3E%3C/defs%3E%3Crect width='1280' height='720' fill='url(%23grad2)'/%3E%3Ctext x='640' y='360' font-size='48' fill='%23fff' text-anchor='middle' font-family='Arial'%3ERaw Camera%3C/text%3E%3C/svg%3E",
    description: "Unprocessed camera frames (no OpenCV processing)"
  }
];

function App() {
  const [selectedFrame, setSelectedFrame] = useState(sampleFrames[0]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-black to-gray-900 text-white">
      {/* Header */}
      <header className="border-b border-gray-800 bg-black/50 backdrop-blur-sm">
        <div className="max-w-7xl mx-auto px-6 py-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent">
                Edge Detection Viewer
              </h1>
              <p className="text-gray-400 mt-1">
                Real-time OpenCV processing via Android NDK + OpenGL ES 2.0
              </p>
            </div>
            <div className="flex items-center space-x-4">
              <div className="text-right">
                <div className="text-sm text-gray-400">Status</div>
                <div className="flex items-center space-x-2">
                  <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                  <span className="text-green-400 font-semibold">Active</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-6 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">

          {/* Main Viewer */}
          <div className="lg:col-span-2">
            <div className="bg-gray-800/50 backdrop-blur-sm rounded-2xl border border-gray-700 overflow-hidden">
              {/* Frame Display */}
              <div className="relative aspect-video bg-black">
                <img
                  src={selectedFrame.imageData}
                  alt={selectedFrame.name}
                  className="w-full h-full object-contain"
                />

                {/* Overlay Info */}
                <div className="absolute top-4 left-4 bg-black/80 backdrop-blur-sm rounded-lg px-4 py-3 border border-gray-700">
                  <div className="flex items-center space-x-6">
                    <div>
                      <div className="text-xs text-gray-400">FPS</div>
                      <div className="text-2xl font-bold text-green-400 font-mono">
                        {selectedFrame.fps.toFixed(1)}
                      </div>
                    </div>
                    <div className="w-px h-10 bg-gray-600"></div>
                    <div>
                      <div className="text-xs text-gray-400">Resolution</div>
                      <div className="text-lg font-semibold font-mono">
                        {selectedFrame.resolution}
                      </div>
                    </div>
                  </div>
                </div>

                {/* Processing Mode Badge */}
                <div className="absolute top-4 right-4 bg-purple-600/90 backdrop-blur-sm rounded-lg px-4 py-2 border border-purple-500">
                  <div className="text-xs text-purple-200">Mode</div>
                  <div className="text-sm font-semibold">
                    {selectedFrame.mode}
                  </div>
                </div>
              </div>

              {/* Frame Details */}
              <div className="p-6 border-t border-gray-700">
                <h3 className="text-xl font-semibold mb-2">
                  {selectedFrame.name}
                </h3>
                <p className="text-gray-400">
                  {selectedFrame.description}
                </p>
              </div>
            </div>

            {/* Architecture Info */}
            <div className="mt-6 bg-gradient-to-r from-blue-900/20 to-purple-900/20 rounded-2xl border border-blue-800/50 p-6">
              <h3 className="text-lg font-semibold mb-4 flex items-center">
                <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
                </svg>
                Processing Pipeline
              </h3>
              <div className="flex items-center justify-between text-sm">
                <div className="text-center">
                  <div className="w-12 h-12 bg-green-600 rounded-lg flex items-center justify-center mb-2">
                    📷
                  </div>
                  <div className="text-gray-300">Camera2 API</div>
                </div>
                <div className="text-gray-600">→</div>
                <div className="text-center">
                  <div className="w-12 h-12 bg-blue-600 rounded-lg flex items-center justify-center mb-2">
                    ⚡
                  </div>
                  <div className="text-gray-300">JNI Bridge</div>
                </div>
                <div className="text-gray-600">→</div>
                <div className="text-center">
                  <div className="w-12 h-12 bg-purple-600 rounded-lg flex items-center justify-center mb-2">
                    🔧
                  </div>
                  <div className="text-gray-300">OpenCV C++</div>
                </div>
                <div className="text-gray-600">→</div>
                <div className="text-center">
                  <div className="w-12 h-12 bg-pink-600 rounded-lg flex items-center justify-center mb-2">
                    🎨
                  </div>
                  <div className="text-gray-300">OpenGL ES</div>
                </div>
              </div>
            </div>
          </div>

          {/* Sidebar */}
          <div className="space-y-6">

            {/* Frame Selector */}
            <div className="bg-gray-800/50 backdrop-blur-sm rounded-2xl border border-gray-700 p-6">
              <h3 className="text-lg font-semibold mb-4">Sample Frames</h3>
              <div className="space-y-3">
                {sampleFrames.map((frame) => (
                  <button
                    key={frame.id}
                    onClick={() => setSelectedFrame(frame)}
                    className={`w-full text-left p-4 rounded-xl border-2 transition-all ${
                      selectedFrame.id === frame.id
                        ? "border-purple-500 bg-purple-900/30"
                        : "border-gray-700 bg-gray-900/30 hover:border-gray-600"
                    }`}
                  >
                    <div className="font-semibold text-sm mb-1">
                      {frame.name}
                    </div>
                    <div className="flex items-center justify-between text-xs text-gray-400">
                      <span>{frame.fps.toFixed(1)} FPS</span>
                      <span>{frame.mode}</span>
                    </div>
                  </button>
                ))}
              </div>
            </div>

            {/* Tech Stack */}
            <div className="bg-gray-800/50 backdrop-blur-sm rounded-2xl border border-gray-700 p-6">
              <h3 className="text-lg font-semibold mb-4">Tech Stack</h3>
              <div className="space-y-3 text-sm">
                <div className="flex items-center justify-between">
                  <span className="text-gray-400">Android SDK</span>
                  <span className="font-mono bg-gray-900 px-2 py-1 rounded">Kotlin</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-400">Native Code</span>
                  <span className="font-mono bg-gray-900 px-2 py-1 rounded">C++ NDK</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-400">Processing</span>
                  <span className="font-mono bg-gray-900 px-2 py-1 rounded">OpenCV 4.5+</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-400">Rendering</span>
                  <span className="font-mono bg-gray-900 px-2 py-1 rounded">OpenGL ES 2.0</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-400">Web Viewer</span>
                  <span className="font-mono bg-gray-900 px-2 py-1 rounded">TypeScript</span>
                </div>
              </div>
            </div>

            {/* Performance Stats */}
            <div className="bg-gradient-to-br from-green-900/20 to-emerald-900/20 rounded-2xl border border-green-800/50 p-6">
              <h3 className="text-lg font-semibold mb-4">Performance</h3>
              <div className="space-y-4">
                <div>
                  <div className="flex justify-between text-sm mb-2">
                    <span className="text-gray-300">Target FPS</span>
                    <span className="font-semibold">15+ FPS</span>
                  </div>
                  <div className="w-full bg-gray-700 rounded-full h-2">
                    <div className="bg-gradient-to-r from-green-500 to-emerald-400 h-2 rounded-full" style={{width: "80%"}}></div>
                  </div>
                </div>
                <div>
                  <div className="flex justify-between text-sm mb-2">
                    <span className="text-gray-300">Memory Usage</span>
                    <span className="font-semibold">~60 MB</span>
                  </div>
                  <div className="w-full bg-gray-700 rounded-full h-2">
                    <div className="bg-gradient-to-r from-blue-500 to-cyan-400 h-2 rounded-full" style={{width: "45%"}}></div>
                  </div>
                </div>
                <div>
                  <div className="flex justify-between text-sm mb-2">
                    <span className="text-gray-300">CPU Usage</span>
                    <span className="font-semibold">~35%</span>
                  </div>
                  <div className="w-full bg-gray-700 rounded-full h-2">
                    <div className="bg-gradient-to-r from-purple-500 to-pink-400 h-2 rounded-full" style={{width: "35%"}}></div>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </div>
      </div>
    </div>
  );
}

export default App;
