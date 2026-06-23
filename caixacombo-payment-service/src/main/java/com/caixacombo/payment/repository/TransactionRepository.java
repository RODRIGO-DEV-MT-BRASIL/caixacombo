package com.caixacombo.payment.repository;

import com.caixacombo.payment.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByPaymentId(Long paymentId);

    List<Transaction> findByEmpresaIdAndCreatedAtBetween(
            String empresaId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT t.type, COUNT(t), SUM(t.amount) FROM Transaction t " +
           "WHERE t.empresaId = :empresaId AND t.createdAt BETWEEN :start AND :end " +
           "GROUP BY t.type")
    List<Object[]> summarizeByType(
            @Param("empresaId") String empresaId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT t FROM Transaction t WHERE t.stoneTransactionId = :stoneId")
    List<Transaction> findByStoneTransactionId(@Param("stoneId") String stoneId);
}
