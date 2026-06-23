package com.caixacombo.payment.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {

    @NotBlank(message = "Stone Transaction ID e obrigatorio")
    private String stoneTransactionId;

    @NotNull(message = "Valor e obrigatorio")
    @DecimalMin(value = "0.01", message = "Valor minimo e 0.01")
    private BigDecimal amount;

    @NotBlank(message = "Empresa ID e obrigatorio")
    private String empresaId;

    @NotBlank(message = "Motivo e obrigatorio")
    private String motivo;

    private String vendingId;
    private String deviceId;
}
