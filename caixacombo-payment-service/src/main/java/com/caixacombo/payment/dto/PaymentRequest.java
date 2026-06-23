package com.caixacombo.payment.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotBlank(message = "External ID e obrigatorio")
    private String externalId;

    @NotNull(message = "Valor e obrigatorio")
    @DecimalMin(value = "0.01", message = "Valor minimo e 0.01")
    private BigDecimal amount;

    @NotBlank(message = "Forma de pagamento e obrigatoria")
    @Pattern(regexp = "^(DINHEIRO|CARTAO_CREDITO|CARTAO_DEBITO|PIX|BOLETO|FIADO)$",
             message = "Forma de pagamento invalida")
    private String paymentMethod;

    @NotBlank(message = "Empresa ID e obrigatorio")
    private String empresaId;

    private String deviceId;
    private String deviceName;

    // Stone specific
    private String cardToken;
    private String stoneTransactionId;
    private String stoneAtk;

    // Installments
    @Min(value = 1, message = "Minimo 1 parcela")
    @Max(value = 12, message = "Maximo 12 parcelas")
    private Integer installments = 1;

    // Venda reference
    private String vendaId;
    private String numeroVenda;

    // Operator
    private String operatorName;
    private String operatorId;

    // Additional info
    private String description;
    private String metadata;
}
