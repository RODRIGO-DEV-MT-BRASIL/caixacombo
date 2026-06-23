package com.caixacombo.payment.scheduler;

import com.caixacombo.payment.model.Payment;
import com.caixacombo.payment.repository.PaymentRepository;
import com.caixacombo.payment.service.AuditService;
import com.caixacombo.payment.service.NodeJsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Stone compliance: reversalProvider a cada hora.
 * Cancela automaticamente transações com erro para evitar cobranças indevidas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReversalScheduler {

    private final PaymentRepository paymentRepository;
    private final AuditService auditService;
    private final NodeJsNotificationService notificationService;

    /**
     * Executa a cada 60 minutos para cancelar transações com erro.
     * Stone recomenda: "chamar reversalProvider a cada hora para cancelar transações com erro"
     */
    @Scheduled(fixedRate = 3600000) // 1 hora
    @Transactional
    public void executeReversal() {
        log.info("[REVERSAL] Iniciando varredura de transações com erro...");

        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        List<Payment> erroredPayments = paymentRepository
                .findByEmpresaIdAndStatusAndDateRange("", "ERROR", cutoff.minusDays(7), LocalDateTime.now());

        // Também busca transações PENDING há mais de 1 hora (possivelmente travadas)
        List<Payment> stuckPayments = paymentRepository
                .findByStatusStuckBefore("PENDING", cutoff);

        int reversed = 0;

        for (Payment payment : erroredPayments) {
            try {
                reversePayment(payment, "ERROR");
                reversed++;
            } catch (Exception e) {
                log.error("[REVERSAL] Erro ao reverter pagamento id={}: {}", payment.getId(), e.getMessage());
            }
        }

        for (Payment payment : stuckPayments) {
            try {
                reversePayment(payment, "STUCK");
                reversed++;
            } catch (Exception e) {
                log.error("[REVERSAL] Erro ao reverter pagamento travado id={}: {}", payment.getId(), e.getMessage());
            }
        }

        if (reversed > 0) {
            log.info("[REVERSAL] {} transações revertidas automaticamente", reversed);
        } else {
            log.info("[REVERSAL] Nenhuma transação para reverter");
        }
    }

    private void reversePayment(Payment payment, String reason) {
        payment.setStatus("CANCELLED");
        payment.setCancelledAt(LocalDateTime.now());
        payment.setStatusMessage("Reversão automática: " + reason + " - " + reason);
        paymentRepository.save(payment);

        auditService.log("REVERSAL", "Payment", payment.getId().toString(),
                payment.getEmpresaId(), "AUTO_CANCELLED",
                "Reversão automática Stone: " + reason);

        notificationService.notifyPaymentCancelled(payment);

        log.info("[REVERSAL] Pagamento id={} revertido (motivo: {})", payment.getId(), reason);
    }
}
