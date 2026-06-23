# Deploy - CaixaCombo (Render)

## Arquitetura

```
Frontend (Render Static)  →  https://caixa-combo-frontend.onrender.com
Backend (Render Web)      →  https://caixa-combo-api.onrender.com
Supabase PostgreSQL       →  https://supabase.com
```

## 1. Backend (já criado)

Service: `caixa-combo-api`
URL: https://caixa-combo-api.onrender.com

## 2. Frontend (criar agora)

1. Render Dashboard → **New +** → **Static Site**
2. Conectar repositório GitHub
3. Configurar:
   - **Name:** `caixa-combo-frontend`
   - **Build Command:** `cd caixa-dashboard/frontend && npm install && npm run build`
   - **Publish Directory:** `caixa-dashboard/frontend/dist`
4. Environment Variables:
   ```
   VITE_API_URL=https://caixa-combo-api.onrender.com
   ```
5. **Create Static Site**

## 3. Atualizar CORS

No service `caixa-combo-api`, adicionar:
```
CORS_ORIGINS=https://caixa-combo-api.onrender.com,https://caixa-combo-frontend.onrender.com
```
