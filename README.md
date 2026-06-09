# MinimalPDF - Ultra-lightweight Android PDF Viewer

MinimalPDF is a zero-bloat, highly performant Android application for viewing PDF files natively. Written in 100% Kotlin and Jetpack Compose, it completely avoids heavy third-party SDKs in favor of Android's native hardware-accelerated `PdfRenderer`.

## Features
- **Zero-bloat**: Does not bundle massive C++ PDF parsing libraries. Fully relies on `android.graphics.pdf.PdfRenderer`.
- **True Dark Mode via ColorMatrix**: Instead of manually parsing or recoloring raw PDF pixels, Dark Mode safely applies a hardware-accelerated Matrix inversion filter via Compose, ensuring fluid 60FPS performance and zero memory overhead.
- **Fluid Gestures**: A sophisticated gesture conflict resolution algorithm handles both vertical lazy scrolling and precise pinch-to-zoom offsets entirely on a shared wrapper container.
- **Lazy Rendering**: Only renders the visual Bitmaps as the user scrolls, avoiding OutOfMemory (OOM) exceptions often found in heavy PDF documents.

## Technical Architecture
To avoid OutOfMemory exceptions and UI stuttering, MinimalPDF uses Jetpack Compose's `LazyColumn`. Individual PDF pages are extracted via `PdfRenderer` on a background coroutine dispatch only when they become visible. 

The zooming engine intelligently intercepts gestures:
- If the zoom `scale` is `1x`, `userScrollEnabled` stays `true`, yielding control to the `LazyColumn` for high-performance scrolling.
- If the user pinches to zoom (`scale > 1x`), `LazyColumn` scrolling is paused, and the unconsumed drag events power the exact translation offsets (`offsetX`, `offsetY`) bounding within the mathematically permitted viewing rectangle.

## Build Instructions
1. Clone the repository to your local machine.
2. Ensure you have the Android SDK installed and configure your `local.properties` (e.g. `sdk.dir=/home/username/Android/Sdk`).
3. Build the APK using the included Gradle wrapper:
```bash
./gradlew assembleDebug
```
4. Install to a connected device:
```bash
adb install -r -d app/build/outputs/apk/debug/app-debug.apk
```
