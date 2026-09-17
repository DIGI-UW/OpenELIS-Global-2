package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.inventory.report.InventoryReportRequest;
import org.openelisglobal.inventory.report.InventoryReportService;
import org.openelisglobal.inventory.report.InventoryReportWriter;
import org.openelisglobal.inventory.report.ReportTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backs {@code InventoryReports.jsx}'s "Generate" button. See
 * {@link InventoryReportService} for what each report type queries.
 */
@RestController
public class InventoryReportRestController {

    private static final Set<String> VALID_REPORT_TYPES = Set.of("STOCK_LEVELS", "EXPIRATION_FORECAST", "USAGE_TRENDS",
            "LOT_TRACEABILITY", "LOW_STOCK", "TRANSACTION_HISTORY");
    private static final Set<String> VALID_EXPORT_FORMATS = Set.of("PDF", "EXCEL", "CSV");

    @Autowired
    private InventoryReportService inventoryReportService;

    @PostMapping("/rest/inventory/reports/generate")
    public void generate(@RequestParam String reportType, @RequestParam String exportFormat,
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false, defaultValue = "true") boolean includeExpired,
            @RequestParam(required = false, defaultValue = "false") boolean groupByType,
            @RequestParam(required = false, defaultValue = "false") boolean groupByLocation,
            HttpServletResponse response) throws IOException {
        try {
            if (!VALID_REPORT_TYPES.contains(reportType)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown report type: " + reportType);
                return;
            }
            if (!VALID_EXPORT_FORMATS.contains(exportFormat)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown export format: " + exportFormat);
                return;
            }

            ReportTable table;
            try {
                InventoryReportRequest request = new InventoryReportRequest(reportType, exportFormat,
                        parseStartDate(startDate), parseEndDate(endDate), includeInactive, includeExpired, groupByType,
                        groupByLocation);
                table = inventoryReportService.generateReport(request);
            } catch (LocalizedValidationException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
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
