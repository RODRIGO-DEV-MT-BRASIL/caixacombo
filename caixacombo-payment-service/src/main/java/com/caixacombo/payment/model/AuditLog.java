package com.caixacombo.payment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_empresa", columnList = "empresaId"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_entity", columnList = "entityType, entityId"),
    @Index(name = "idx_audit_created", columnList = "createdAt DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String action; // CREATE, UPDATE, DELETE, PAYMENT, REFUND, LOGIN, LOGOUT

    @Column(nullable = false, length = 50)
    private String entityType; // PAYMENT, TRANSACTION, USER, DEVICE

    @Column(length = 50)
    private String entityId;

    @Column(nullable = false, length = 50)
    private String empresaId;

    @Column(length = 50)
    private String userId;

    @Column(length = 100)
    private String username;

    @Column(length = 50)
    private String deviceId;

    @Column(columnDefinition = "TEXT")
    private String oldValues;

    @Column(columnDefinition = "TEXT")
    private String newValues;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 200)
    private String userAgent;

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS, FAILED, UNAUTHORIZED

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
