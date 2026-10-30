# Sol IR Remote

Unofficial offline Android infrared remote for the **DStv HD 8S**, designed for the **Xiaomi Mi 10T** and other Android devices with a compatible IR blaster.

## Current status

- Source code and GitHub Actions APK build workflow are present.
- **Not yet compiled or tested as a standalone APK.**
- The source IR file contains 11 commands. The user confirmed that the original extended remote file controls decoder navigation and Volume Down, and the replacement **Pronto Volume Up** command works in IR Blaster Remote.
- The Android app currently displays only **1** in the number pad because other numeric codes have not yet been merged/tested.
- Android IR transmission may behave differently from the original IR Blaster Remote app; test on the phone.

## Build APK in GitHub Actions

1. Open the **Actions** tab.
2. Select **Build Android debug APK**.
3. Choose **Run workflow** (or check the automatic run triggered by a push).
4. Open the successful run and download **SolIRRemote-debug-apk** from **Artifacts**.
5. Extract the ZIP and install `app-debug.apk` on your Android phone. Only install APKs you trust.

Build command for Android Studio or Codex: `gradle :app:assembleDebug --no-daemon`. Requires JDK 17 and Android SDK API 35.

## Project files

- `app/src/main/assets/dstv_8s_final_working.ir` — IR signals. **Do not change the working VOL_PLUS and VOL_MINUS timing arrays without testing.**
- `app/src/main/java/com/solremote/dstv/MainActivity.java` — offline Android UI and IR sender using `ConsumerIrManager`.
- `app/src/main/AndroidManifest.xml` — IR permission, no internet permission.
- `.github/workflows/build-debug-apk.yml` — automated debug APK build.
- `CODEX_PROMPT.md` — next steps for Codex.

Not affiliated with or endorsed by MultiChoice / DStv.
