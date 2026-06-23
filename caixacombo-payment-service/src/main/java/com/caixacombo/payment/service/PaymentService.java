package com.caixacombo.payment.service;

import com.caixacombo.payment.dto.PaymentRequest;
import com.caixacombo.payment.dto.PaymentResponse;
import com.caixacombo.payment.model.Payment;
import com.caixacombo.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StoneService stoneService;
    private final AuditService auditService;

    /**
     * Criar pagamento
     */
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        // Gerar external ID se nao fornecido
        if (request.getExternalId() == null || request.getExternalId().isEmpty()) {
            request.setExternalId(UUID.randomUUID().toString());
        }

        log.info("Criando pagamento: empresaId={}, amount={}, method={}",
                request.getEmpresaId(), request.getAmount(), request.getPaymentMethod());

        // Processar via Stone
        Payment payment = stoneService.processPayment(request);

        // Audit
        auditService.logWithUser("CREATE", "Payment", payment.getId().toString(),
                request.getEmpresaId(), request.getOperatorId(), request.getOperatorName(),
                "SUCCESS", "Pagamento criado: " + request.getExternalId());

        return PaymentResponse.from(payment);
    }

    /**
     * Cancelar pagamento
     */
    @Transactional
    public PaymentResponse cancelPayment(String stoneAtk, java.math.BigDecimal amount,
                                         String empresaId, String motivo, String userId, String username) {
        log.info("Cancelando pagamento: atk={}, empresaId={}", stoneAtk, empresaId);

        com.caixacombo.payment.dto.CancelRequest request = com.caixacombo.payment.dto.CancelRequest.builder()
                .stoneAtk(stoneAtk)
                .amount(amount)
                .empresaId(empresaId)
                .motivo(motivo)
                .build();

        Payment payment = stoneService.cancelPayment(request);

        auditService.logWithUser("CANCEL", "Payment", payment.getId().toString(),
                empresaId, userId, username, "SUCCESS", "Pagamento cancelado: " + motivo);

        return PaymentResponse.from(payment);
    }

    /**
     * Estornar pagamento
     */
    @Transactional
    public PaymentResponse refundPayment(String stoneTransactionId, java.math.BigDecimal amount,
                                         String empresaId, String motivo, String userId, String username) {
        log.info("Estornando pagamento: stoneId={}, empresaId={}", stoneTransactionId, empresaId);

        com.caixacombo.payment.dto.RefundRequest request = com.caixacombo.payment.dto.RefundRequest.builder()
                .stoneTransactionId(stoneTransactionId)
                .amount(amount)
                .empresaId(empresaId)
                .motivo(motivo)
                .build();

        Payment payment = stoneService.refundPayment(request);

        auditService.logWithUser("REFUND", "Payment", payment.getId().toString(),
                empresaId, userId, username, "SUCCESS", "Pagamento estornado: " + motivo);

        return PaymentResponse.from(payment);
    }

    /**
     * Buscar pagamento por ID
     */
    public PaymentResponse getById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pagamento nao encontrado: " + id));
        return PaymentResponse.from(payment);
    }

    /**
     * Buscar pagamento por external ID
     */
    public PaymentResponse getByExternalId(String externalId) {
        Payment payment = paymentRepository.findByExternalId(externalId)
                .orElseThrow(() -> new RuntimeException("Pagamento nao encontrado: " + externalId));
        return PaymentResponse.from(payment);
    }

    /**
     * Buscar pagamento por Stone ATK
     */
    public PaymentResponse getByStoneAtk(String stoneAtk) {
        Payment payment = paymentRepository.findByStoneAtk(stoneAtk)
                .orElseThrow(() -> new RuntimeException("Pagamento nao encontrado: " + stoneAtk));
        return PaymentResponse.from(payment);
    }

    /**
     * Listar pagamentos da empresa
     */
    public Page<PaymentResponse> listByEmpresa(String empresaId, Pageable pageable) {
        return paymentRepository.findByEmpresaId(empresaId, pageable)
                .map(PaymentResponse::from);
    }

    /**
     * Listar pagamentos por periodo
     */
    public List<PaymentResponse> listByPeriod(String empresaId, LocalDateTime start, LocalDateTime end) {
        return paymentRepository.findByEmpresaIdAndCreatedAtBetween(empresaId, start, end)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    /**
     * Buscar pagamentos de uma venda
     */
    public List<PaymentResponse> listByVenda(String vendaId) {
        return paymentRepository.findByVendaId(vendaId)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    /**
     * Confirmar pagamento via webhook (Stone)
     */
    @Transactional
    public void confirmPayment(String stoneTransactionId, String status) {
        Payment payment = paymentRepository.findByStoneTransactionId(stoneTransactionId)
                .orElse(null);

        if (payment != null) {
            String previousStatus = payment.getStatus();
            payment.setStatus(status);
            if ("APPROVED".equals(status)) {
                payment.setPaidAt(LocalDateTime.now());
            }
            paymentRepository.save(payment);

            auditService.log("WEBHOOK_CONFIRM", "Payment", payment.getId().toString(),
                    payment.getEmpresaId(), "SUCCESS",
                    "Status alterado via webhook: " + previousStatus + " -> " + status);
        }
    }
}
