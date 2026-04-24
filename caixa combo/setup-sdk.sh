#!/bin/bash
set -e

SDK_DIR="$HOME/android-sdk"
mkdir -p "$SDK_DIR"

echo "Baixando Android SDK Command Line Tools..."
cd /tmp
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip

# Verificar hash SHA256 (Google não fornece hash oficial, mas verificamos tamanho)
EXPECTED_SIZE=1073741824  # ~1GB
ACTUAL_SIZE=$(stat -c%s commandlinetools-linux-11076708_latest.zip 2>/dev/null || echo "0")
if [ "$ACTUAL_SIZE" -lt 100000000 ]; then
    echo "❌ Erro: Download incompleto ou corrompido (tamanho: $ACTUAL_SIZE bytes)"
    rm -f commandlinetools-linux-11076708_latest.zip
    exit 1
fi

unzip -q commandlinetools-linux-11076708_latest.zip
mkdir -p "$SDK_DIR/cmdline-tools"
mv cmdline-tools "$SDK_DIR/cmdline-tools/latest"
rm commandlinetools-linux-11076708_latest.zip

export ANDROID_HOME="$SDK_DIR"
export PATH="$PATH:$SDK_DIR/cmdline-tools/latest/bin:$SDK_DIR/platform-tools"

echo "Aceitando licenças..."
yes | sdkmanager --licenses 2>&1 || true

echo "Instalando componentes..."
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "sources;android-34"

echo "SDK instalado em: $SDK_DIR"
ls -la "$SDK_DIR/"
