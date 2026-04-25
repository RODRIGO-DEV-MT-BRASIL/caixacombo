# CaixaCombo Dashboard Web

Dashboard em tempo real para monitoramento e controle dos dispositivos Android CaixaCombo.

## ✅ O que foi implementado

- **Autenticação JWT** - Login seguro com tokens
- **Persistência de dados** - Dados salvos em `data.json`
- **CORS restrito** - Apenas origens autorizadas
- **Bug do io corrigido** - Socket.io declarado antes das rotas REST
- **Tela de login** - Interface de autenticação no frontend

## 🏗️ Arquitetura

```
Dispositivo Android → WebSocket Server (JWT) → Dashboard Web (Login)
```

## 📋 Requisitos

- Node.js 18+
- npm ou yarn
- Android Studio (para integração com app Android)

## 🚀 Instalação Rápida

```bash
# Backend
cd caixa-dashboard
npm install
npm start

# Frontend (outro terminal)
cd frontend
npm install
npm run dev
```

**Acesse:** `http://localhost:3000`  
**Login padrão:** `admin` / `admin123`

## 📱 Integração Android

### 1. WebSocketService.kt já criado
Localização: `app/src/main/java/com/seucaixa/caixacombo/service/WebSocketService.kt`

### 2. Configurar IP do servidor
Altere a linha 24 do `WebSocketService.kt`:
```kotlin
private var SOCKET_URL = "http://SEU_IP_SERVIDOR:3001"
```

### 3. Iniciar o Service
No `MainActivity.kt` ou `CaixaApplication.kt`:
```kotlin
val serviceIntent = Intent(this, WebSocketService::class.java)
WebSocketService.setDeviceInfo("device-001", "Caixa 01")
startService(serviceIntent)
```

## 🔌 WebSocket Events

### Dispositivo → Server
- `device_connect`: Dispositivo se conecta (com token opcional)
- `device_status`: Status do dispositivo
- `sale_data`: Dados de venda
- `caixa_data`: Dados do caixa

### Server → Dispositivo
- `execute_command`: Comando para executar
- `request_data`: Solicitar dados
- `request_sync`: Solicitar sincronização

### Dashboard → Server
- `dashboard_connect`: Dashboard se conecta (com token)
- `command_device`: Enviar comando para dispositivo
- `request_device_data`: Solicitar dados de dispositivo

### Server → Dashboard
- `device_connected`: Dispositivo conectado
- `device_disconnected`: Dispositivo desconectado
- `device_status_update`: Atualização de status
- `devices_list`: Lista de dispositivos
- `sale_update`: Atualização de venda
- `caixa_update`: Atualização do caixa
- `command_error`: Erro no comando
- `auth_error`: Erro de autenticação

## 🔒 Segurança

### Autenticação JWT
- Login com usuário/senha
- Token JWT válido por 24h
- Token armazenado no localStorage

### CORS Restrito
- Configure origens permitidas no `.env`:
```
CORS_ORIGINS=http://localhost:3000,http://192.168.1.100:3000
```

### Alterar JWT_SECRET
Edite o `.env`:
```
JWT_SECRET=sua-chave-super-secreta-muito-longa
```

## 📊 Funcionalidades

### Dashboard Web
- ✅ Login com autenticação
- ✅ Monitoramento em tempo real
- ✅ Status online/offline
- ✅ Visualização de dados do caixa
- ✅ Controle remoto de dispositivos
- ✅ Atualizações de vendas em tempo real
- ✅ Logout funcional

### Comandos Remotos
- **atualizar**: Solicitar dados atualizados
- **reiniciar_app**: Reiniciar o app
- **sincronizar**: Forçar sincronização

## 🔧 Configuração (.env)

```env
PORT=3000
HOST=0.0.0.0
FRONTEND_URL=http://localhost:3000
CORS_ORIGINS=http://localhost:3000
JWT_SECRET=caixacombo-secret-key-change-in-production
SERVER_IP=localhost
```

## � Estrutura de Arquivos

```
caixa-dashboard/
├── server.js          # Backend com JWT e persistência
├── data.json          # Dados persistentes
├── .env               # Configurações
├── package.json       # Dependências (jsonwebtoken, bcryptjs)
└── frontend/
    ├── src/
    │   ├── App.jsx        # App com autenticação
    │   ├── pages/
    │   │   ├── Login.jsx  # Tela de login
    │   │   └── Dashboard.jsx
    │   ├── hooks/
    │   │   └── useDevices.js
    │   └── lib/
    │       └── socket.js   # Socket com JWT
    └── package.json
```

## 📝 Notas

- O app Android funciona normalmente sem o dashboard
- O WebSocketService é opcional
- Dados são persistidos automaticamente em `data.json`
- Para produção, use HTTPS/WSS

## 📖 Guia Completo

Veja o arquivo `GUIA_INICIALIZACAO.md` na pasta raiz para instruções detalhadas.
