# Sol’s Living Room

Unofficial offline Android infrared remotes for the **DStv HD 8S** and **LG TV**, designed for the **Xiaomi Mi 10T** and other Android devices with a compatible IR blaster.

## Current status

- Source code and GitHub Actions APK build workflow are present.
- Version 1.2.0: debug APK assembly, Android lint (0 errors, 9 warnings), APK signature verification, and all seven Python IR regression checks pass. IR transmission still needs a physical phone with an IR blaster.
- The DStv source IR file contains 26 commands: the uploaded app’s 20 commands are byte-for-byte preserved, with the six user-confirmed Power, DStv Menu, TV Guide, Options, Channel Up, and Channel Down captures added. Back / Exit reuses EXIT. The user confirmed that the original extended remote file controls decoder navigation and Volume Down, and the replacement **Pronto Volume Up** command works in IR Blaster Remote.
- The number pad displays **0–9**, with signals for every digit. Codes for 0 and 2–9 were copied from the user-supplied `dstv_8s_with_full_keypad.ir` and still need decoder testing. Version 1.0.1 replaces the malformed 33-bit code for 1 with the 32-bit waveform from that same upload, after a phone test showed the old code sending 0. This replacement still needs confirmation on the decoder. All buttons require a phone with an IR blaster.
- A separate LG TV page uses the user-supplied `lg_60la6208_tv_ir_test.ir` exactly as uploaded: 29 raw commands for the LG 60LA6208 test profile. The file is experimental and still needs confirmation on the physical television. Its signals are kept separate from the DStv map.
- Android IR transmission may behave differently from the original IR Blaster Remote app; test on the phone.

## Interface

Sol’s Living Room uses native Android controls, system fonts, and vector graphics, with no additional UI dependencies. The handset-style layout keeps Info/Exit, a circular directional pad, volume controls, and the complete numeric keypad together. It retains the charcoal and sage style, adds a warm ivory palette with brass accents and off-white keys, and includes the `zibah@2026` footer. Numeric keys draw bold digit outlines directly, rather than inheriting device Button text styling. The app opts out of automatic dark recolouring on Android 10 and newer. Both palettes retain explicit high-contrast colours and distinct key outlines. Key heights expand with the phone’s text size to keep labels visible. The appearance button offers device setting, Light, or Dark; the preference is saved locally. The layout scrolls on smaller displays. The app opens a device chooser with DStv and LG TV cards. Use the phone’s Back button to return to the chooser; LG also has a Switch remote button. The original digit renderer, appearance settings, room chooser, all 20 uploaded DStv signals, and all 29 LG signals are preserved. Both remotes have side-by-side vertical VOL and CH rockers, with + above −. DStv also has buttons for the six newly added commands and a Switch remote button. The requested reference model is AD1700; exact button placement has not yet been checked against a photograph.

## Download the current development build

[Download Sol’s Living Room APK](https://raw.githubusercontent.com/zibah04/Sol-IR-Remote/living-room-codes-rockers/downloads/Sols-Living-Room-debug.apk)

This direct link downloads the debug APK from the `living-room-codes-rockers` branch, outside the cloud preview pane. The matching SHA-256 digest is in `downloads/SHA256SUMS`. The current version is **1.2.0** (version code 6). To refresh it after a code change, run `bash scripts/package-apk.sh` and commit the updated APK and checksum together with the source. This is a development build for testing on your IR-equipped phone. Version 1.2.0 uses application ID `com.solremote.livingroom` so it installs alongside the uploaded `com.solremote.dstv` app: that APK was signed with a different certificate, which cannot be recovered from an APK. Your existing app can remain installed. The two apps have separate appearance preferences.

## Build APK in GitHub Actions

1. Open the **Actions** tab.
2. Select **Build Android debug APK**.
3. Choose **Run workflow** (or check the automatic run triggered by a push).
4. Open the successful run and download **Sols-Living-Room-debug-apk** from **Artifacts**.
5. Extract the ZIP and install `app-debug.apk` on your Android phone. Only install APKs you trust.

Build command for Android Studio or Codex: `gradle :app:assembleDebug --no-daemon`. Requires JDK 17 and Android SDK API 35.

## Project files

- `app/src/main/assets/dstv_8s_final_working.ir` — IR signals. **Do not change the working VOL_PLUS and VOL_MINUS timing arrays without testing.**
- `app/src/main/assets/lg_60la6208_tv_ir_test.ir` — unmodified user-provided LG test signals.
- `app/src/main/java/com/solremote/dstv/LgTvActivity.java` — separate LG page and signal map.
- `app/src/main/java/com/solremote/dstv/RoomHomeActivity.java` — device chooser.
- `app/src/main/java/com/solremote/dstv/MainActivity.java` — offline Android UI and IR sender using `ConsumerIrManager`.
- `app/src/main/AndroidManifest.xml` — IR permission, no internet permission.
- `.github/workflows/build-debug-apk.yml` — automated debug APK build.
- `CODEX_PROMPT.md` — next steps for Codex.

Not affiliated with or endorsed by MultiChoice / DStv.

## IR regression checks

Run `python3 -m unittest discover -s tests -v`. These check that all ten digits are present, have 32-bit frames, and encode the digit printed on the button. Packaging and CI run these checks before building. They verify signal structure, not physical decoder response.

## Native digit rendering checks

The separate Android test APK includes `DigitRenderingInstrumentation`. It renders each digit in both palettes, enabled and disabled, at normal and enlarged text sizes (80 checks), and checks glyph visibility, clipping, and accessibility state. It adds no dependencies or test code to the distributed app.

With the Android toolchain activated and an emulator connected:

```sh
gradle :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.solremote.livingroom.test/com.solremote.dstv.DigitRenderingInstrumentation
```

Require `status=passed`, `render_checks=80`, and instrumentation result code `-1`. Device-specific rendering still needs confirmation on the user's phone.

## Device navigation checks

The separate `DeviceNavigationInstrumentation` runner opens DStv → LG → DStv through the actual chooser cards, checks the LG switch button and every LG command binding, and verifies that all DStv waveforms remain unchanged after switching. Run it after installing the app and Android test APK:

```sh
gradle :app:assembleDebugAndroidTest -PtestRunner=com.solremote.dstv.DeviceNavigationInstrumentation --no-daemon
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.solremote.livingroom.test/com.solremote.dstv.DeviceNavigationInstrumentation
```

The navigation runner also checks every DStv command binding, the VOL/CH geometry on both remotes, and minimum 48dp touch targets. Pass `-e theme 1` or `-e theme 2` to test light or dark appearance. Require `status=passed`, `rocker_layout_checks=2`, `remote_open_checks=3`, `dstv_signals=26`, `lg_signals=29`, `dstv_after_switch=unchanged`, and instrumentation code `-1`.
