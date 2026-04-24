#!/bin/bash

# Script para gerar APK do Caixa Combo
# Execute: ./build-apk.sh

set -e

echo "=========================================="
echo "   🚀 Caixa Combo - Build APK"
echo "=========================================="
echo ""

# Cores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Verificar se está no diretório correto
if [ ! -d "app" ]; then
    echo -e "${RED}❌ Erro: Execute este script no diretório do projeto CaixaCombo${NC}"
    exit 1
fi

echo -e "${YELLOW}📋 Verificando dependências...${NC}"

# Verificar Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java não instalado. Instale o JDK 17 ou superior.${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo -e "${GREEN}✓ Java encontrado: $JAVA_VERSION${NC}"

# Limpar build anterior
echo -e "${YELLOW}🧹 Limpando build anterior...${NC}"
./gradlew :app:clean

# Build do APK
echo -e "${YELLOW}🔨 Compilando projeto...${NC}"
./gradlew :app:assembleRelease

# Verificar se APK foi gerado
APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
if [ -f "$APK_PATH" ]; then
    echo -e "${GREEN}✅ APK gerado com sucesso!${NC}"
    echo ""
    echo "📍 Localização: $APK_PATH"
    echo "📦 Tamanho: $(du -h $APK_PATH | cut -f1)"
    echo ""
    echo -e "${GREEN}🎉 Build concluído!${NC}"
    echo ""
    echo "🔧 Para instalar no dispositivo:"
    echo "   adb install $APK_PATH"
    echo ""
    echo "📱 Ou transfira o arquivo para o dispositivo e instale manualmente."
else
    echo -e "${RED}❌ Falha ao gerar APK${NC}"
    exit 1
fi
