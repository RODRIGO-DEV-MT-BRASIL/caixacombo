package com.caixacombo.payment.service;

import com.caixacombo.payment.dto.DashboardSummary;
import com.caixacombo.payment.dto.ReportRequest;
import com.caixacombo.payment.model.Payment;
import com.caixacombo.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final PaymentRepository paymentRepository;

    /**
     * Gerar resumo do dashboard
     */
    @Cacheable(value = "dashboard", key = "#empresaId + '_' + #period")
    public DashboardSummary getDashboardSummary(String empresaId, String period) {
        LocalDateTime[] dates = getDateRange(period);
        LocalDateTime start = dates[0];
        LocalDateTime end = dates[1];

        BigDecimal totalVendas = paymentRepository.sumApprovedAmountByDateRange(empresaId, start, end);
        Long totalTransacoes = paymentRepository.countApprovedByDateRange(empresaId, start, end);

        if (totalVendas == null) totalVendas = BigDecimal.ZERO;
        if (totalTransacoes == null) totalTransacoes = 0L;

        BigDecimal ticketMedio = totalTransacoes > 0 ?
                totalVendas.divide(BigDecimal.valueOf(totalTransacoes), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Por forma de pagamento
        List<Object[]> porFormaPagamento = paymentRepository.sumByPaymentMethod(empresaId, start, end);
        Map<String, BigDecimal> vendasPorFormaPagamento = new LinkedHashMap<>();
        Map<String, Long> transacoesPorFormaPagamento = new LinkedHashMap<>();
        for (Object[] row : porFormaPagamento) {
            vendasPorFormaPagamento.put((String) row[0], (BigDecimal) row[1]);
            transacoesPorFormaPagamento.put((String) row[0], (Long) row[2]);
        }

        // Por status
        List<Object[]> porStatus = paymentRepository.countByStatus(empresaId, start, end);
        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (Object[] row : porStatus) {
            statusMap.put((String) row[0], (Long) row[1]);
        }

        // Por dia (agrupar em Java para compatibilidade com H2)
        List<Payment> approvedPayments = paymentRepository.findApprovedByDateRange(empresaId, start, end);
        Map<String, BigDecimal> vendasPorDia = new LinkedHashMap<>();
        for (Payment p : approvedPayments) {
            String dateStr = p.getCreatedAt().toLocalDate().toString();
            vendasPorDia.merge(dateStr, p.getAmount(), BigDecimal::add);
        }

        return DashboardSummary.builder()
                .totalVendas(totalVendas)
                .totalTransacoes(totalTransacoes)
                .ticketMedio(ticketMedio)
                .porFormaPagamento(vendasPorFormaPagamento)
                .transacoesPorFormaPagamento(transacoesPorFormaPagamento)
                .porStatus(statusMap)
                .vendasPorDia(vendasPorDia)
                .build();
    }

    /**
     * Gerar relatorio em PDF
     */
    public byte[] generatePdfReport(ReportRequest request) {
        try {
            List<Payment> payments = getPaymentsForReport(request);

            // Preparar dados para JasperReports
            List<Map<String, Object>> reportData = payments.stream()
                    .map(p -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", p.getId());
                        map.put("data", p.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                        map.put("valor", p.getAmount());
                        map.put("formaPagamento", p.getPaymentMethod());
                        map.put("status", p.getStatus());
                        map.put("operador", p.getOperatorName() != null ? p.getOperatorName() : "-");
                        map.put("terminal", p.getDeviceName() != null ? p.getDeviceName() : "-");
                        return map;
                    })
                    .collect(Collectors.toList());

            // Criar relatorio Jasper
            JasperReport jasperReport = JasperCompileManager.compileReport(
                    createJRXMLContent());

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("empresaId", request.getEmpresaId());
            parameters.put("periodo", request.getStartDate() + " a " + request.getEndDate());
            parameters.put("totalVendas", payments.stream()
                    .filter(p -> "APPROVED".equals(p.getStatus()))
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar PDF", e);
            throw new RuntimeException("Erro ao gerar relatorio PDF: " + e.getMessage());
        }
    }

    /**
     * Gerar relatorio em Excel
     */
    public byte[] generateExcelReport(ReportRequest request) {
        try {
            List<Payment> payments = getPaymentsForReport(request);

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Pagamentos");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Data", "Valor", "Forma Pgto", "Status", "Operador", "Terminal"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (Payment p : payments) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getCreatedAt().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                row.createCell(2).setCellValue(p.getAmount().doubleValue());
                row.createCell(3).setCellValue(p.getPaymentMethod());
                row.createCell(4).setCellValue(p.getStatus());
                row.createCell(5).setCellValue(p.getOperatorName() != null ? p.getOperatorName() : "-");
                row.createCell(6).setCellValue(p.getDeviceName() != null ? p.getDeviceName() : "-");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar Excel", e);
            throw new RuntimeException("Erro ao gerar relatorio Excel: " + e.getMessage());
        }
    }

    private List<Payment> getPaymentsForReport(ReportRequest request) {
        LocalDateTime start = request.getStartDate().atStartOfDay();
        LocalDateTime end = request.getEndDate().atTime(LocalTime.MAX);

        return paymentRepository.findByEmpresaIdAndCreatedAtBetween(
                request.getEmpresaId(), start, end);
    }

    private LocalDateTime[] getDateRange(String period) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start;

        switch (period.toLowerCase()) {
            case "hoje":
                start = LocalDate.now().atStartOfDay();
                break;
            case "semana":
                start = LocalDate.now().minusWeeks(1).atStartOfDay();
                break;
            case "mes":
                start = LocalDate.now().minusMonths(1).atStartOfDay();
                break;
            case "trimestre":
                start = LocalDate.now().minusMonths(3).atStartOfDay();
                break;
            case "ano":
                start = LocalDate.now().minusYears(1).atStartOfDay();
                break;
            default:
                start = LocalDate.now().atStartOfDay();
        }

        return new LocalDateTime[]{start, end};
    }

    private String createJRXMLContent() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\"\n" +
                "              xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "              xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports\n" +
                "              http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\"\n" +
                "              name=\"Pagamentos\" pageWidth=\"595\" pageHeight=\"842\" columnWidth=\"515\">\n" +
                "    <field name=\"id\" class=\"java.lang.Long\"/>\n" +
                "    <field name=\"data\" class=\"java.lang.String\"/>\n" +
                "    <field name=\"valor\" class=\"java.math.BigDecimal\"/>\n" +
                "    <field name=\"formaPagamento\" class=\"java.lang.String\"/>\n" +
                "    <field name=\"status\" class=\"java.lang.String\"/>\n" +
                "    <field name=\"operador\" class=\"java.lang.String\"/>\n" +
                "    <field name=\"terminal\" class=\"java.lang.String\"/>\n" +
                "    <title><band height=\"50\">\n" +
                "        <textField><reportElement x=\"0\" y=\"0\" width=\"515\" height=\"30\"/>\n" +
                "            <textFieldExpression><![CDATA[\"Relatorio de Pagamentos - CaixaCombo\"]]></textFieldExpression>\n" +
                "        </textField>\n" +
                "    </band></title>\n" +
                "<columnHeader><band height=\"20\">\n" +
                "    <staticText><reportElement x=\"0\" y=\"0\" width=\"40\" height=\"20\"/>\n" +
                "        <text><![CDATA[ID]]></text></staticText>\n" +
                "    <staticText><reportElement x=\"40\" y=\"0\" width=\"100\" height=\"20\"/>\n" +
                "        <text><![CDATA[Data]]></text></staticText>\n" +
                "    <staticText><reportElement x=\"140\" y=\"0\" width=\"80\" height=\"20\"/>\n" +
                "        <text><![CDATA[Valor]]></text></staticText>\n" +
                "    <staticText><reportElement x=\"220\" y=\"0\" width=\"100\" height=\"20\"/>\n" +
                "        <text><![CDATA[Forma Pgto]]></text></staticText>\n" +
                "    <staticText><reportElement x=\"320\" y=\"0\" width=\"80\" height=\"20\"/>\n" +
                "        <text><![CDATA[Status]]></text></staticText>\n" +
                "    <staticText><reportElement x=\"400\" y=\"0\" width=\"115\" height=\"20\"/>\n" +
                "        <text><![CDATA[Operador]]></text></staticText>\n" +
                "</band></columnHeader>\n" +
                "<detail><band height=\"15\">\n" +
                "    <textField><reportElement x=\"0\" y=\"0\" width=\"40\" height=\"15\"/>\n" +
                "        <textFieldExpression><![CDATA[$F{id}]]></textFieldExpression>\n" +
                "    </textField>\n" +
                "    <textField><reportElement x=\"40\" y=\"0\" width=\"100\" height=\"15\"/>\n" +
                "        <textFieldExpression><![CDATA[$F{data}]]></textFieldExpression>\n" +
                "    </textField>\n" +
                "    <textField><reportElement x=\"140\" y=\"0\" width=\"80\" height=\"15\"/>\n" +
                "        <textFieldExpression><![CDATA[$F{valor}]]></textFieldExpression>\n" +
                "    </textField>\n" +
                "    <textField><reportElement x=\"220\" y=\"0\" width=\"100\" height=\"15\"/>\n" +
                "        <textFieldExpression><![CDATA[$F{formaPagamento}]]></textFieldExpression>\n" +
                "    </textField>\n" +
                "    <textField><reportElement x=\"320\" y=\"0\" width=\"80\" height=\"15\"/>\n" +
                "        <textFieldExpression><![CDATA[$F{status}]]></textFieldExpression>\n" +
                "    </textField>\n" +
                "    <textField><reportElement x=\"400\" y=\"0\" width=\"115\" height=\"15\"/>\n" +
                "        <textFieldExpression><![CDATA[$F{operador}]]></textFieldExpression>\n" +
                "    </textField>\n" +
                "</band></detail>\n" +
                "</jasperReport>";
    }
}
