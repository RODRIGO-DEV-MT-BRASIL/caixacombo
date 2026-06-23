package com.caixacombo.payment.controller;

import com.caixacombo.payment.dto.DashboardSummary;
import com.caixacombo.payment.dto.ReportRequest;
import com.caixacombo.payment.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Relatorios", description = "API de relatorios e dashboard")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard/{empresaId}")
    @Operation(summary = "Dashboard resumo", description = "Retorna resumo do dashboard para a empresa")
    public ResponseEntity<DashboardSummary> getDashboard(
            @PathVariable String empresaId,
            @RequestParam(defaultValue = "hoje") String period) {
        return ResponseEntity.ok(reportService.getDashboardSummary(empresaId, period));
    }

    @GetMapping("/dashboard/{empresaId}/period")
    @Operation(summary = "Dashboard por periodo")
    public ResponseEntity<DashboardSummary> getDashboardByPeriod(
            @PathVariable String empresaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        // TODO: Implementar busca por periodo customizado
        return ResponseEntity.ok(reportService.getDashboardSummary(empresaId, "mes"));
    }

    @PostMapping("/pdf")
    @Operation(summary = "Gerar relatorio PDF", description = "Gera relatorio de pagamentos em PDF")
    public ResponseEntity<byte[]> generatePdfReport(@Valid @RequestBody ReportRequest request) {
        log.info("Gerando relatorio PDF: empresaId={}, periodo={} a {}",
                request.getEmpresaId(), request.getStartDate(), request.getEndDate());

        byte[] pdfBytes = reportService.generatePdfReport(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("relatorio-pagamentos-" + request.getStartDate() + ".pdf")
                .build());

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @PostMapping("/excel")
    @Operation(summary = "Gerar relatorio Excel", description = "Gera relatorio de pagamentos em Excel")
    public ResponseEntity<byte[]> generateExcelReport(@Valid @RequestBody ReportRequest request) {
        log.info("Gerando relatorio Excel: empresaId={}, periodo={} a {}",
                request.getEmpresaId(), request.getStartDate(), request.getEndDate());

        byte[] excelBytes = reportService.generateExcelReport(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("relatorio-pagamentos-" + request.getStartDate() + ".xlsx")
                .build());

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }
}
