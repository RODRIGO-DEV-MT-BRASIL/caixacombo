package com.caixacombo.payment.service;

import com.caixacombo.payment.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NodeJsNotificationService {

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    private final RestTemplate restTemplate;

    @Value("${nodejs.api-url:http://localhost:3001}")
    private String nodejsApiUrl;

    @Value("${nodejs.api-secret:}")
    private String nodejsApiSecret;

    /**
     * Notificar pagamento aprovado via RabbitMQ e HTTP
     */
    @Async
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void notifyPaymentApproved(Payment payment) {
        Map<String, Object> event = Map.of(
                "type", "payment.approved",
                "paymentId", payment.getId(),
                "externalId", payment.getExternalId(),
                "stoneTransactionId", payment.getStoneTransactionId() != null ? payment.getStoneTransactionId() : "",
                "amount", payment.getAmount(),
                "paymentMethod", payment.getPaymentMethod(),
                "empresaId", payment.getEmpresaId(),
                "deviceId", payment.getDeviceId() != null ? payment.getDeviceId() : "",
                "vendaId", payment.getVendaId() != null ? payment.getVendaId() : ""
        );

        // Enviar via RabbitMQ (se disponivel)
        if (rabbitTemplate != null) {
            try {
                rabbitTemplate.convertAndSend("payment.exchange", "payment.approved", event);
                log.debug("Evento payment.approved enviado via RabbitMQ");
            } catch (Exception e) {
                log.warn("RabbitMQ indisponivel, notificando via HTTP", e);
            }
        }

        // Enviar via HTTP como fallback
        try {
            HttpHeaders headers = createHeaders();
            restTemplate.exchange(
                    nodejsApiUrl + "/api/payment/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(event, headers),
                    String.class
            );
            log.debug("Evento payment.approved notificado via HTTP");
        } catch (Exception e) {
            log.error("Erro ao notificar Node.js via HTTP", e);
        }
    }

    /**
     * Notificar pagamento recusado
     */
    @Async
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void notifyPaymentDeclined(Payment payment) {
        Map<String, Object> event = Map.of(
                "type", "payment.declined",
                "paymentId", payment.getId(),
                "externalId", payment.getExternalId(),
                "status", payment.getStatus(),
                "statusMessage", payment.getStatusMessage() != null ? payment.getStatusMessage() : "",
                "empresaId", payment.getEmpresaId()
        );

        if (rabbitTemplate != null) {
            try {
                rabbitTemplate.convertAndSend("payment.exchange", "payment.declined", event);
            } catch (Exception e) {
                log.warn("RabbitMQ indisponivel", e);
            }
        }

        try {
            HttpHeaders headers = createHeaders();
            restTemplate.exchange(
                    nodejsApiUrl + "/api/payment/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(event, headers),
                    String.class
            );
        } catch (Exception e) {
            log.error("Erro ao notificar Node.js", e);
        }
    }

    /**
     * Notificar pagamento cancelado
     */
    @Async
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void notifyPaymentCancelled(Payment payment) {
        Map<String, Object> event = Map.of(
                "type", "payment.cancelled",
                "paymentId", payment.getId(),
                "externalId", payment.getExternalId(),
                "stoneTransactionId", payment.getStoneTransactionId() != null ? payment.getStoneTransactionId() : "",
                "empresaId", payment.getEmpresaId()
        );

        if (rabbitTemplate != null) {
            try {
                rabbitTemplate.convertAndSend("payment.exchange", "payment.cancelled", event);
            } catch (Exception e) {
                log.warn("RabbitMQ indisponivel", e);
            }
        }

        try {
            HttpHeaders headers = createHeaders();
            restTemplate.exchange(
                    nodejsApiUrl + "/api/payment/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(event, headers),
                    String.class
            );
        } catch (Exception e) {
            log.error("Erro ao notificar Node.js", e);
        }
    }

    /**
     * Notificar pagamento estornado
     */
    @Async
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void notifyPaymentRefunded(Payment payment) {
        Map<String, Object> event = Map.of(
                "type", "payment.refunded",
                "paymentId", payment.getId(),
                "externalId", payment.getExternalId(),
                "refundAmount", payment.getRefundAmount() != null ? payment.getRefundAmount() : payment.getAmount(),
                "empresaId", payment.getEmpresaId()
        );

        if (rabbitTemplate != null) {
            try {
                rabbitTemplate.convertAndSend("payment.exchange", "payment.refunded", event);
            } catch (Exception e) {
                log.warn("RabbitMQ indisponivel", e);
            }
        }

        try {
            HttpHeaders headers = createHeaders();
            restTemplate.exchange(
                    nodejsApiUrl + "/api/payment/webhook",
                    HttpMethod.POST,
                    new HttpEntity<>(event, headers),
                    String.class
            );
        } catch (Exception e) {
            log.error("Erro ao notificar Node.js", e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (nodejsApiSecret != null && !nodejsApiSecret.isEmpty()) {
            headers.set("X-API-Secret", nodejsApiSecret);
        }
        return headers;
    }
}
