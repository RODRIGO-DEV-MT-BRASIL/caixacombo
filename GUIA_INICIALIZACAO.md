# 🚀 Guia de Inicialização - Caixa Combo + Dashboard

## ✅ O que foi implementado

### Backend (caixa-dashboard/server.js)
- ✅ Bug do `io` corrigido (declarado antes das rotas REST)
- ✅ Persistência de dados em arquivo JSON (`data.json`)
- ✅ Autenticação JWT completa
- ✅ CORS restrito a origens específicas
- ✅ Usuário admin padrão criado automaticamente

### Frontend (caixa-dashboard/frontend/)
- ✅ Tela de login com autenticação
- ✅ Integração JWT no WebSocket
- ✅ Logout funcional
- ✅ Proteção de rotas

### Android (caixa combo/)
- ✅ WebSocketService.kt criado
- ✅ Dependência Socket.io adicionada
- ✅ Service declarado no AndroidManifest

---

## 🔧 Configuração Inicial

### 1. Configurar o Backend

```bash
cd /home/rodrigo-dev-mt/Documentos/teste/caixa-dashboard

# Instalar dependências (incluindo jsonwebtoken e bcryptjs)
npm install

# Editar o .env com suas configurações
nano .env
```

### 2. Configurar o Frontend

```bash
cd /home/rodrigo-dev-mt/Documentos/teste/caixa-dashboard/frontend

# Instalar dependências
npm install
```

### 3. Configurar o Android

Abra o arquivo:
```
/home/rodrigo-dev-mt/Documentos/teste/caixa combo/app/src/main/java/com/seucaixa/caixacombo/service/WebSocketService.kt
```

Altere a linha 24 para o IP do seu servidor:
```kotlin
private var SOCKET_URL = "http://SEU_IP_SERVIDOR:3001"
```

**Para descobrir seu IP:**
```bash
# Linux
ip addr show | grep inet

# ou
hostname -I
```

---

## 🚀 Iniciando o Sistema

### Opção 1: Iniciar tudo junto
```bash
cd /home/rodrigo-dev-mt/Documentos/teste/caixa-dashboard
npm run dev:all
```

### Opção 2: Iniciar separadamente

**Terminal 1 - Backend:**
```bash
cd /home/rodrigo-dev-mt/Documentos/teste/caixa-dashboard
npm start
```

**Terminal 2 - Frontend:**
```bash
cd /home/rodrigo-dev-mt/Documentos/teste/caixa-dashboard/frontend
npm run dev
```

### Acessar o Dashboard
- Abra o navegador: `http://localhost:3000`
- **Usuário padrão:** `admin`
- **Senha padrão:** `admin123`

---

## 📱 Integrando com o Android

### 1. Build do APK

```bash
cd "/home/rodrigo-dev-mt/Documentos/teste/caixa combo"

# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease
```

### 2. Iniciar o WebSocketService no Android

No `MainActivity.kt` ou `CaixaApplication.kt`, adicione:

```kotlin
import android.content.Intent
import com.seucaixa.caixacombo.service.WebSocketService

// No onCreate ou onde preferir:
val serviceIntent = Intent(this, WebSocketService::class.java)

// Configurar dispositivo (opcional)
WebSocketService.setDeviceInfo(
    id = "device-001",
    name = "Caixa 01"
)

// Configurar servidor (se não quiser editar o arquivo)
WebSocketService.configureServer("http://192.168.1.100:3001")

// Iniciar serviço
startService(serviceIntent)
```

### 3. Enviar dados do Android para o Dashboard

```kotlin
import org.json.JSONObject

// Enviar venda
val sale = JSONObject().apply {
    put("id", "venda-001")
    put("total", 150.00)
    put("formaPagamento", "DINHEIRO")
    put("itens", JSONArray().apply {
        put(JSONObject().apply {
            put("produto", "Coca-Cola 2L")
            put("quantidade", 2)
            put("preco", 8.99)
        })
    })
}
webSocketService.sendSaleData(sale)

// Enviar status
webSocketService.sendDeviceStatus("online")
```

---

## 🔐 Segurança

### Alterar senha do admin

1. Faça login no dashboard
2. Use a API para alterar:
```bash
curl -X POST http://localhost:3001/api/auth/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN" \
  -d '{"username":"novo_usuario","password":"senha_segura","role":"user"}'
```

### Alterar JWT_SECRET

Edite o `.env`:
```
JWT_SECRET=sua-chave-super-secreta-muito-longa
```

### Adicionar origens CORS

Edite o `.env`:
```
CORS_ORIGINS=http://localhost:3000,http://192.168.1.100:3000,https://seu-dominio.com
```

---

## 📊 Estrutura de Dados (data.json)

```json
{
  "produtos": [
    {
      "id": 1234567890,
      "nome": "Coca-Cola 2L",
      "precoVenda": 8.99,
      "categoria": "Bebidas",
      "estoque": 50,
      "createdBy": "admin",
      "createdAt": "2024-04-23T02:00:00.000Z"
    }
  ],
  "categorias": [],
  "vendas": [],
  "operacoes": [],
  "usuarios": [
    {
      "id": 1,
      "username": "admin",
      "password": "$2a$10$...",
      "role": "admin",
      "createdAt": "2024-04-23T02:00:00.000Z"
    }
  ]
}
```

---

## 🧪 Testando a Conexão

### Teste via navegador (console do browser)

```javascript
// Conectar ao WebSocket
const socket = io('http://localhost:3001');

socket.on('connect', () => {
  console.log('Conectado!');
  
  // Simular conexão de dispositivo
  socket.emit('device_connect', {
    deviceId: 'test-001',
    deviceName: 'Dispositivo Teste',
    deviceType: 'Android'
  });
});

socket.on('device_connected', (data) => {
  console.log('Dispositivo conectado:', data);
});
```

### Teste via cURL

```bash
# Login
curl -X POST http://localhost:3001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Listar produtos (com token)
curl http://localhost:3001/api/produtos \
  -H "Authorization: Bearer SEU_TOKEN"
```

---

## 🐛 Troubleshooting

### Erro: "Token não fornecido"
- Faça login novamente
- Verifique se o token está no localStorage

### Erro: "Origem não permitida pelo CORS"
- Adicione a origem no `.env` em `CORS_ORIGINS`

### Erro: "Cannot find module 'jsonwebtoken'"
```bash
cd caixa-dashboard
npm install
```

### Android não conecta ao WebSocket
- Verifique se o IP está correto no `WebSocketService.kt`
- Verifique se o servidor está rodando
- Verifique se o firewall permite a porta 3001

### Dados somem ao reiniciar
- Verifique se o arquivo `data.json` existe
- Verifique permissões de escrita na pasta

---

## 📝 Próximos Passos

1. ✅ Sistema base funcionando
2. ⬜ Implementar HTTPS/WSS para produção
3. ⬜ Adicionar mais validações de segurança
4. ⬜ Criar backup automático do data.json
5. ⬜ Implementar sincronização bidirecional completa

---

## 📞 Suporte

- Dashboard: `http://localhost:3000`
- API: `http://localhost:3001/api`
- WebSocket: `http://localhost:3001`

**Credenciais padrão:** admin / (senha definida no .env)

> NOTA: O código de acesso do admin no Android é gerado aleatoriamente no primeiro acesso.
> Verifique o Logcat do dispositivo (tag: MainActivity) para encontrar o código gerado.
