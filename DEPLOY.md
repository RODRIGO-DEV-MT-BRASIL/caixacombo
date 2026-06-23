# ===========================================
# Guia de Deploy - CaixaCombo
# ===========================================

## Arquitetura

```
Frontend (Vercel)     →  caixacombo.vercel.app
Backend Node.js       →  Cyclic (gratuito, sem cartão)
Supabase PostgreSQL   →  supabase.com
```

## 1. Deploy Backend no Cyclic

### Passo 1: Criar conta
- Acesse https://cyclic.sh
- Crie conta com GitHub (gratuito, sem cartão)

### Passo 2: Criar projeto
1. Clique **"Create New App"**
2. Conecte seu repositório GitHub
3. Selecione a pasta: `caixa-dashboard`
4. Clique **Deploy**

### Passo 3: Configurar Environment Variables
No painel do Cyclic, va em **Variables** e adicione:

```
NODE_ENV=production
DATABASE_URL=postgres://postgres.wxxlpsvgbokdtzcjjiis:oPpfOze4FFwFv15x@aws-1-us-east-1.pooler.supabase.com:5432/postgres
JWT_SECRET=<gere_com_openssl_rand_hex_64>
CORS_ORIGINS=https://caixacombo.vercel.app
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<senha_forte>
NODEJS_API_SECRET=<gere_com_openssl_rand_hex_32>
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres.wxxlpsvgbokdtzcjjiis
SPRING_DATASOURCE_PASSWORD=oPpfOze4FFwFv15x
PAYMENT_SERVICE_URL=http://localhost:8080
SUPABASE_URL=https://wxxlpsvgbokdtzcjjiis.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Ind4eGxwc3ZnYm9rZHR6Y2pqaWlzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIxNzQ3OTEsImV4cCI6MjA5Nzc1MDc5MX0.kk_LB4CbNZSVw7NPyeXK97Yaw2p9YVl8rSxarWM9SQY
SUPABASE_SERVICE_ROLE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Ind4eGxwc3ZnYm9rZHR6Y2pqaWlzIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4MjE3NDc5MSwiZXhwIjoyMDk3NzUwNzkxfQ.rqdZBtLNBJVqz6iJovnPggdDTVi6hprtauDjq0vdj1M
SUPABASE_JWT_SECRET=cVAx7wRK/uR9qrC+e/8tDaE2aHXIMDpsOE+TmyDJB2DZXLIm/FEcW4vCZ9D6YbhfBI1bqW/yFk+SDLTbfkTzRA==
```

### Passo 4: Pegar URL do backend
Depois do deploy, o Cyclic gera uma URL como:
```
https://caixa-dashboard-server-production-xxxx.up.railway.app
```
ou
```
https://xxx.cyclic.app
```

## 2. Deploy Frontend no Vercel

### Passo 1: Conectar repositório
1. Acesse https://vercel.com
2. Importe o repositório GitHub
3. Configure:
   - **Framework**: Vite
   - **Root Directory**: `caixa-dashboard/frontend`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`

### Passo 2: Environment Variables
No painel do Vercel, va em **Settings → Environment Variables**:

```
VITE_API_URL=https://xxx.cyclic.app
```

### Passo 3: Deploy
Vercel faz deploy automático a cada push no GitHub.

## 3. Gerar Secrets

```bash
# JWT Secret
openssl rand -hex 64

# API Secret
openssl rand -hex 32
```

## 4. Verificar

1. Acesse https://caixacombo.vercel.app
2. Abra Console (F12)
3. Teste login
4. Verifique se as requisições vão para o backend correto

## 5. Estrutura

```
caixacombo/
├── .env                    # Local (vazio)
├── .env.example            # Template
├── vercel.json             # Config Vercel (frontend)
├── caixa-dashboard/
│   ├── .env                # Local (vazio)
│   ├── .env.example        # Template
│   ├── package.json        # Backend Node.js
│   ├── database.js         # Conexao PostgreSQL
│   ├── db.js               # Cache em memoria
│   ├── server.js           # API
│   └── frontend/           # React (Vercel)
└── caixa combo/            # App Android
    └── .env                # Local (vazio)
```
