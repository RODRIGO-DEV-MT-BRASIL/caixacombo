package com.caixacombo.payment.service;

import com.caixacombo.payment.dto.CancelRequest;
import com.caixacombo.payment.dto.PaymentRequest;
import com.caixacombo.payment.dto.RefundRequest;
import com.caixacombo.payment.model.Payment;
import com.caixacombo.payment.model.Transaction;
import com.caixacombo.payment.repository.PaymentRepository;
import com.caixacombo.payment.repository.TransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoneService {

    private final RestTemplate restTemplate;
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final NodeJsNotificationService notificationService;

    @Value("${stone.api-url}")
    private String stoneApiUrl;

    @Value("${stone.api-key}")
    private String stoneApiKey;

    @Value("${stone.merchant-id}")
    private String stoneMerchantId;

    @Value("${stone.environment}")
    private String stoneEnvironment;

    /**
     * Processar pagamento via Stone
     */
    @Transactional
    @CacheEvict(value = "dashboard", key = "#request.empresaId")
    public Payment processPayment(PaymentRequest request) {
        log.info("Processando pagamento: externalId={}, amount={}, method={}",
                request.getExternalId(), request.getAmount(), request.getPaymentMethod());

        // Criar registro de pagamento
        Payment payment = Payment.builder()
                .externalId(request.getExternalId())
                .amount(request.getAmount())
                .amountInCents(request.getAmount().multiply(BigDecimal.valueOf(100)))
                .paymentMethod(request.getPaymentMethod())
                .status("PENDING")
                .empresaId(request.getEmpresaId())
                .deviceId(request.getDeviceId())
                .deviceName(request.getDeviceName())
                .vendaId(request.getVendaId())
                .numeroVenda(request.getNumeroVenda())
                .operatorName(request.getOperatorName())
                .operatorId(request.getOperatorId())
                .build();

        payment = paymentRepository.save(payment);

        // Criar transacao de auditoria
        Transaction transaction = Transaction.builder()
                .type("AUTHORIZATION")
                .amount(request.getAmount())
                .status("PENDING")
                .payment(payment)
                .empresaId(request.getEmpresaId())
                .build();
        transaction = transactionRepository.save(transaction);

        try {
            // Se for pagamento em dinheiro ou fiado, aprovar direto
            if ("DINHEIRO".equals(request.getPaymentMethod()) ||
                "FIADO".equals(request.getPaymentMethod())) {

                payment.setStatus("APPROVED");
                payment.setPaidAt(LocalDateTime.now());
                payment.setStoneTransactionId("CASH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                payment = paymentRepository.save(payment);

                transaction.setStatus("SUCCESS");
                transaction.setStoneTransactionId(payment.getStoneTransactionId());
                transactionRepository.save(transaction);

                auditService.log("PAYMENT", "Payment", payment.getId().toString(),
                        request.getEmpresaId(), "APPROVED", "Pagamento em dinheiro/fiado aprovado");

                // Notificar Node.js
                notificationService.notifyPaymentApproved(payment);

                return payment;
            }

            // Processar com Stone API
            if (stoneApiKey == null || stoneApiKey.isBlank()) {
                log.warn("STONE_API_KEY nao configurada - modo simulacao");
                payment.setStatus("APPROVED");
                payment.setPaidAt(LocalDateTime.now());
                payment.setStoneTransactionId("SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                payment.setCardBrand("VISA");
                payment.setCardLastDigits("4242");
                payment.setInstallments("1");
                payment = paymentRepository.save(payment);

                transaction.setStatus("SUCCESS");
                transaction.setStoneTransactionId(payment.getStoneTransactionId());
                transactionRepository.save(transaction);

                auditService.log("PAYMENT", "Payment", payment.getId().toString(),
                        request.getEmpresaId(), "APPROVED", "Pagamento simulado (sem Stone API key)");

                notificationService.notifyPaymentApproved(payment);
                return payment;
            }

            Map<String, Object> stoneRequest = buildStoneRequest(request);
            HttpHeaders headers = createStoneHeaders();

            ResponseEntity<String> response = restTemplate.exchange(
                    stoneApiUrl + "/v1/transactions",
                    HttpMethod.POST,
                    new HttpEntity<>(stoneRequest, headers),
                    String.class
            );

            JsonNode responseJson = objectMapper.readTree(response.getBody());

            // Atualizar pagamento com resposta da Stone
            payment.setStoneTransactionId(responseJson.path("id").asText());
            payment.setStoneAtk(responseJson.path("atc").asText(null));
            payment.setCardBrand(responseJson.path("card_brand").asText(null));
            payment.setCardLastDigits(responseJson.path("card_last_digits").asText(null));
            payment.setInstallments(String.valueOf(responseJson.path("installments").asInt(1)));
            payment.setStoneResponseRaw(response.getBody());

            String stoneStatus = responseJson.path("status").asText();
            if ("APPROVED".equals(stoneStatus) || "CAPTURED".equals(stoneStatus)) {
                payment.setStatus("APPROVED");
                payment.setPaidAt(LocalDateTime.now());
            } else if ("DECLINED".equals(stoneStatus)) {
                payment.setStatus("DECLINED");
                payment.setStatusMessage(responseJson.path("status_message").asText("Pagamento recusado"));
            } else {
                payment.setStatus("PENDING");
            }

            payment = paymentRepository.save(payment);

            // Atualizar transacao
            transaction.setStoneTransactionId(payment.getStoneTransactionId());
            transaction.setStatus("SUCCESS".equals(stoneStatus) ? "SUCCESS" : "FAILED");
            transaction.setResponseData(response.getBody());
            transactionRepository.save(transaction);

            // Notificar Node.js
            if ("APPROVED".equals(payment.getStatus())) {
                notificationService.notifyPaymentApproved(payment);
            } else {
                notificationService.notifyPaymentDeclined(payment);
            }

            log.info("Pagamento processado: id={}, stoneId={}, status={}",
                    payment.getId(), payment.getStoneTransactionId(), payment.getStatus());

            return payment;

        } catch (Exception e) {
            log.error("Erro ao processar pagamento com Stone", e);

            payment.setStatus("ERROR");
            payment.setStatusMessage(e.getMessage());
            paymentRepository.save(payment);

            transaction.setStatus("FAILED");
            transaction.setErrorMessage(e.getMessage());
            transactionRepository.save(transaction);

            auditService.log("PAYMENT_ERROR", "Payment", payment.getId().toString(),
                    request.getEmpresaId(), "FAILED", e.getMessage());

            throw new RuntimeException("Erro ao processar pagamento: " + e.getMessage());
        }
    }

    /**
     * Cancelar pagamento via Stone
     */
    @Transactional
    @CacheEvict(value = "dashboard", key = "#request.empresaId")
    public Payment cancelPayment(CancelRequest request) {
        log.info("Cancelando pagamento: atk={}, amount={}", request.getStoneAtk(), request.getAmount());

        Payment payment = paymentRepository.findByStoneAtk(request.getStoneAtk())
                .orElseThrow(() -> new RuntimeException("Pagamento nao encontrado com ATK: " + request.getStoneAtk()));

        if (!"APPROVED".equals(payment.getStatus())) {
            throw new RuntimeException("Pagamento nao pode ser cancelado - status: " + payment.getStatus());
        }

        try {
            String responseData = "simulado";

            if (stoneApiKey != null && !stoneApiKey.isBlank()) {
                Map<String, Object> cancelRequest = Map.of(
                        "amount", request.getAmount().multiply(BigDecimal.valueOf(100))
                );
                HttpHeaders headers = createStoneHeaders();
                ResponseEntity<String> response = restTemplate.exchange(
                        stoneApiUrl + "/v1/transactions/" + payment.getStoneTransactionId() + "/cancel",
                        HttpMethod.POST,
                        new HttpEntity<>(cancelRequest, headers),
                        String.class
                );
                responseData = response.getBody();
            } else {
                log.warn("STONE_API_KEY nao configurada - cancelamento simulado");
            }

            payment.setStatus("CANCELLED");
            payment.setCancelledAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            Transaction transaction = Transaction.builder()
                    .type("CANCEL")
                    .amount(request.getAmount())
                    .status("SUCCESS")
                    .payment(payment)
                    .empresaId(request.getEmpresaId())
                    .stoneTransactionId(payment.getStoneTransactionId())
                    .responseData(responseData)
                    .build();
            transactionRepository.save(transaction);

            notificationService.notifyPaymentCancelled(payment);

            auditService.log("PAYMENT_CANCEL", "Payment", payment.getId().toString(),
                    request.getEmpresaId(), "SUCCESS", "Pagamento cancelado: " + request.getMotivo());

            return payment;

        } catch (Exception e) {
            log.error("Erro ao cancelar pagamento", e);
            throw new RuntimeException("Erro ao cancelar pagamento: " + e.getMessage());
        }
    }

    /**
     * Estornar pagamento via Stone
     */
    @Transactional
    @CacheEvict(value = "dashboard", key = "#request.empresaId")
    public Payment refundPayment(RefundRequest request) {
        log.info("Estornando pagamento: stoneId={}, amount={}", request.getStoneTransactionId(), request.getAmount());

        Payment payment = paymentRepository.findByStoneTransactionId(request.getStoneTransactionId())
                .orElseThrow(() -> new RuntimeException("Pagamento nao encontrado: " + request.getStoneTransactionId()));

        if (!"APPROVED".equals(payment.getStatus())) {
            throw new RuntimeException("Pagamento nao pode ser estornado - status: " + payment.getStatus());
        }

        if (request.getAmount().compareTo(payment.getAmount()) > 0) {
            throw new RuntimeException("Valor do estorno excede o valor do pagamento");
        }

        try {
            // Estornar via Stone
            Map<String, Object> refundRequest = Map.of(
                    "amount", request.getAmount().multiply(BigDecimal.valueOf(100))
            );

            HttpHeaders headers = createStoneHeaders();
            ResponseEntity<String> response = restTemplate.exchange(
                    stoneApiUrl + "/v1/transactions/" + payment.getStoneTransactionId() + "/refund",
                    HttpMethod.POST,
                    new HttpEntity<>(refundRequest, headers),
                    String.class
            );

            // Atualizar pagamento
            payment.setStatus("REFUNDED");
            payment.setRefundAmount(request.getAmount());
            payment.setRefundReason(request.getMotivo());
            payment.setRefundedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            // Criar transacao de estorno
            Transaction transaction = Transaction.builder()
                    .type("REFUND")
                    .amount(request.getAmount())
                    .status("SUCCESS")
                    .payment(payment)
                    .empresaId(request.getEmpresaId())
                    .stoneTransactionId(payment.getStoneTransactionId())
                    .responseData(response.getBody())
                    .build();
            transactionRepository.save(transaction);

            // Notificar Node.js
            notificationService.notifyPaymentRefunded(payment);

            auditService.log("PAYMENT_REFUND", "Payment", payment.getId().toString(),
                    request.getEmpresaId(), "SUCCESS", "Pagamento estornado: " + request.getMotivo());

            return payment;

        } catch (Exception e) {
            log.error("Erro ao estornar pagamento", e);
            throw new RuntimeException("Erro ao estornar pagamento: " + e.getMessage());
        }
    }

    /**
     * Consultar status de pagamento na Stone
     */
    @Cacheable(value = "stone-status", key = "#stoneTransactionId", unless = "#result == null")
    public JsonNode getTransactionStatus(String stoneTransactionId) {
        try {
            HttpHeaders headers = createStoneHeaders();
            ResponseEntity<String> response = restTemplate.exchange(
                    stoneApiUrl + "/v1/transactions/" + stoneTransactionId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("Erro ao consultar status Stone", e);
            return null;
        }
    }

    private Map<String, Object> buildStoneRequest(PaymentRequest request) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)));
        body.put("currency", "BRL");
        body.put("merchant_id", stoneMerchantId);
        body.put("environment", stoneEnvironment);

        if (request.getCardToken() != null) {
            body.put("card_token", request.getCardToken());
        }
        if (request.getInstallments() != null && request.getInstallments() > 1) {
            body.put("installments", request.getInstallments());
        }

        return body;
    }

    private HttpHeaders createStoneHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + stoneApiKey);
        headers.set("X-Api-Key", stoneApiKey);
        return headers;
    }
}
