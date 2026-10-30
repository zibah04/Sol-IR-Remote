#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
gradle :app:assembleDebug :app:lintDebug --no-daemon --console=plain
mkdir -p downloads
cp app/build/outputs/apk/debug/app-debug.apk downloads/Sols-Living-Room-debug.apk
(cd downloads && sha256sum Sols-Living-Room-debug.apk > SHA256SUMS)
echo 'Development APK and SHA-256 checksum prepared in downloads/'
