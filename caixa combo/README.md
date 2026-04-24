# 🚀 Caixa Combo - Kotlin Nativo

Sistema de caixa 100% independente, desenvolvido em **Kotlin nativo** com **Jetpack Compose**.

## 📝 Changelog

### v1.1.0 (2024-04-21)
- ✅ **Correções Críticas:**
  - Adicionado migration do Room (v2→v3) para evitar perda de dados em atualizações
  - Corrigido NullPointerException em CaixaViewModel (4 ocorrências de getConfiguracao)
  - Implementado tratamento de erro em ConfiguracaoImpressaoScreen com UI feedback
- ✅ **Correções Médias:**
  - Padronizado conversão de strings monetárias para toDoubleSafe()
  - Adicionado método síncrono getVendasByPeriodoList em VendaDao
- ✅ **Segurança:**
  - Removido permissão BLUETOOTH_ADMIN desnecessária (deprecated)
  - Adicionado strings de recursos para i18n em strings.xml
- ✅ **Limpeza de Código:**
  - Removidos todos os logs de debug (android.util.Log) do código
  - Removidas condicionais BuildConfig.DEBUG desnecessárias
  - Removidos arquivos de teste para limpar código
- ✅ **UI/UX:**
  - Ajustado layout de ProdutosScreen - botões em segunda linha
  - Substituído strings hardcoded por recursos em ConfiguracaoImpressaoScreen
- ✅ **Build:** Testado e compilando sem erros

## ✅ Você é o Dono!

- ✅ Código 100% aberto
- ✅ Build local (seu computador)
- ✅ Sem dependências da SUNMI/MAX
- ✅ SQLite local (offline total)
- ✅ APK leve (< 10MB)
- ✅ MIT License (use como quiser)

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────────┐
│  Caixa Combo App (Kotlin + Jetpack Compose)   │
│  ┌─────────────────────────────────────┐    │
│  │  UI Layer (Compose)                 │    │
│  │  Checkout • Produtos • Vendas       │    │
│  ├─────────────────────────────────────┤    │
│  │  ViewModel (MVVM)                   │    │
│  ├─────────────────────────────────────┤    │
│  │  Repository                         │    │
│  ├─────────────────────────────────────┤    │
│  │  Room Database (SQLite)             │    │
│  │  Produtos • Categorias • Vendas     │    │
│  └─────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
              │
              ▼
    📱 Dispositivo Android (SUNMI ou qualquer)
```

## 📦 Estrutura do Projeto

```
kotlin-caixa/
├── app/src/main/java/com/seucaixa/caixacombo/
│   ├── MainActivity.kt           # Entry point
│   ├── CaixaApplication.kt       # Inicialização
│   ├── data/
│   │   ├── model/                # Produto, Venda, Categoria
│   │   ├── database/             # Room Database + DAOs
│   │   └── repository/           # Repositórios
│   └── ui/
│       ├── screens/              # Telas (Checkout, Produtos, Vendas)
│       ├── viewmodel/            # ViewModels
│       └── theme/                # Cores, tipografia
├── build.gradle.kts              # Configuração Gradle
├── build-apk.sh                  # Script de build
└── README.md                     # Este arquivo
```

## 🚀 Como Começar

### 1. Pré-requisitos

```bash
# Instalar Android Studio: https://developer.android.com/studio
# JDK 17+ (incluído no Android Studio)
```

### 2. Clonar/Abrir Projeto

```bash
# Abra o Android Studio
File → Open → Selecione a pasta kotlin-caixa
```

### 3. Sincronizar Gradle

```bash
# No Android Studio, clique em "Sync Now" quando pedir
# Ou execute:
./gradlew sync
```

### 4. Rodar no Emulador/Dispositivo

```bash
# Conecte seu dispositivo Android
# Ou inicie um emulador no Android Studio

# Clique no botão "Run" (▶️) no Android Studio
```

### 5. Gerar APK (Release)

```bash
# Método 1: Script automatizado
./build-apk.sh

# Método 2: Gradle direto
./gradlew assembleRelease

# APK gerado em:
# app/build/outputs/apk/release/app-release-unsigned.apk
```

## 🎯 Funcionalidades

### ✅ Prontas
- [x] Checkout com carrinho
- [x] Gestão de produtos (CRUD)
- [x] Categorias
- [x] Controle de estoque
- [x] Histórico de vendas
- [x] Múltiplas formas de pagamento (Dinheiro, Cartão, PIX)
- [x] Filtros por período
- [x] Interface responsiva (tablet/phone)
- [x] Tema claro/escuro
- [x] Dados offline (SQLite)

### 🚧 Em Desenvolvimento
- [ ] Impressão de recibos (Bluetooth/USB)
- [ ] Leitor de código de barras
- [ ] Sincronização cloud (opcional)
- [ ] Relatórios detalhados
- [ ] Gestão de usuários
- [ ] Backup/Restore

## 🎨 Telas

### Checkout
```
┌──────────────────────────┬──────────────┐
│  Buscar...               │  CARRINHO    │
│                          │              │
│  Coca-Cola 2L     R$8.99 │  Coca-Cola   │
│  Pão Francês      R$0.50 │  x2      - + │
│  Arroz 5kg       R$22.90 │         🗑️   │
│  ...                     │              │
│                          │  Total:      │
│                          │  R$ 32.38    │
│                          │              │
│                          │ [Limpar]     │
│                          │ [Finalizar]  │
└──────────────────────────┴──────────────┘
```

### Gestão de Produtos
```
┌──────────────────────────────────┐
│  Gestão de Produtos     [+] [🗂️] │
├──────────────────────────────────┤
│  Buscar...                       │
├──────────────────────────────────┤
│  Coca-Cola 2L              R$8.99│
│  Bebidas • Estoque: 50 UN   ✏️ 🗑️│
├──────────────────────────────────┤
│  Pão Francês             R$0.50  │
│  Padaria • Estoque: 100 UN  ✏️ 🗑️│
└──────────────────────────────────┘
```

## 🔧 Configuração

### Alterar idioma padrão
```kotlin
// MainActivity.kt
val idioma = "pt-BR" // pt-BR, es, pt-PT, en
```

### Alterar moeda
```kotlin
// Produto.kt
fun precoFormatado(): String {
    return "R$ %.2f".format(precoVenda) // R$, $, €
}
```

## 🛠️ Build Local

### Debug (desenvolvimento)
```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Release (produção)
```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release-unsigned.apk
```

### Instalar no dispositivo
```bash
# Via ADB
adb install app/build/outputs/apk/release/app-release-unsigned.apk

# Ou transfira o APK para o dispositivo
```

## 📱 Compatibilidade

- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Dispositivos:** Tablets e smartphones Android
- **SUNMI:** Totalmente compatível

## 🆘 Troubleshooting

### Erro: "Gradle sync failed"
```bash
# Solução:
File → Invalidate Caches / Restart
```

### Erro: "JDK not found"
```bash
# Configure no Android Studio:
File → Settings → Build → Gradle → Gradle JDK
Selecione: Android Studio default JDK 17
```

### APK muito grande
```bash
# Habilitar ProGuard/R8 em build.gradle.kts:
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(...)
    }
}
```

## 📄 Licença

```
MIT License

Copyright (c) 2024 Caixa Combo

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files...
```

**Resumo:** Você pode fazer o que quiser com este código.

## 🤝 Contribuição

Este é SEU código. Modifique como quiser!

## 💬 Suporte

Abra uma issue ou modifique você mesmo - você é o dono! 🎉

---

**🎉 Pronto para usar!** Abra no Android Studio e comece a buildar! 🚀
