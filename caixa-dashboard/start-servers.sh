#!/bin/bash

echo "🔄 Limpando portas e iniciando servidores..."

# Matar processos nas portas 3000-3005
for port in 3000 3001 3002 3003 3004 3005; do
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo "🔪 Matar processo na porta $port"
        fuser -k $port/tcp 2>/dev/null
    fi
done

# Aguardar um pouco para as portas liberarem
sleep 2

echo "🚀 Iniciando servidores..."
cd "/home/rodrigo-dev-mt/Documentos/teste/caixa-dashboard"
npm run dev:all
