package org.openelisglobal.coldstorage.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.openelisglobal.coldstorage.service.FreezerReportService;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/coldstorage/reports")
public class FreezerReportController {

    @Autowired
    private FreezerReportService freezerReportService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_PDF_VALUE)
    public void generateReport(@RequestBody ReportRequest request, HttpServletResponse response) {
        try {
            LogEvent.logInfo(this.getClass().getSimpleName(), "generateReport",
                    "Generating report - Type: " + request.getReportType() + ", FreezerId: " + request.getFreezerId()
                            + ", StartDate: " + request.getStartDate() + ", EndDate: " + request.getEndDate());

            LocalDate startDate = LocalDate.parse(request.getStartDate(), DATE_FORMATTER);
            LocalDate endDate = LocalDate.parse(request.getEndDate(), DATE_FORMATTER);

            Long freezerId = null;
            if (request.getFreezerId() != null && !request.getFreezerId().isEmpty()
                    && !request.getFreezerId().equals("All Freezers")) {
                try {
                    freezerId = Long.parseLong(request.getFreezerId());
                } catch (NumberFormatException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "generateReport",
                            "Invalid freezerId format: " + request.getFreezerId());
                }
            }

            byte[] pdfBytes = freezerReportService.generatePdfReport(request.getReportType(), freezerId, startDate,
                    endDate);

            if (pdfBytes == null || pdfBytes.length == 0) {
                LogEvent.logError(this.getClass().getSimpleName(), "generateReport",
                        "PDF generation returned null or empty bytes");
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate report");
                } catch (IOException ioe) {
                    LogEvent.logError(this.getClass().getSimpleName(), "generateReport",
                            "Failed to send error response: " + ioe.getMessage());
                }
                return;
            }

            // Write directly to response output stream
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + generateFilename(request.getReportType(), startDate, endDate) + "\"");
            response.setContentLength(pdfBytes.length);

            try {
                response.getOutputStream().write(pdfBytes);
                response.getOutputStream().flush();
                LogEvent.logInfo(this.getClass().getSimpleName(), "generateReport",
                        "Successfully wrote PDF to response: " + pdfBytes.length + " bytes");
            } catch (IOException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "generateReport",
                        "Failed to write PDF to response stream: " + e.getMessage());
                throw e;
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "generateReport",
                    "Error in report generation: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            if (!response.isCommitted()) {
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Failed to generate report: " + e.getMessage());
                } catch (IOException ioe) {
                    LogEvent.logError(this.getClass().getSimpleName(), "generateReport",
                            "Failed to send error response: " + ioe.getMessage());
                }
            }
        }
    }

    private String generateFilename(String reportType, LocalDate startDate, LocalDate endDate) {
        String type = reportType.toLowerCase().replace(" ", "_");
        return String.format("freezer_report_%s_%s_to_%s.pdf", type, startDate.format(DATE_FORMATTER),
                endDate.format(DATE_FORMATTER));
    }

    public static class ReportRequest {
        private String reportType;
        private String freezerId;
        private String startDate;
        private String endDate;

        public String getReportType() {
            return reportType;
        }

        public void setReportType(String reportType) {
            this.reportType = reportType;
        }

        public String getFreezerId() {
            return freezerId;
        }

        public void setFreezerId(String freezerId) {
            this.freezerId = freezerId;
        }

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }
    }
}
