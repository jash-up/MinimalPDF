# MinimalPDF - Ultra-lightweight Android PDF Viewer

cuz i wanted an ad free psf software

Ps. I designed the app and developed part of it. Most of the actual code is from gemini. 
I havent tested it as much either, built it over a day, so if there is any issue please lemme know or send a pull request ig
And if there are any features youd like, you can lemme know.

## Features
- Doesnt have bloat
- Dark mode switch(like a color switcher thingi which just inverts all colors)
- Normal scrolling and zooming gestures
- Page select and page numbers(took a surprising amount of effort, like half an hour, so im including it here, broke like twice when i tried to change the pdf renderer)
- Lazy rendering(just render the part which yoi are reading cuz i read textbooks and they be chonky)

## Other things
Uses Jetpack Compose's LazyColumn and PdfRenderer


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
