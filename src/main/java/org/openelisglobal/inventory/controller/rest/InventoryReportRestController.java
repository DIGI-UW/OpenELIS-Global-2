package org.openelisglobal.inventory.controller.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.inventory.report.InventoryReportRequest;
import org.openelisglobal.inventory.report.InventoryReportService;
import org.openelisglobal.inventory.report.InventoryReportWriter;
import org.openelisglobal.inventory.report.ReportTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backs {@code InventoryReports.jsx}'s "Generate" button. See
 * {@link InventoryReportService} for what each report type queries.
 */
@RestController
public class InventoryReportRestController {

    private static final Set<String> VALID_EXPORT_FORMATS = Set.of("PDF", "EXCEL", "CSV");

    @Autowired
    private InventoryReportService inventoryReportService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/rest/inventory/reports/generate")
    public void generate(@RequestParam String reportType, @RequestParam String exportFormat,
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false, defaultValue = "true") boolean includeExpired,
            @RequestParam(required = false, defaultValue = "false") boolean groupByType,
            @RequestParam(required = false, defaultValue = "false") boolean groupByLocation,
            HttpServletResponse response) throws IOException {
        try {
            ReportTable table;
            try {
                if (!VALID_EXPORT_FORMATS.contains(exportFormat)) {
                    throw new LocalizedValidationException("reports.error.unknownExportFormat",
                            "Unknown export format: " + exportFormat);
                }
                InventoryReportRequest request = new InventoryReportRequest(reportType, exportFormat,
                        parseStartDate(startDate), parseEndDate(endDate), includeInactive, includeExpired, groupByType,
                        groupByLocation);
                table = inventoryReportService.generateReport(request);
            } catch (LocalizedValidationException e) {
                sendValidationError(response, e);
                return;
            }

            String filenameBase = reportType.toLowerCase().replace('_', '-');
            switch (exportFormat) {
            case "CSV":
                response.setContentType("text/csv");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + filenameBase + ".csv\"");
                InventoryReportWriter.writeCsv(table, response.getOutputStream());
                break;
            case "PDF":
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + filenameBase + ".pdf\"");
                InventoryReportWriter.writePdf(table, response.getOutputStream());
                break;
            case "EXCEL":
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + filenameBase + ".xlsx\"");
                InventoryReportWriter.writeExcel(table, response.getOutputStream());
                break;
            default:
                // VALID_EXPORT_FORMATS admits no other value.
                break;
            }
        } catch (Exception e) {
            LogEvent.logError(e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating report");
            }
        }
    }

    /**
     * Same {message, errorCode, params} body as
     * {@link InventoryItemRestController}.
     */
    private void sendValidationError(HttpServletResponse response, LocalizedValidationException e) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("message", e.getMessage());
        body.put("errorCode", e.getErrorCode());
        body.put("params", e.getParams());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private Timestamp parseStartDate(String value) {
        LocalDate date = parseLocalDate(value);
        return date == null ? null : Timestamp.valueOf(date.atStartOfDay());
    }

    /**
     * The consuming queries use an inclusive {@code BETWEEN}, so an end date left
     * at midnight would drop the whole last day.
     */
    private Timestamp parseEndDate(String value) {
        LocalDate date = parseLocalDate(value);
        return date == null ? null : Timestamp.valueOf(date.atTime(23, 59, 59, 999_000_000));
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new LocalizedValidationException("reports.error.invalidDate", "Invalid date: " + value);
        }
    }
}
