package com.caixacombo.payment.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequest {

    @NotBlank(message = "Empresa ID e obrigatorio")
    private String empresaId;

    @NotNull(message = "Data inicial e obrigatoria")
    private LocalDate startDate;

    @NotNull(message = "Data final e obrigatoria")
    private LocalDate endDate;

    private String format; // PDF, EXCEL, CSV

    private String reportType; // VENDAS, PAGAMENTOS, CONCILIACAO, CAIXA

    // Filters
    private String deviceId;
    private String operatorId;
    private String paymentMethod;
}
