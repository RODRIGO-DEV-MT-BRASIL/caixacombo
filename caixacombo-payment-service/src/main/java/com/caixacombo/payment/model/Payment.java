package com.caixacombo.payment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_stone_id", columnList = "stoneTransactionId"),
    @Index(name = "idx_payment_empresa", columnList = "empresaId"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_created", columnList = "createdAt DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String externalId; // ID do Node.js

    @Column(length = 100)
    private String stoneTransactionId;

    @Column(length = 100)
    private String stoneAtk; // Authorization Token Key

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountInCents;

    @Column(nullable = false, length = 30)
    private String paymentMethod; // DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, PIX

    @Column(nullable = false, length = 20)
    private String status; // PENDING, APPROVED, DECLINED, REFUNDED, CANCELLED

    @Column(length = 200)
    private String statusMessage;

    @Column(length = 500)
    private String cardBrand;

    @Column(length = 20)
    private String cardLastDigits;

    @Column(length = 100)
    private String installments;

    // Tenant
    @Column(nullable = false, length = 50)
    private String empresaId;

    @Column(length = 50)
    private String deviceId;

    @Column(length = 100)
    private String deviceName;

    // Venda reference
    @Column(length = 50)
    private String vendaId;

    @Column(length = 50)
    private String numeroVenda;

    // Operator
    @Column(length = 100)
    private String operatorName;

    @Column(length = 50)
    private String operatorId;

    // Stone response raw
    @Column(columnDefinition = "TEXT")
    private String stoneResponseRaw;

    // Refund info
    @Column(precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @Column(length = 200)
    private String refundReason;

    @Column(length = 100)
    private String refundTransactionId;

    private LocalDateTime refundedAt;

    // Timestamps
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime paidAt;

    @Column
    private LocalDateTime cancelledAt;
}
