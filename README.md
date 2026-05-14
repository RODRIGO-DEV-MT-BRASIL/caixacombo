# Caixa Combo

Sistema de PDV Android para controle de vendas, checkout, login de funcionários e operação de caixa com suporte a white-label por empresa.

## Regra principal de pagamento

**O Caixa Combo deve aceitar somente Stone como sistema de pagamento.**

Não devem ser implementados outros provedores de pagamento como PagSeguro, Mercado Pago, Cielo, Rede, InfinitePay, Ton ou gateways genéricos.

Toda integração de pagamento deve seguir este padrão:

```txt
PDV Caixa Combo -> Módulo Stone -> Terminal Stone / API Stone -> Confirmação da venda
```

## Objetivo do projeto

O Caixa Combo tem como objetivo ser um sistema de caixa moderno para uso em restaurantes, bares, lanchonetes, lojas, eventos e operações que precisam de venda rápida em terminal Android.

## Módulos atuais identificados

- App Android para terminal de caixa
- Tela de checkout/POS
- Login de funcionário por código
- Login de funcionário por email + PIN
- Integração com API REST de autenticação
- Branding por empresa após login
- Cores personalizadas por empresa
- Configuração white-label via Android flavors
- Data e hora no terminal em formato brasileiro
- Pagamento exclusivo via Stone

## Tecnologias identificadas

- Android
- Kotlin
- Jetpack Compose
- Gradle Kotlin DSL
- API REST
- White-label com product flavors
- Integração Stone como única solução de pagamento

## Organização recomendada

A estrutura ideal do projeto deve evoluir para:

```txt
caixacombo/
├── apps/
│   ├── android-terminal/
│   └── web-admin/
├── backend/
│   ├── src/
│   ├── prisma/
│   └── package.json
├── docs/
│   ├── ARQUITETURA.md
│   ├── STONE.md
│   ├── ROADMAP.md
│   └── CHECKLIST.md
└── README.md
```

> Importante: a reorganização física das pastas deve ser feita com cuidado para não quebrar o build atual do Android.

## Prioridades

1. Documentar o projeto atual
2. Padronizar nomes de pastas e módulos
3. Separar app Android, backend e painel web
4. Criar fluxo de build release para APK
5. Melhorar segurança do login e permissões
6. Adicionar modo offline com sincronização
7. Criar documentação de deploy e instalação
8. Implementar integração Stone como única forma de pagamento

## Status

Projeto em organização técnica inicial.

## Próximos passos

- Mapear arquivos principais do app Android
- Identificar endpoints usados pela aplicação
- Documentar telas e fluxos
- Criar roadmap de evolução
- Padronizar estrutura de pastas
- Criar pipeline de build e release
- Criar módulo de pagamento exclusivo Stone
