#!/bin/bash
cd "$(dirname "$0")"
export ANDROID_HOME="$HOME/android-sdk"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0"
export SDKMAN_DIR="$HOME/.sdkman"
[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"
./gradlew :app:clean :app:assembleDebug --no-daemon 2>&1 | tail -100 > resultado.txt
echo "=== RESULTADO ===" >> resultado.txt
ls -lh app/build/outputs/apk/debug/*.apk 2>&1 >> resultado.txt

# Instala via ADB se houver dispositivo conectado
echo "" >> resultado.txt
echo "=== INSTALAÇÃO ADB ===" >> resultado.txt
adb devices >> resultado.txt 2>&1
adb install -r app/build/outputs/apk/debug/app-debug.apk >> resultado.txt 2>&1
