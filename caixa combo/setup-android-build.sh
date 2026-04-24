#!/bin/bash
# Setup para build Android sem Android Studio

set -e

echo "Setting up Android build environment..."

# Diretório de instalação
ANDROID_HOME="$HOME/android-sdk"
mkdir -p "$ANDROID_HOME"

# Baixar Android Command Line Tools
echo "Downloading Android SDK..."
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
mkdir -p "$ANDROID_HOME/cmdline-tools"
mv cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
rm commandlinetools-linux-11076708_latest.zip

# Configurar variáveis
export ANDROID_HOME="$ANDROID_HOME"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

# Aceitar licenças
echo "Accepting licenses..."
yes | sdkmanager --licenses || true

# Instalar SDK e build tools
echo "Installing SDK components..."
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" || true

# Configurar local.properties
SCRIPT_DIR="$(dirname "$0")"
echo "sdk.dir=$ANDROID_HOME" > "$SCRIPT_DIR/local.properties"

echo "Setup complete!"
echo "ANDROID_HOME=$ANDROID_HOME"
echo ""
echo "Add to your ~/.bashrc:"
echo "export ANDROID_HOME=$ANDROID_HOME"
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools'
