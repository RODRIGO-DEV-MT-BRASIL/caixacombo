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

</div>

<br>
<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:38A1F3,50:5E5FE7,100:9A4BE0&height=250&section=header&text=Rodrigo%20Dev%20MT&fontSize=60&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=Full%20Stack%20Developer%20•%20Mobile%20•%20Cloud%20•%20IA&descAlignY=60&descSize=20"/>

</div>

<!-- ✨ Frase Rotativa Animada -->
<div align="center">
  <img src="https://readme-typing-svg.herokuapp.com?font=Fira+Code&weight=600&pause=1000&color=38A1F3&center=true&vCenter=true&width=900&lines=Ol%C3%A1%2C+seja+bem-vindo+ao+meu+perfil!+%F0%9F%91%8B;Full+Stack+Developer+%E2%80%A2+Mobile+%E2%80%A2+Cloud+%E2%80%A2+IA+%F0%9F%9A%80;Web+Scraping+%26+Automa%C3%A7%C3%B5es+Escal%C3%A1veis+%F0%9F%93%8A;Transformando+ideias+em+software+de+alto+impacto+%E2%9A%A1;C%C3%B3digo+limpo+%2B+arquitetura+s%C3%B3lida+%F0%9F%92%8E"/>
</div>

<!-- 💻 Animação Desenvolvedor Codando -->
<div align="center">
  <img src="https://user-images.githubusercontent.com/74038190/212750155-3ceddfbd-19d3-40a1-85b0-9931f9c50a31.gif" width="600" alt="Developer coding animation"/>
</div>

<br>

<div align="center">

<a href="https://wa.me/5566996184323">
<img src="https://img.shields.io/badge/WhatsApp-25D366?style=for-the-badge&logo=whatsapp&logoColor=white"/>
</a>

<a href="https://t.me/rodrigodevmt">
<img src="https://img.shields.io/badge/Telegram-229ED9?style=for-the-badge&logo=telegram&logoColor=white"/>
</a>

<a href="https://github.com/rodrigopdevmt">
<img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white"/>
</a>

<a href="https://www.linkedin.com/in/rodrigo-dev-mt-929293372">
<img src="https://img.shields.io/badge/LinkedIn-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white"/>
</a>

<a href="https://www.rodrigodevmt.com.br">
<img src="https://img.shields.io/badge/Website-FF5722?style=for-the-badge&logo=googlechrome&logoColor=white"/>
</a>

</div>

---

