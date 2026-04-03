# ESSGOO Car Head Unit Launcher

## Project Overview

A custom Android launcher APK for an ESSGOO aftermarket car head unit running Android 10 (AOSP). The launcher replaces the stock manufacturer home screen with a fully customizable, lightweight alternative tailored for in-car use.

## Target Hardware

- **Device:** ESSGOO head unit for Porsche Cayenne 9PA (2002–2010)
- **OS:** Android 13 (AOSP — includes Android Auto / Apple CarPlay / Mirror Link)
- **CPU:** ARM Cortex-A53 1.3 GHz × 4 (quad-core)
- **RAM:** 2 GB DDR
- **Storage:** 32 GB eMMC
- **Screen:** 9-inch capacitive touch, **1280 × 720**, landscape
- **Audio:** 4 × 45 W output
- **Connectivity:** WiFi, Bluetooth, FM 87.5–108 MHz, GPS (internal antenna)
- **Input:** Touchscreen, steering wheel control interface, physical buttons

## Goals

- Replace the stock ESSGOO launcher with a clean, fast, fully custom home screen
- Display installed apps in a touch-friendly grid
- Launch apps on tap
- Handle the Home button correctly (launcher must be sticky — no escaping to the stock UI)
- Optionally hook into head-unit-specific intents (radio, navigation, backup camera)
- Stay lightweight — no heavy frameworks, minimal dependencies

## Technical Requirements

- **Target SDK:** API 33 (Android 13)
- **Min SDK:** API 26
- **Single Activity** architecture preferred for simplicity and performance
- **Landscape-only** layout (the unit is fixed landscape in the dash)
- **Screen:** 1280 × 720, ~163 dpi — design and test at this resolution
- Large touch targets — aim for 100dp+ cells; driver is glancing, not aiming
- Sideloaded via ADB or USB stick (no Play Store on this unit)

## Project Structure

```
essgoo-launcher/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml       # HOME intent filter declared here
│   │   ├── java/com/essgoo/launcher/
│   │   │   ├── MainActivity.kt       # Main launcher activity
│   │   │   ├── AppGridAdapter.kt     # RecyclerView adapter for app icons
│   │   │   └── AppInfo.kt            # Data class for installed app info
│   │   └── res/
│   │       ├── layout/
│   │       │   ├── activity_main.xml
│   │       │   └── item_app.xml
│   │       └── values/
│   │           ├── colors.xml
│   │           └── dimens.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── CLAUDE.md
```

## Key Implementation Details

### Launcher Declaration (AndroidManifest.xml)
The activity must declare HOME and DEFAULT categories to appear as a launcher option:
```xml
<activity android:name=".MainActivity">
  <intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.HOME" />
    <category android:name="android.intent.category.DEFAULT" />
  </intent-filter>
</activity>
```

### App Discovery
Use `PackageManager.getInstalledApplications()` filtered by `Intent.ACTION_MAIN` + `CATEGORY_LAUNCHER` to get the launchable app list.

### App Launch
```kotlin
val intent = packageManager.getLaunchIntentForPackage(packageName)
startActivity(intent)
```

### Head Unit Hard Button Intents
The stock ESSGOO firmware maps physical buttons (Radio, Nav, etc.) to proprietary broadcast intents. These can be sniffed with:
```bash
adb shell dumpsys activity | grep -i intent
adb shell getevent   # to watch raw key events
```
Paste results into Claude Code to generate intent handlers.

## Development Workflow

### Virtual Device Setup (for local testing in Android Studio)
Create a custom AVD that matches the head unit's exact screen:

1. **Tools → Device Manager → + → Create Virtual Device**
2. Click **New Hardware Profile** (bottom-left)
3. Set:
   - Name: `ESSGOO 9in 1280x720`
   - Screen size: `9.0` inches
   - Resolution: `1280 × 720` px
   - Orientation: **Landscape**
   - Has Hardware Buttons: ✓ (Back/Home/Menu)
4. **Finish** → select the new profile → click **Next**
5. System image: choose **API 33** (Android 13, x86_64) — download if needed
6. On the last screen, set **Startup orientation** to **Landscape** → **Finish**

To run: select the `ESSGOO 9in 1280x720` AVD in the device dropdown, then click **Run 'app'** (▶).

### Enable ADB on the Head Unit (for deploying to the real unit)
1. Settings → About → tap **Build Number** 7 times
2. Settings → Developer Options → enable **USB Debugging**
3. Connect via USB: `adb devices`
4. Or connect over Wi-Fi: `adb connect <head_unit_ip>:5555`
   - Find the IP at Settings → WiFi → tap the connected network

### Build & Install to Head Unit
```bash
# ADB is at: ~/Library/Android/sdk/platform-tools/adb
# Add to PATH permanently: echo 'export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"' >> ~/.zshrc

./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Set as Default Launcher
After install, press the Home button on the unit → select your launcher → tap **Always**.

### Useful ADB Debug Commands
```bash
adb shell dumpsys activity                   # inspect running activities
adb shell logcat | grep -i launcher          # filter logs
adb shell pm list packages                   # list installed packages
adb shell getevent -l                        # watch hardware key events
```

## Coding Standards

- **Language:** Kotlin
- **UI:** XML layouts (no Compose — keeps it compatible with API 21+)
- **No third-party dependencies** unless absolutely necessary
- Keep APK size small — target under 2MB
- No animations heavier than simple alpha fades (performance on low-end hardware)
- Comments on anything head-unit-specific or non-obvious

## Known Constraints & Gotchas

- The stock ESSGOO launcher may reassert itself after reboot on some firmware versions — may need ADB to set default persistently: `adb shell cmd package set-home-activity org.devines.carlauncher/.MainActivity`
- Backup camera, AC controls, and volume overlay are handled by the MCU layer — not accessible from a normal launcher without root or factory-mode APKs
- Play Store is absent; all APKs must be sideloaded
- Some ESSGOO units lock the default launcher in firmware — factory password (`8888` or `3368`) may be needed to unlock settings

## Reference Resources

- [XDA Android Head Units forum](https://xdaforums.com/f/android-head-units.11/)
- [BricksOpenLauncher (open-source reference)](https://github.com/dimskiy/BricksOpenLauncher)
- [Mini Car Launcher (open-source reference)](https://github.com/jamal2362/Mini-Car-Launcher)
- ESSGOO Download Center: https://essgoo.com/blogs/download-center

## Current Status

Initial scaffold complete. Full Gradle project, AndroidManifest, and working MainActivity exist — displays installed apps in a 1280×720 landscape grid and launches them on tap. Next: test on the custom AVD, then sideload to the Porsche Cayenne head unit via ADB.
