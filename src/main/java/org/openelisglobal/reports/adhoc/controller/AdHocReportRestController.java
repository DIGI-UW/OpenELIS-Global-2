package org.openelisglobal.reports.adhoc.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.reports.adhoc.dto.AdHocReportDefinitionDTO;
import org.openelisglobal.reports.adhoc.dto.AdHocReportResultDTO;
import org.openelisglobal.reports.adhoc.dto.AvailableFieldsDTO;
import org.openelisglobal.reports.adhoc.service.AdHocFieldDefinitionService;
import org.openelisglobal.reports.adhoc.service.AdHocReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/adhoc-report")
public class AdHocReportRestController {

    @Autowired
    private AdHocFieldDefinitionService fieldDefinitionService;

    @Autowired
    private AdHocReportService reportService;

    @GetMapping("/fields")
    public ResponseEntity<AvailableFieldsDTO> getAvailableFields() {
        try {
            AvailableFieldsDTO fields = fieldDefinitionService.getAvailableFields();
            return ResponseEntity.ok(fields);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getAvailableFields",
                    "Error retrieving available fields: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/preview")
    public ResponseEntity<?> previewReport(@RequestBody AdHocReportDefinitionDTO definition) {
        try {
            reportService.validateReportDefinition(definition);

            if (definition.getLimit() == null || definition.getLimit() > 100) {
                definition.setLimit(100);
            }

            AdHocReportResultDTO result = reportService.executeReport(definition);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "previewReport", "Validation error: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "previewReport",
                    "Error executing report preview: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error generating report preview"));
        }
    }

    @PostMapping(value = "/generate-pdf", produces = "application/pdf")
    public void generatePdfReport(@RequestBody AdHocReportDefinitionDTO definition, HttpServletResponse response) {
        try {
            reportService.validateReportDefinition(definition);

            byte[] pdfBytes = reportService.generatePdfReport(definition);
            String filename = buildFilename(definition);

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(pdfBytes.length);
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();

            LogEvent.logInfo(this.getClass().getSimpleName(), "generatePdfReport",
                    "Generated PDF report: " + filename + " (" + pdfBytes.length + " bytes)");

        } catch (IllegalArgumentException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "generatePdfReport",
                    "Validation error: " + e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "generatePdfReport",
                    "Error generating PDF report: " + e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating PDF report");
        }
    }

    private String buildFilename(AdHocReportDefinitionDTO definition) {
        String title = definition.getReportTitle();
        if (title == null || title.trim().isEmpty()) {
            title = "adhoc-report";
        } else {
            title = title.replaceAll("[^a-zA-Z0-9\\-_]", "_").toLowerCase();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return title + "_" + timestamp + ".pdf";
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message) {
        try {
            response.setStatus(status);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"" + message.replace("\"", "\\\"") + "\"}");
        } catch (Exception ex) {
            LogEvent.logError(this.getClass().getSimpleName(), "writeErrorResponse", "Error writing error response");
        }
    }

    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
