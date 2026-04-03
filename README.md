# carlauncher

Custom Android launcher for the ESSGOO head unit in a Porsche Cayenne 9PA (2002–2010).
Dark theme, Porsche font, large touch targets. Home screen shows clock, date, and four app tiles. All Apps opens a full grid.

---

## Installing on the Head Unit

### 1. Build the APK

In Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**

The APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

### 2. Copy to a USB Stick

Copy `app-debug.apk` to the root of a FAT32-formatted USB stick.

---

### 3. Install from the Head Unit

1. Plug the USB stick into the head unit
2. Open the **File Manager** app on the head unit
3. Navigate to the USB drive and tap `app-debug.apk`
4. Follow the install prompts — tap **Install** when asked

---

### 4. Set as Default Launcher

1. Press the **Home button** on the head unit
2. A dialog will appear asking which launcher to use — select **carlauncher**
3. Tap **Always**

If no dialog appears, set it manually in **Settings → Apps → Default Apps → Home App** and select carlauncher.

---

### 5. Assign the CarLink and Radio Tiles

The two left tiles on the home screen are configurable:

1. **Tap** either tile (shows `+` / "Tap to set")
2. Pick the app from the list
3. **Long-press** either tile at any time to reassign it

---

## Updating the App

1. Build a new APK in Android Studio
2. Copy the new `app-debug.apk` to the USB stick
3. Install it from the File Manager — it will overwrite the existing version and keep your tile settings

---

## Troubleshooting

| Problem | Fix |
|---|---|
| App not visible after install | Press Home button — launchers don't appear in the app drawer |
| No install prompt when tapping APK | Go to Settings → Apps → Special App Access → Install Unknown Apps → File Manager → Allow |
| Launcher reverts after reboot | Settings → Apps → Default Apps → Home App → select carlauncher |
| Tiles reset after update | Long-press each tile and reassign |
| Factory settings locked | Try password `8888` or `3368` |

---

## Note on ADB

ESSGOO head units do not support ADB connections. The USB ports on the unit operate in host mode (for USB drives and CarPlay) and cannot be used for ADB. USB stick installation is the only supported method.
