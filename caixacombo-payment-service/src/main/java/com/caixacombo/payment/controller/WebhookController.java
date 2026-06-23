package com.caixacombo.payment.controller;

import com.caixacombo.payment.service.AuditService;
import com.caixacombo.payment.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Receber notificacoes de webhook")
public class WebhookController {

    private final PaymentService paymentService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Value("${stone.api-key:}")
    private String stoneApiKey;

    @Value("${nodejs.api-secret:}")
    private String nodeJsApiSecret;

    /**
     * Receber webhook da Stone
     */
    @PostMapping("/stone")
    @Operation(summary = "Webhook Stone", description = "Recebe notificacoes de pagamento da Stone")
    public ResponseEntity<Void> handleStoneWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Stone-Signature", required = false) String signature,
            @RequestHeader(value = "X-Webhook-Id", required = false) String webhookId) {

        log.info("Webhook Stone recebido: webhookId={}", webhookId);

        // Verificar assinatura (se configurada)
        if (stoneApiKey != null && !stoneApiKey.isEmpty() && signature != null) {
            if (!verifyStoneSignature(payload, signature)) {
                log.warn("Assinatura Stone invalida");
                auditService.log("WEBHOOK_ERROR", "Stone", webhookId,
                        null, "UNAUTHORIZED", "Assinatura invalida");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.path("type").asText();
            String transactionId = event.path("transaction_id").asText();

            log.info("Webhook Stone: type={}, transactionId={}", eventType, transactionId);

            switch (eventType) {
                case "payment.confirmed":
                case "payment.captured":
                    paymentService.confirmPayment(transactionId, "APPROVED");
                    break;
                case "payment.declined":
                    paymentService.confirmPayment(transactionId, "DECLINED");
                    break;
                case "payment.refunded":
                    paymentService.confirmPayment(transactionId, "REFUNDED");
                    break;
                case "payment.cancelled":
                    paymentService.confirmPayment(transactionId, "CANCELLED");
                    break;
                default:
                    log.warn("Tipo de evento desconhecido: {}", eventType);
            }

            auditService.log("WEBHOOK_RECEIVED", "Stone", transactionId,
                    null, "SUCCESS", "Evento: " + eventType);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Erro ao processar webhook Stone", e);
            auditService.log("WEBHOOK_ERROR", "Stone", webhookId,
                    null, "FAILED", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Receber webhook do Node.js (notificacao de pagamento processado)
     */
    @PostMapping("/nodejs")
    @Operation(summary = "Webhook Node.js", description = "Recebe notificacoes do backend Node.js")
    public ResponseEntity<Void> handleNodeJsWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-API-Secret", required = false) String apiSecret) {

        log.info("Webhook Node.js recebido");

        // Verificar secret (HMAC SHA256)
        if (nodeJsApiSecret != null && !nodeJsApiSecret.isEmpty()) {
            if (apiSecret == null || !nodeJsApiSecret.equals(apiSecret)) {
                log.warn("Webhook Node.js: secret invalido");
                auditService.log("WEBHOOK_ERROR", "NodeJs", null,
                        null, "UNAUTHORIZED", "Secret invalido");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.path("type").asText();

            log.info("Webhook Node.js: type={}", eventType);

            // Processar eventos do Node.js conforme necessario
            auditService.log("WEBHOOK_RECEIVED", "NodeJs", null,
                    event.path("empresaId").asText(null), "SUCCESS", "Evento: " + eventType);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Erro ao processar webhook Node.js", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean verifyStoneSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    stoneApiKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().equals(signature);
        } catch (Exception e) {
            log.error("Erro ao verificar assinatura", e);
            return false;
        }
    }
}
