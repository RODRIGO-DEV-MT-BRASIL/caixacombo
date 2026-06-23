package com.caixacombo.payment.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelRequest {

    @NotBlank(message = "Stone ATK e obrigatorio para cancelamento")
    private String stoneAtk;

    @NotNull(message = "Valor e obrigatorio")
    @DecimalMin(value = "0.01", message = "Valor minimo e 0.01")
    private BigDecimal amount;

    @NotBlank(message = "Empresa ID e obrigatorio")
    private String empresaId;

    private String motivo;
    private String vendingId;
    private String deviceId;
}
