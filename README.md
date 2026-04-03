# carlauncher

Minimal dark-theme Android launcher for aftermarket car head units. Clean home screen with clock, date, and four configurable app tiles. All Apps opens a full grid.

Built for a 9" 1280×720 Android 13 head unit in a Porsche Cayenne 9PA (2002–2010), but should work on any landscape Android head unit running API 26+.

---

## Installing on the Head Unit

### 1. Build the APK

In Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**

The APK will be at:
```
app/build/outputs/apk/debug/carlauncher.apk
```

---

### 2. Copy to a USB Stick

Copy `carlauncher.apk` to the root of a FAT32-formatted USB stick.

---

### 3. Install from the Head Unit

1. Plug the USB stick into the head unit
2. Open the **File Manager** app on the head unit
3. Navigate to the USB drive and tap `carlauncher.apk`
4. Follow the install prompts — tap **Install** when asked

---

### 4. Set as Default Launcher

1. Press the **Home button** on the head unit
2. A dialog will appear asking which launcher to use — select **carlauncher**
3. Tap **Always**

If no dialog appears, set it manually in **Settings → Apps → Default Apps → Home App** and select carlauncher.

---

### 5. Assign App Tiles

The three left tiles on the home screen are configurable:

1. **Tap** any tile (shows `+` / "Tap to set")
2. Pick the app from the list
3. **Long-press** any tile at any time to reassign it

---

## Updating the App

1. Build a new APK in Android Studio — it outputs as `carlauncher.apk` automatically
2. Copy `carlauncher.apk` to the root of a USB stick
3. Plug the USB stick into the head unit
4. Open **All Apps** — the update button in the top-right will turn green if `carlauncher.apk` is detected
5. Tap the update button and select **Install**

---

## Troubleshooting

| Problem | Fix |
|---|---|
| App not visible after install | Press Home button — launchers don't appear in the app drawer |
| No install prompt when tapping APK | Go to Settings → Apps → Special App Access → Install Unknown Apps → File Manager → Allow |
| Launcher reverts after reboot | Settings → Apps → Default Apps → Home App → select carlauncher |
| Tiles reset after update | Long-press each tile and reassign |
| Update button says no APK found | Make sure the file is named exactly `carlauncher.apk` and is at the root of the USB stick |
| Factory settings locked | Try password `8888` or `3368` |

---

## Note on ADB

Many aftermarket head units do not support ADB over USB — the USB ports operate in host mode (for USB drives and CarPlay) rather than device mode. The ESSGOO unit this was built on falls into this category. USB stick installation is the recommended method for units where ADB is unavailable.
