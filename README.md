# Sol’s Living Room

Unofficial offline Android infrared remote for the **DStv HD 8S**, designed for the **Xiaomi Mi 10T** and other Android devices with a compatible IR blaster.

## Current status

- Source code and GitHub Actions APK build workflow are present.
- Debug APK builds and Android lint have passed in the cloud environment. The app has launched in an Android 11 emulator; IR transmission still needs a physical phone with an IR blaster.
- The source IR file contains 20 commands. The user confirmed that the original extended remote file controls decoder navigation and Volume Down, and the replacement **Pronto Volume Up** command works in IR Blaster Remote.
- The number pad displays **0–9**, with signals for every digit. Codes for 0 and 2–9 were copied from the user-supplied `dstv_8s_with_full_keypad.ir` and still need decoder testing. Version 1.0.1 replaces the malformed 33-bit code for 1 with the 32-bit waveform from that same upload, after a phone test showed the old code sending 0. This replacement still needs confirmation on the decoder. All buttons require a phone with an IR blaster.
- LG TV support is planned. No LG signal file is bundled yet; the current controls operate the DStv decoder only.
- Android IR transmission may behave differently from the original IR Blaster Remote app; test on the phone.

## Interface

Sol’s Living Room uses native Android controls, system fonts, and vector graphics, with no additional UI dependencies. The handset-style layout keeps Info/Exit, a circular directional pad, volume controls, and the complete numeric keypad together. It retains the charcoal and sage style, adds a warm ivory palette with brass accents and off-white keys, and includes the `zibah@2026` footer. Numeric keys draw bold digit outlines directly, rather than inheriting device Button text styling. The app opts out of automatic dark recolouring on Android 10 and newer. Both palettes retain explicit high-contrast colours and distinct key outlines. Key heights expand with the phone’s text size to keep labels visible. The appearance button offers device setting, Light, or Dark; the preference is saved locally. The layout scrolls on smaller displays. LG TV is marked as not added until its model and IR signals are available. The requested reference model is AD1700; exact button placement has not yet been checked against a photograph.

## Download the current development build

[Download Sol’s Living Room APK](https://raw.githubusercontent.com/zibah04/Sol-IR-Remote/sols-living-room/downloads/Sols-Living-Room-debug.apk)

This direct link downloads the debug APK from the `sols-living-room` branch, outside the cloud preview pane. The matching SHA-256 digest is in `downloads/SHA256SUMS`. The current version is **1.0.3** (version code 4). To refresh it after a code change, run `bash scripts/package-apk.sh` and commit the updated APK and checksum together with the source. This is a development build for testing on your IR-equipped phone.

## Build APK in GitHub Actions

1. Open the **Actions** tab.
2. Select **Build Android debug APK**.
3. Choose **Run workflow** (or check the automatic run triggered by a push).
4. Open the successful run and download **Sols-Living-Room-debug-apk** from **Artifacts**.
5. Extract the ZIP and install `app-debug.apk` on your Android phone. Only install APKs you trust.

Build command for Android Studio or Codex: `gradle :app:assembleDebug --no-daemon`. Requires JDK 17 and Android SDK API 35.

## Project files

- `app/src/main/assets/dstv_8s_final_working.ir` — IR signals. **Do not change the working VOL_PLUS and VOL_MINUS timing arrays without testing.**
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
adb shell am instrument -w com.solremote.dstv.test/com.solremote.dstv.DigitRenderingInstrumentation
```

Require `status=passed`, `render_checks=80`, and instrumentation result code `-1`. Device-specific rendering still needs confirmation on the user's phone.
