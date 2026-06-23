package com.caixacombo.payment.controller;

import com.caixacombo.payment.model.AuditLog;
import com.caixacombo.payment.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auditoria", description = "API de auditoria")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/empresa/{empresaId}")
    @Operation(summary = "Listar logs de auditoria da empresa")
    public ResponseEntity<Page<AuditLog>> listByEmpresa(
            @PathVariable String empresaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.findByEmpresaId(
                empresaId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/empresa/{empresaId}/action/{action}")
    @Operation(summary = "Buscar logs por acao")
    public ResponseEntity<List<AuditLog>> listByAction(
            @PathVariable String empresaId,
            @PathVariable String action,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(auditService.findByPeriod(
                empresaId, action, start.atStartOfDay(), end.atTime(LocalTime.MAX)));
    }

    @GetMapping("/empresa/{empresaId}/summary")
    @Operation(summary = "Resumo de acoes por periodo")
    public ResponseEntity<List<Object[]>> summaryByAction(
            @PathVariable String empresaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(auditService.countByAction(
                empresaId, start.atStartOfDay(), end.atTime(LocalTime.MAX)));
    }
}
