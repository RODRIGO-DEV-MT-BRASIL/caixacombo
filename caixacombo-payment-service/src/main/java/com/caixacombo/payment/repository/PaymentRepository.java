package com.caixacombo.payment.repository;

import com.caixacombo.payment.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByExternalId(String externalId);

    Optional<Payment> findByStoneTransactionId(String stoneTransactionId);

    Optional<Payment> findByStoneAtk(String stoneAtk);

    List<Payment> findByEmpresaIdAndCreatedAtBetween(
            String empresaId, LocalDateTime start, LocalDateTime end);

    Page<Payment> findByEmpresaId(String empresaId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.empresaId = :empresaId " +
           "AND p.status = :status AND p.createdAt BETWEEN :start AND :end")
    List<Payment> findByEmpresaIdAndStatusAndDateRange(
            @Param("empresaId") String empresaId,
            @Param("status") String status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.empresaId = :empresaId " +
           "AND p.status = 'APPROVED' AND p.createdAt BETWEEN :start AND :end")
    BigDecimal sumApprovedAmountByDateRange(
            @Param("empresaId") String empresaId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.empresaId = :empresaId " +
           "AND p.status = 'APPROVED' AND p.createdAt BETWEEN :start AND :end")
    Long countApprovedByDateRange(
            @Param("empresaId") String empresaId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT p.paymentMethod, SUM(p.amount), COUNT(p) FROM Payment p " +
           "WHERE p.empresaId = :empresaId AND p.status = 'APPROVED' " +
           "AND p.createdAt BETWEEN :start AND :end " +
           "GROUP BY p.paymentMethod")
    List<Object[]> sumByPaymentMethod(
            @Param("empresaId") String empresaId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT p FROM Payment p WHERE p.empresaId = :empresaId AND p.status = 'APPROVED' " +
           "AND p.createdAt BETWEEN :start AND :end")
    List<Payment> findApprovedByDateRange(
            @Param("empresaId") String empresaId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT p.status, COUNT(p) FROM Payment p " +
           "WHERE p.empresaId = :empresaId AND p.createdAt BETWEEN :start AND :end " +
           "GROUP BY p.status")
    List<Object[]> countByStatus(
            @Param("empresaId") String empresaId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    long countByEmpresaIdAndDeviceId(String empresaId, String deviceId);

    List<Payment> findByVendaId(String vendaId);

    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.createdAt < :before")
    List<Payment> findByStatusStuckBefore(@Param("status") String status, @Param("before") LocalDateTime before);

    @Query("SELECT p FROM Payment p WHERE p.empresaId = :empresaId " +
           "AND p.status = 'APPROVED' AND p.paidAt IS NOT NULL " +
           "ORDER BY p.paidAt DESC")
    List<Payment> findRecentApproved(@Param("empresaId") String empresaId, Pageable pageable);
}
