package com.caixacombo.payment.dto;

import com.caixacombo.payment.model.Payment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long id;
    private String externalId;
    private String stoneTransactionId;
    private String stoneAtk;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String statusMessage;
    private String cardBrand;
    private String cardLastDigits;
    private String installments;
    private String empresaId;
    private String vendaId;
    private String numeroVenda;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    // Refund info
    private BigDecimal refundAmount;
    private String refundReason;
    private LocalDateTime refundedAt;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .externalId(payment.getExternalId())
                .stoneTransactionId(payment.getStoneTransactionId())
                .stoneAtk(payment.getStoneAtk())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .statusMessage(payment.getStatusMessage())
                .cardBrand(payment.getCardBrand())
                .cardLastDigits(payment.getCardLastDigits())
                .installments(payment.getInstallments())
                .empresaId(payment.getEmpresaId())
                .vendaId(payment.getVendaId())
                .numeroVenda(payment.getNumeroVenda())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .refundAmount(payment.getRefundAmount())
                .refundReason(payment.getRefundReason())
                .refundedAt(payment.getRefundedAt())
                .build();
    }
}
