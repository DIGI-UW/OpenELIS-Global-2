package org.openelisglobal.biorepository.controller.rest;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.openelisglobal.biorepository.service.BiorepositoryExportService;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for biorepository data export operations.
 *
 * Provides endpoints for exporting dashboard metrics and audit trail data in
 * multiple formats: CSV, Excel, JSON, PDF.
 */
@RestController
@RequestMapping(value = "/rest/biorepository")
public class BiorepositoryExportRestController extends BaseRestController {

    @Autowired
    private BiorepositoryExportService exportService;

    /**
     * Export dashboard metrics in specified format.
     *
     * @param format   Export format (csv, excel, json, pdf)
     * @param response HTTP response for file download
     */
    @GetMapping("/dashboard/export/{format}")
    public void exportDashboard(@PathVariable("format") String format, HttpServletResponse response)
            throws IOException {

        byte[] exportData;
        String contentType;
        String fileExtension;

        try {
            switch (format.toLowerCase()) {
            case "csv":
                exportData = exportService.exportDashboardToCSV();
                contentType = "text/csv";
                fileExtension = "csv";
                break;
            case "excel":
                exportData = exportService.exportDashboardToExcel();
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                fileExtension = "xlsx";
                break;
            case "json":
                exportData = exportService.exportDashboardToJSON();
                contentType = "application/json";
                fileExtension = "json";
                break;
            case "pdf":
                exportData = exportService.exportDashboardToPDF();
                contentType = "application/pdf";
                fileExtension = "pdf";
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unsupported format: " + format + ". Supported formats: csv, excel, json, pdf");
            }

            String timestamp = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String filename = "biorepository_dashboard_" + timestamp + "." + fileExtension;

            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(exportData.length);
            response.getOutputStream().write(exportData);
            response.getOutputStream().flush();

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Export failed: " + e.getMessage(), e);
        }
    }

    /**
     * Export audit trail (chain of custody logs) in specified format with optional
     * filters.
     *
     * @param format           Export format (csv, excel, json, pdf)
     * @param sampleExternalId Optional filter by sample barcode
     * @param action           Optional filter by custody action
     * @param custodianId      Optional filter by custodian user ID
     * @param startDate        Optional filter by start date (YYYY-MM-DD)
     * @param endDate          Optional filter by end date (YYYY-MM-DD)
     * @param response         HTTP response for file download
     */
    @GetMapping("/custody/export/{format}")
    public void exportAuditTrail(@PathVariable("format") String format,
            @RequestParam(required = false) String sampleExternalId, @RequestParam(required = false) String action,
            @RequestParam(required = false) Integer custodianId, @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, HttpServletResponse response) throws IOException {

        try {
            // Parse custody action
            CustodyAction custodyAction = null;
            if (action != null && !action.trim().isEmpty() && !"ALL".equalsIgnoreCase(action)) {
                try {
                    custodyAction = CustodyAction.valueOf(action);
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action type: " + action);
                }
            }

            // Parse dates
            java.sql.Timestamp startTimestamp = null;
            java.sql.Timestamp endTimestamp = null;

            if (startDate != null && !startDate.trim().isEmpty()) {
                try {
                    startTimestamp = java.sql.Timestamp.valueOf(java.time.LocalDate
                            .parse(startDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay());
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid startDate format. Expected YYYY-MM-DD");
                }
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                try {
                    endTimestamp = java.sql.Timestamp.valueOf(java.time.LocalDate
                            .parse(endDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE).atTime(23, 59, 59));
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid endDate format. Expected YYYY-MM-DD");
                }
            }

            // Export data
            byte[] exportData;
            String contentType;
            String fileExtension;

            switch (format.toLowerCase()) {
            case "csv":
                exportData = exportService.exportAuditTrailToCSV(sampleExternalId, custodyAction, custodianId,
                        startTimestamp, endTimestamp);
                contentType = "text/csv";
                fileExtension = "csv";
                break;
            case "excel":
                exportData = exportService.exportAuditTrailToExcel(sampleExternalId, custodyAction, custodianId,
                        startTimestamp, endTimestamp);
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                fileExtension = "xlsx";
                break;
            case "json":
                exportData = exportService.exportAuditTrailToJSON(sampleExternalId, custodyAction, custodianId,
                        startTimestamp, endTimestamp);
                contentType = "application/json";
                fileExtension = "json";
                break;
            case "pdf":
                exportData = exportService.exportAuditTrailToPDF(sampleExternalId, custodyAction, custodianId,
                        startTimestamp, endTimestamp);
                contentType = "application/pdf";
                fileExtension = "pdf";
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unsupported format: " + format + ". Supported formats: csv, excel, json, pdf");
            }

            String timestamp = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String filename = sampleExternalId != null && !sampleExternalId.trim().isEmpty()
                    ? "audit_trail_" + sampleExternalId + "_" + timestamp + "." + fileExtension
                    : "audit_trail_" + timestamp + "." + fileExtension;

            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(exportData.length);
            response.getOutputStream().write(exportData);
            response.getOutputStream().flush();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Export failed: " + e.getMessage(), e);
        }
    }
}
