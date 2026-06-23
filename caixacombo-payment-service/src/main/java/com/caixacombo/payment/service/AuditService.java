package com.caixacombo.payment.service;

import com.caixacombo.payment.model.AuditLog;
import com.caixacombo.payment.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Registrar log de auditoria (assincrono)
     */
    @Async
    public void log(String action, String entityType, String entityId,
                    String empresaId, String status, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .empresaId(empresaId)
                    .status(status)
                    .metadata(details)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit: {} {} {} - {} - {}", action, entityType, entityId, status, details);
        } catch (Exception e) {
            log.error("Erro ao salvar audit log", e);
        }
    }

    /**
     * Registrar log com usuario
     */
    @Async
    public void logWithUser(String action, String entityType, String entityId,
                            String empresaId, String userId, String username,
                            String status, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .empresaId(empresaId)
                    .userId(userId)
                    .username(username)
                    .status(status)
                    .metadata(details)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Erro ao salvar audit log", e);
        }
    }

    /**
     * Buscar logs de auditoria
     */
    public Page<AuditLog> findByEmpresaId(String empresaId, Pageable pageable) {
        return auditLogRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaId, pageable);
    }

    /**
     * Buscar logs por periodo
     */
    public java.util.List<AuditLog> findByPeriod(String empresaId, String action,
                                                  LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByEmpresaIdAndActionAndCreatedAtBetween(
                empresaId, action, start, end);
    }

    /**
     * Contar acoes por periodo
     */
    public java.util.List<Object[]> countByAction(String empresaId,
                                                   LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.countByAction(empresaId, start, end);
    }
}
