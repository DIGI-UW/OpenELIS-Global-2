package org.openelisglobal.biorepository.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for exporting biorepository data in multiple formats.
 *
 * Aligns with BiorepositoryDashboardService and ChainOfCustodyService APIs.
 */
@Service
public class BiorepositoryExportServiceImpl implements BiorepositoryExportService {

    @Autowired
    private BiorepositoryDashboardService dashboardService;

    @Autowired
    private ChainOfCustodyService custodyService;

    private static final String CSV_SEPARATOR = ",";
    private static final String CSV_QUOTE = "\"";

    // ==================== Dashboard Exports ====================

    @Override
    @Transactional(readOnly = true)
    public byte[] exportDashboardToCSV() throws IOException {
        Map<String, Object> dashboardData = aggregateDashboardMetrics();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        // Write header
        writer.println("Metric Category,Metric Name,Value");

        // Storage Capacity
        Map<String, Object> capacity = (Map<String, Object>) dashboardData.get("storageCapacity");
        if (capacity != null) {
            writeCSVRow(writer, "Storage Capacity", "Total Devices", capacity.get("totalDevices"));
            writeCSVRow(writer, "Storage Capacity", "Total Samples Stored", capacity.get("totalSamplesStored"));
            writeCSVRow(writer, "Storage Capacity", "Pending Storage", capacity.get("pendingStorage"));
            writeCSVRow(writer, "Storage Capacity", "Average Utilization %", capacity.get("averageUtilization"));
        }

        // Sample Aging
        Map<String, Object> aging = (Map<String, Object>) dashboardData.get("sampleAging");
        if (aging != null) {
            writeCSVRow(writer, "Sample Aging", "Total Active Samples", aging.get("total"));
            writeCSVRow(writer, "Sample Aging", "Expiring within 30 days", aging.get("expiring30Days"));
            writeCSVRow(writer, "Sample Aging", "Expiring within 60 days", aging.get("expiring60Days"));
            writeCSVRow(writer, "Sample Aging", "Expiring within 90 days", aging.get("expiring90Days"));
            writeCSVRow(writer, "Sample Aging", "Expired Samples", aging.get("expired"));
        }

        // QC Compliance
        Map<String, Object> qc = (Map<String, Object>) dashboardData.get("qcCompliance");
        if (qc != null) {
            writeCSVRow(writer, "QC Compliance", "Total Inspections", qc.get("totalInspections"));
            writeCSVRow(writer, "QC Compliance", "Passed Inspections", qc.get("passedInspections"));
            writeCSVRow(writer, "QC Compliance", "Failed Inspections", qc.get("failedInspections"));
            writeCSVRow(writer, "QC Compliance", "Compliance Rate %", qc.get("complianceRate"));
        }

        // Retrieval Statistics
        Map<String, Object> retrieval = (Map<String, Object>) dashboardData.get("retrievalStats");
        if (retrieval != null) {
            writeCSVRow(writer, "Retrieval Statistics", "Total Requests", retrieval.get("totalRequests"));
            writeCSVRow(writer, "Retrieval Statistics", "Pending Requests", retrieval.get("pendingRequests"));
            writeCSVRow(writer, "Retrieval Statistics", "Rejected Requests", retrieval.get("rejectedRequests"));
            writeCSVRow(writer, "Retrieval Statistics", "Completed Requests", retrieval.get("completedRequests"));
        }

        writer.flush();
        return baos.toByteArray();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportDashboardToExcel() throws IOException {
        Map<String, Object> dashboardData = aggregateDashboardMetrics();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Dashboard Metrics");

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Create header row
        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "Metric Category", headerStyle);
        createCell(headerRow, 1, "Metric Name", headerStyle);
        createCell(headerRow, 2, "Value", headerStyle);

        int rowNum = 1;

        // Storage Capacity
        Map<String, Object> capacity = (Map<String, Object>) dashboardData.get("storageCapacity");
        if (capacity != null) {
            rowNum = writeExcelRow(sheet, rowNum, "Storage Capacity", "Total Devices", capacity.get("totalDevices"));
            rowNum = writeExcelRow(sheet, rowNum, "Storage Capacity", "Total Samples Stored",
                    capacity.get("totalSamplesStored"));
            rowNum = writeExcelRow(sheet, rowNum, "Storage Capacity", "Pending Storage",
                    capacity.get("pendingStorage"));
            rowNum = writeExcelRow(sheet, rowNum, "Storage Capacity", "Average Utilization %",
                    capacity.get("averageUtilization"));
        }

        // Sample Aging
        Map<String, Object> aging = (Map<String, Object>) dashboardData.get("sampleAging");
        if (aging != null) {
            rowNum = writeExcelRow(sheet, rowNum, "Sample Aging", "Total Active Samples", aging.get("total"));
            rowNum = writeExcelRow(sheet, rowNum, "Sample Aging", "Expiring within 30 days",
                    aging.get("expiring30Days"));
            rowNum = writeExcelRow(sheet, rowNum, "Sample Aging", "Expiring within 60 days",
                    aging.get("expiring60Days"));
            rowNum = writeExcelRow(sheet, rowNum, "Sample Aging", "Expiring within 90 days",
                    aging.get("expiring90Days"));
            rowNum = writeExcelRow(sheet, rowNum, "Sample Aging", "Expired Samples", aging.get("expired"));
        }

        // QC Compliance
        Map<String, Object> qc = (Map<String, Object>) dashboardData.get("qcCompliance");
        if (qc != null) {
            rowNum = writeExcelRow(sheet, rowNum, "QC Compliance", "Total Inspections", qc.get("totalInspections"));
            rowNum = writeExcelRow(sheet, rowNum, "QC Compliance", "Passed Inspections", qc.get("passedInspections"));
            rowNum = writeExcelRow(sheet, rowNum, "QC Compliance", "Failed Inspections", qc.get("failedInspections"));
            rowNum = writeExcelRow(sheet, rowNum, "QC Compliance", "Compliance Rate %", qc.get("complianceRate"));
        }

        // Retrieval Statistics
        Map<String, Object> retrieval = (Map<String, Object>) dashboardData.get("retrievalStats");
        if (retrieval != null) {
            rowNum = writeExcelRow(sheet, rowNum, "Retrieval Statistics", "Total Requests",
                    retrieval.get("totalRequests"));
            rowNum = writeExcelRow(sheet, rowNum, "Retrieval Statistics", "Pending Requests",
                    retrieval.get("pendingRequests"));
            rowNum = writeExcelRow(sheet, rowNum, "Retrieval Statistics", "Rejected Requests",
                    retrieval.get("rejectedRequests"));
            rowNum = writeExcelRow(sheet, rowNum, "Retrieval Statistics", "Completed Requests",
                    retrieval.get("completedRequests"));
        }

        // Auto-size columns
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportDashboardToJSON() throws IOException {
        Map<String, Object> dashboardData = aggregateDashboardMetrics();

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper.writeValueAsBytes(dashboardData);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportDashboardToPDF() throws IOException {
        Map<String, Object> dashboardData = aggregateDashboardMetrics();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font bodyFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
            Paragraph title = new Paragraph("Biorepository Dashboard Metrics", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(16f);
            document.add(title);

            Map<String, Object> capacity = castMap(dashboardData.get("storageCapacity"));
            Map<String, Object> aging = castMap(dashboardData.get("sampleAging"));
            Map<String, Object> qc = castMap(dashboardData.get("qcCompliance"));
            Map<String, Object> retrieval = castMap(dashboardData.get("retrievalStats"));

            addMetricTable(document, "Storage Capacity", capacity, bodyFont, sectionFont, List.of(
                    metricRow("Total Devices", capacity.get("totalDevices")),
                    metricRow("Total Samples Stored", capacity.get("totalSamplesStored")),
                    metricRow("Pending Storage", capacity.get("pendingStorage")),
                    metricRow("Average Utilization %", capacity.get("averageUtilization"))));

            addMetricTable(document, "Sample Aging", aging, bodyFont, sectionFont, List.of(
                    metricRow("Total Active Samples", aging.get("total")),
                    metricRow("Expiring within 30 days", aging.get("expiring30Days")),
                    metricRow("Expiring within 60 days", aging.get("expiring60Days")),
                    metricRow("Expiring within 90 days", aging.get("expiring90Days")),
                    metricRow("Expired Samples", aging.get("expired"))));

            addMetricTable(document, "QC Compliance", qc, bodyFont, sectionFont, List.of(
                    metricRow("Total Inspections", qc.get("totalInspections")),
                    metricRow("Passed Inspections", qc.get("passedInspections")),
                    metricRow("Failed Inspections", qc.get("failedInspections")),
                    metricRow("Compliance Rate %", qc.get("complianceRate"))));

            addMetricTable(document, "Retrieval Statistics", retrieval, bodyFont, sectionFont, List.of(
                    metricRow("Total Requests", retrieval.get("totalRequests")),
                    metricRow("Pending Requests", retrieval.get("pendingRequests")),
                    metricRow("Rejected Requests", retrieval.get("rejectedRequests")),
                    metricRow("Completed Requests", retrieval.get("completedRequests"))));

            document.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("Failed to generate dashboard PDF export", e);
        }
    }

    // ==================== Audit Trail Exports ====================

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAuditTrailToCSV(String sampleExternalId, CustodyAction action, Integer custodianId,
            Timestamp startDate, Timestamp endDate) throws IOException {

        List<ChainOfCustodyLog> logs = custodyService.searchCustodyLogs(sampleExternalId, action, custodianId,
                startDate, endDate, 0, Integer.MAX_VALUE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        // Write header
        writer.println(
                "Timestamp,Sample Barcode,Accession Number,Custody Action,From Location,To Location,From Custodian,To Custodian,Storage Coordinates,Temperature (°C),Notes");

        // Write data rows
        for (ChainOfCustodyLog log : logs) {
            StringBuilder row = new StringBuilder();
            row.append(escapeCSV(log.getActionTimestamp().toString())).append(CSV_SEPARATOR);

            String sampleBarcode = (log.getSampleItem() != null && log.getSampleItem().getExternalId() != null)
                    ? log.getSampleItem().getExternalId()
                    : "";
            row.append(escapeCSV(sampleBarcode)).append(CSV_SEPARATOR);

            String accessionNumber = (log.getSampleItem() != null && log.getSampleItem().getSample() != null
                    && log.getSampleItem().getSample().getAccessionNumber() != null)
                            ? log.getSampleItem().getSample().getAccessionNumber()
                            : "";
            row.append(escapeCSV(accessionNumber)).append(CSV_SEPARATOR);

            row.append(escapeCSV(log.getCustodyAction().name())).append(CSV_SEPARATOR);
            row.append(escapeCSV(log.getFromLocation() != null ? log.getFromLocation() : "")).append(CSV_SEPARATOR);
            row.append(escapeCSV(log.getToLocation() != null ? log.getToLocation() : "")).append(CSV_SEPARATOR);
            row.append(escapeCSV(log.getFromCustodian() != null ? log.getFromCustodian().getNameForDisplay() : ""))
                    .append(CSV_SEPARATOR);
            row.append(escapeCSV(log.getToCustodian() != null ? log.getToCustodian().getNameForDisplay() : ""))
                    .append(CSV_SEPARATOR);
            row.append(escapeCSV(log.getStorageCoordinates() != null ? log.getStorageCoordinates() : ""))
                    .append(CSV_SEPARATOR);
            row.append(escapeCSV(log.getTemperature() != null ? log.getTemperature().toString() : ""))
                    .append(CSV_SEPARATOR);
            row.append(escapeCSV(log.getNotes() != null ? log.getNotes() : ""));

            writer.println(row.toString());
        }

        writer.flush();
        return baos.toByteArray();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAuditTrailToExcel(String sampleExternalId, CustodyAction action, Integer custodianId,
            Timestamp startDate, Timestamp endDate) throws IOException {

        List<ChainOfCustodyLog> logs = custodyService.searchCustodyLogs(sampleExternalId, action, custodianId,
                startDate, endDate, 0, Integer.MAX_VALUE);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Audit Trail");

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Create header row
        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "Timestamp", headerStyle);
        createCell(headerRow, 1, "Sample Barcode", headerStyle);
        createCell(headerRow, 2, "Accession Number", headerStyle);
        createCell(headerRow, 3, "Custody Action", headerStyle);
        createCell(headerRow, 4, "From Location", headerStyle);
        createCell(headerRow, 5, "To Location", headerStyle);
        createCell(headerRow, 6, "From Custodian", headerStyle);
        createCell(headerRow, 7, "To Custodian", headerStyle);
        createCell(headerRow, 8, "Storage Coordinates", headerStyle);
        createCell(headerRow, 9, "Temperature (°C)", headerStyle);
        createCell(headerRow, 10, "Notes", headerStyle);

        int rowNum = 1;
        for (ChainOfCustodyLog log : logs) {
            Row row = sheet.createRow(rowNum++);

            createCell(row, 0, log.getActionTimestamp().toString(), null);

            String sampleBarcode = (log.getSampleItem() != null && log.getSampleItem().getExternalId() != null)
                    ? log.getSampleItem().getExternalId()
                    : "";
            createCell(row, 1, sampleBarcode, null);

            String accessionNumber = (log.getSampleItem() != null && log.getSampleItem().getSample() != null
                    && log.getSampleItem().getSample().getAccessionNumber() != null)
                            ? log.getSampleItem().getSample().getAccessionNumber()
                            : "";
            createCell(row, 2, accessionNumber, null);

            createCell(row, 3, log.getCustodyAction().name(), null);
            createCell(row, 4, log.getFromLocation() != null ? log.getFromLocation() : "", null);
            createCell(row, 5, log.getToLocation() != null ? log.getToLocation() : "", null);
            createCell(row, 6, log.getFromCustodian() != null ? log.getFromCustodian().getNameForDisplay() : "", null);
            createCell(row, 7, log.getToCustodian() != null ? log.getToCustodian().getNameForDisplay() : "", null);
            createCell(row, 8, log.getStorageCoordinates() != null ? log.getStorageCoordinates() : "", null);
            createCell(row, 9, log.getTemperature() != null ? log.getTemperature().toString() : "", null);
            createCell(row, 10, log.getNotes() != null ? log.getNotes() : "", null);
        }

        // Auto-size columns
        for (int i = 0; i < 11; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAuditTrailToJSON(String sampleExternalId, CustodyAction action, Integer custodianId,
            Timestamp startDate, Timestamp endDate) throws IOException {

        List<ChainOfCustodyLog> logs = custodyService.searchCustodyLogs(sampleExternalId, action, custodianId,
                startDate, endDate, 0, Integer.MAX_VALUE);

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChainOfCustodyLog log : logs) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("actionTimestamp", log.getActionTimestamp().toString());
            map.put("custodyAction", log.getCustodyAction().name());

            if (log.getSampleItem() != null) {
                if (log.getSampleItem().getExternalId() != null) {
                    map.put("sampleExternalId", log.getSampleItem().getExternalId());
                }
                if (log.getSampleItem().getSample() != null
                        && log.getSampleItem().getSample().getAccessionNumber() != null) {
                    map.put("accessionNumber", log.getSampleItem().getSample().getAccessionNumber());
                }
            }

            if (log.getFromLocation() != null) {
                map.put("fromLocation", log.getFromLocation());
            }
            if (log.getToLocation() != null) {
                map.put("toLocation", log.getToLocation());
            }
            if (log.getFromCustodian() != null) {
                map.put("fromCustodian", log.getFromCustodian().getNameForDisplay());
            }
            if (log.getToCustodian() != null) {
                map.put("toCustodian", log.getToCustodian().getNameForDisplay());
            }
            if (log.getStorageCoordinates() != null) {
                map.put("storageCoordinates", log.getStorageCoordinates());
            }
            if (log.getTemperature() != null) {
                map.put("temperature", log.getTemperature());
            }
            if (log.getNotes() != null) {
                map.put("notes", log.getNotes());
            }

            result.add(map);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper.writeValueAsBytes(result);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAuditTrailToPDF(String sampleExternalId, CustodyAction action, Integer custodianId,
            Timestamp startDate, Timestamp endDate) throws IOException {
        List<ChainOfCustodyLog> logs = custodyService.searchCustodyLogs(sampleExternalId, action, custodianId,
                startDate, endDate, 0, Integer.MAX_VALUE);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
            Font bodyFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
            Paragraph title = new Paragraph("Biorepository Audit Trail Export", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10f);
            document.add(title);
            document.add(new Paragraph("Records: " + logs.size(), bodyFont));
            document.add(new Paragraph("Generated: " + Timestamp.valueOf(java.time.LocalDateTime.now()), bodyFont));
            document.add(new Paragraph(" ", bodyFont));

            PdfPTable table = new PdfPTable(new float[] { 1.6f, 1.4f, 1.8f, 1.4f, 1.6f, 1.6f, 1.8f, 1.0f, 1.8f });
            table.setWidthPercentage(100f);
            addHeaderCell(table, "Timestamp", headerFont);
            addHeaderCell(table, "Barcode", headerFont);
            addHeaderCell(table, "Accession", headerFont);
            addHeaderCell(table, "Action", headerFont);
            addHeaderCell(table, "From", headerFont);
            addHeaderCell(table, "To", headerFont);
            addHeaderCell(table, "Custodian", headerFont);
            addHeaderCell(table, "Temp (C)", headerFont);
            addHeaderCell(table, "Notes", headerFont);

            for (ChainOfCustodyLog log : logs) {
                addBodyCell(table, log.getActionTimestamp() != null ? log.getActionTimestamp().toString() : "", bodyFont);
                addBodyCell(table,
                        log.getSampleItem() != null && log.getSampleItem().getExternalId() != null
                                ? log.getSampleItem().getExternalId()
                                : "",
                        bodyFont);
                addBodyCell(table,
                        log.getSampleItem() != null && log.getSampleItem().getSample() != null
                                && log.getSampleItem().getSample().getAccessionNumber() != null
                                        ? log.getSampleItem().getSample().getAccessionNumber()
                                        : "",
                        bodyFont);
                addBodyCell(table, log.getCustodyAction() != null ? log.getCustodyAction().name() : "", bodyFont);
                addBodyCell(table, log.getFromLocation() != null ? log.getFromLocation() : "", bodyFont);
                addBodyCell(table, log.getToLocation() != null ? log.getToLocation() : "", bodyFont);
                String custodian = log.getToCustodian() != null ? log.getToCustodian().getNameForDisplay()
                        : (log.getFromCustodian() != null ? log.getFromCustodian().getNameForDisplay() : "");
                addBodyCell(table, custodian, bodyFont);
                addBodyCell(table, log.getTemperature() != null ? log.getTemperature().toString() : "", bodyFont);
                addBodyCell(table, log.getNotes() != null ? log.getNotes() : "", bodyFont);
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("Failed to generate audit trail PDF export", e);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Aggregate dashboard metrics from dashboard service.
     */
    private Map<String, Object> aggregateDashboardMetrics() {
        Map<String, Object> result = new HashMap<>();

        result.put("storageCapacity", dashboardService.getStorageCapacityMetrics());
        result.put("sampleAging", dashboardService.getSampleAgingMetrics());
        result.put("qcCompliance", dashboardService.getQCComplianceMetrics());
        result.put("retrievalStats", dashboardService.getRetrievalStatistics(null, null));

        return result;
    }

    /**
     * Write a single CSV row for dashboard metrics.
     */
    private void writeCSVRow(PrintWriter writer, String category, String metric, Object value) {
        writer.println(escapeCSV(category) + CSV_SEPARATOR + escapeCSV(metric) + CSV_SEPARATOR
                + escapeCSV(String.valueOf(value)));
    }

    /**
     * Write a single Excel row for dashboard metrics.
     */
    private int writeExcelRow(Sheet sheet, int rowNum, String category, String metric, Object value) {
        Row row = sheet.createRow(rowNum);
        createCell(row, 0, category, null);
        createCell(row, 1, metric, null);
        createCell(row, 2, String.valueOf(value), null);
        return rowNum + 1;
    }

    /**
     * Escape CSV special characters.
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(CSV_SEPARATOR) || value.contains(CSV_QUOTE) || value.contains("\n")) {
            return CSV_QUOTE + value.replace(CSV_QUOTE, CSV_QUOTE + CSV_QUOTE) + CSV_QUOTE;
        }
        return value;
    }

    /**
     * Create Excel cell with value and style.
     */
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    private String[] metricRow(String metric, Object value) {
        return new String[] { metric, value != null ? String.valueOf(value) : "" };
    }

    private void addMetricTable(Document document, String sectionTitle, Map<String, Object> source, Font bodyFont,
            Font sectionFont, List<String[]> rows) throws DocumentException {
        document.add(new Paragraph(sectionTitle, sectionFont));
        PdfPTable table = new PdfPTable(new float[] { 2.2f, 1.2f });
        table.setWidthPercentage(100f);
        for (String[] row : rows) {
            addBodyCell(table, row[0], bodyFont);
            addBodyCell(table, row[1], bodyFont);
        }
        table.setSpacingAfter(10f);
        if (!source.isEmpty()) {
            document.add(table);
        } else {
            document.add(new Paragraph("No data available", bodyFont));
        }
    }

    private void addHeaderCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", font));
        table.addCell(cell);
    }
}
