# Integração Stone — Caixa Combo

## Objetivo

O Caixa Combo utilizará exclusivamente a Stone como solução de pagamento.

## Fluxo de pagamento

```txt
Cliente -> Caixa Combo -> Stone SDK/API -> Terminal Stone -> Confirmação -> Impressão
```

## Recursos previstos

- Crédito
- Débito
- PIX Stone
- Cancelamento de venda
- Reimpressão
- Consulta de transação
- Fechamento de lote
- Conciliação

## Estrutura recomendada

```txt
backend/src/modules/stone/
├── stone.module.ts
├── stone.controller.ts
├── stone.service.ts
├── dto/
├── interfaces/
└── webhooks/
```

## Android

```txt
apps/android-terminal/
├── payment/
│   ├── StonePaymentManager.kt
│   ├── StonePIX.kt
│   ├── StoneCredit.kt
│   ├── StoneDebit.kt
│   └── StoneReceiptPrinter.kt
```

## Melhorias futuras

- Modo offline de contingência
- Sincronização automática
- Dashboard financeiro
- Gestão de taxas Stone
- Relatórios de vendas
- Split por operador
- Multiempresa

## Segurança

- JWT obrigatório
- Assinatura de transações
- Logs de auditoria
- Controle de cancelamentos
- Permissões por usuário

## Interface recomendada do checkout

- Botões grandes para toque rápido
- Confirmação sonora
- Status visual da transação
- Tela otimizada para Android POS
- Feedback de erro amigável
- Operação rápida com poucos toques
