#!/bin/bash
# Script final para build do Caixa Combo

set -e

echo "=========================================="
echo "   🚀 CAIXA COMBO - BUILD FINAL"
echo "=========================================="
echo ""

# Configurar ambiente
export SDKMAN_DIR="$HOME/.sdkman"
[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"

export ANDROID_HOME="$HOME/android-sdk"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

cd "$(dirname "$0")"

echo "✓ Java: $(java -version 2>&1 | head -1)"
echo "✓ Gradle: $(gradle --version 2>&1 | head -1)"
echo "✓ Android SDK: $ANDROID_HOME"
echo ""

# Limpar build anterior
echo "🧹 Limpando build anterior..."
rm -rf app/build .gradle build

# Build
echo "🔨 Compilando APK..."
echo "   (Isso pode levar 2-5 minutos na primeira vez)"
echo ""

if gradle assembleDebug --info 2>&1 | tee build.log | grep -E "BUILD|FAIL|error:" | tail -20; then
    echo ""
    echo "✅ BUILD CONCLUÍDO!"
    echo ""
else
    echo ""
    echo "❌ BUILD FALHOU"
    echo "Verifique o arquivo build.log para detalhes"
    exit 1
fi

# Verificar APK
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo "📱 APK gerado:"
    echo "   Local: $APK_PATH"
    echo "   Tamanho: $(du -h $APK_PATH | cut -f1)"
    echo ""
    echo "🎉 Pronto para instalar!"
    echo ""
    echo "Comando para instalar:"
    echo "adb install -r $APK_PATH"
    echo ""
else
    echo "❌ APK não encontrado"
    exit 1
fi
