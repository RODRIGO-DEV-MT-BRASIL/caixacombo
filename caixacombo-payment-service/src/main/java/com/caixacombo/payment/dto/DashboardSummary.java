package com.caixacombo.payment.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummary {

    private BigDecimal totalVendas;
    private Long totalTransacoes;
    private BigDecimal ticketMedio;

    // Por forma de pagamento
    private Map<String, BigDecimal> porFormaPagamento;
    private Map<String, Long> transacoesPorFormaPagamento;

    // Por status
    private Map<String, Long> porStatus;

    // Por dia (ultimos 7 dias)
    private Map<String, BigDecimal> vendasPorDia;

    // Top produtos
    private Map<String, BigDecimal> topProdutos;

    // Terminais ativos
    private Long terminaisAtivos;
    private Long terminaisEmUso;
}
