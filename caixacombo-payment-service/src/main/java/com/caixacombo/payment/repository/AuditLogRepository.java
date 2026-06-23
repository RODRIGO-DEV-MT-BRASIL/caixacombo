package com.caixacombo.payment.repository;

import com.caixacombo.payment.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByEmpresaIdOrderByCreatedAtDesc(String empresaId, Pageable pageable);

    List<AuditLog> findByEmpresaIdAndActionAndCreatedAtBetween(
            String empresaId, String action, LocalDateTime start, LocalDateTime end);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a " +
           "WHERE a.empresaId = :empresaId AND a.createdAt BETWEEN :start AND :end " +
           "GROUP BY a.action")
    List<Object[]> countByAction(
            @Param("empresaId") String empresaId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT a FROM AuditLog a WHERE a.empresaId = :empresaId " +
           "AND a.entityType = :entityType AND a.entityId = :entityId " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findByEntity(
            @Param("empresaId") String empresaId,
            @Param("entityType") String entityType,
            @Param("entityId") String entityId);
}
