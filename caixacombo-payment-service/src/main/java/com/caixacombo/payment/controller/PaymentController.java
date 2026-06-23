package com.caixacombo.payment.controller;

import com.caixacombo.payment.dto.PaymentRequest;
import com.caixacombo.payment.dto.PaymentResponse;
import com.caixacombo.payment.service.PaymentService;
import com.caixacombo.payment.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pagamentos", description = "API de pagamentos CaixaCombo")
public class PaymentController {

    private final PaymentService paymentService;
    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "Criar pagamento", description = "Processa um novo pagamento via Stone")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        log.info("Criando pagamento: empresaId={}, amount={}", request.getEmpresaId(), request.getAmount());
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pagamento por ID")
    public ResponseEntity<PaymentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @GetMapping("/external/{externalId}")
    @Operation(summary = "Buscar pagamento por external ID")
    public ResponseEntity<PaymentResponse> getByExternalId(@PathVariable String externalId) {
        return ResponseEntity.ok(paymentService.getByExternalId(externalId));
    }

    @GetMapping("/stone-atk/{stoneAtk}")
    @Operation(summary = "Buscar pagamento por Stone ATK")
    public ResponseEntity<PaymentResponse> getByStoneAtk(@PathVariable String stoneAtk) {
        return ResponseEntity.ok(paymentService.getByStoneAtk(stoneAtk));
    }

    @GetMapping("/empresa/{empresaId}")
    @Operation(summary = "Listar pagamentos da empresa")
    public ResponseEntity<Page<PaymentResponse>> listByEmpresa(
            @PathVariable String empresaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(paymentService.listByEmpresa(empresaId, PageRequest.of(page, size)));
    }

    @GetMapping("/empresa/{empresaId}/period")
    @Operation(summary = "Listar pagamentos por periodo")
    public ResponseEntity<List<PaymentResponse>> listByPeriod(
            @PathVariable String empresaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(paymentService.listByPeriod(
                empresaId,
                start.atStartOfDay(),
                end.atTime(LocalTime.MAX)
        ));
    }

    @GetMapping("/venda/{vendaId}")
    @Operation(summary = "Listar pagamentos de uma venda")
    public ResponseEntity<List<PaymentResponse>> listByVenda(@PathVariable String vendaId) {
        return ResponseEntity.ok(paymentService.listByVenda(vendaId));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancelar pagamento", description = "Cancela um pagamento via Stone")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @PathVariable Long id,
            @RequestParam String motivo,
            Authentication authentication) {
        PaymentResponse payment = paymentService.getById(id);
        return ResponseEntity.ok(paymentService.cancelPayment(
                payment.getStoneAtk(),
                payment.getAmount(),
                payment.getEmpresaId(),
                motivo,
                authentication.getName(),
                authentication.getName()
        ));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Estornar pagamento", description = "Estorna um pagamento via Stone")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam String motivo,
            Authentication authentication) {
        PaymentResponse payment = paymentService.getById(id);
        return ResponseEntity.ok(paymentService.refundPayment(
                payment.getStoneTransactionId(),
                amount,
                payment.getEmpresaId(),
                motivo,
                authentication.getName(),
                authentication.getName()
        ));
    }
}
