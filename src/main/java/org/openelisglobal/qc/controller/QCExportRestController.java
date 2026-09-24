package org.openelisglobal.qc.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.common.util.PdfExportSupport.ExportWindow;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.qa.security.QaPermissions;
import org.openelisglobal.qc.report.QCBenchExportService;
import org.openelisglobal.qc.report.QCBenchExportService.BenchExport;
import org.openelisglobal.qc.report.QCExportWriter;
import org.openelisglobal.qc.service.QCChartDataService;
import org.openelisglobal.qc.service.QCChartDataService.QCExportModel;
import org.openelisglobal.qc.valueholder.QCSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * QC inspector export (OGC-706): CSV run/violation detail and a formatted PDF
 * (embedded Levey-Jennings charts + sigma tables) for a single instrument over
 * a date window, optionally narrowed to a test and/or control level.
 *
 * <p>
 * Follows the established tabular-export precedent (E-Sig Log, OGC-703): iText
 * 5 + {@link StringUtil#csvEscape}, not JasperReports. Data is assembled inside
 * a read transaction by {@link QCChartDataService#getExportModel} and
 * {@link QCBenchExportService}, and rendered by {@link QCExportWriter}; what is
 * left here is parameter parsing and the response headers.
 *
 * <p>
 * This is the first {@code @PreAuthorize} on the {@code /rest/qc/*} surface —
 * the rest of it is currently ungated (tracked as a follow-up finding).
 */
@RestController
@RequestMapping("/rest/qc/export")
public class QCExportRestController {

    @Autowired
    private QCChartDataService chartDataService;

    @Autowired
    private QCBenchExportService benchExportService;

    // Injected rather than ConfigurationProperties.getInstance() so test slices
    // don't have to register the static SpringContext holder (mirrors esig).
    @Autowired
    private ConfigurationProperties configurationProperties;

    @GetMapping("/csv")
    @PreAuthorize(QaPermissions.VIEW_QC)
    public void exportCsv(@RequestParam String instrumentId, @RequestParam(required = false) String testId,
            @RequestParam(required = false) String controlLevel, @RequestParam String startDate,
            @RequestParam String endDate, HttpServletResponse response) throws IOException {

        ExportWindow window = PdfExportSupport.parseWindow(startDate, endDate, "startDate", "endDate");
        QCExportModel model = chartDataService.getExportModel(instrumentId, testId, controlLevel, window.start(),
                window.end(), PdfExportSupport.MAX_EXPORT_ROWS);

        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + exportFilename(model.instrumentName(), startDate, endDate, "csv") + "\"");

        QCExportWriter.writeCsv(model, response.getOutputStream());
    }

    /**
     * The bench QC register: one row per manual or RDT control run in the window
     * (OGC-1147). /rest/qc/export/bench/csv?startDate=&amp;endDate=[&amp;source=]
     *
     * <p>
     * A separate flat export rather than a source option on {@link #exportCsv}:
     * that document is a Westgard review, sectioned per control lot and carrying
     * statistics and sigma per section. An RDT control has no lot and no
     * statistics, so it has no section to occupy — the mismatch is the structure,
     * not just the instrument label. What an assessor wants from bench QC is a
     * register anyway: who ran which control, when, and what it read.
     */
    @GetMapping("/bench/csv")
    @PreAuthorize(QaPermissions.VIEW_QC)
    public void exportBenchCsv(@RequestParam String startDate, @RequestParam String endDate,
            @RequestParam(required = false) String source, HttpServletResponse response) throws IOException {

        ExportWindow window = PdfExportSupport.parseWindow(startDate, endDate, "startDate", "endDate");
        BenchExport bench = benchExportService.getBenchExport(window.start(), window.end(),
                QCSource.parseBenchFilter(source), PdfExportSupport.MAX_EXPORT_ROWS);

        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + exportFilename("bench-qc", startDate, endDate, "csv") + "\"");

        QCExportWriter.writeBenchCsv(bench, response.getOutputStream());
    }

    @GetMapping("/pdf")
    @PreAuthorize(QaPermissions.VIEW_QC)
    public void exportPdf(@RequestParam String instrumentId, @RequestParam(required = false) String testId,
            @RequestParam(required = false) String controlLevel, @RequestParam String startDate,
            @RequestParam String endDate, HttpServletResponse response) throws IOException {

        ExportWindow window = PdfExportSupport.parseWindow(startDate, endDate, "startDate", "endDate");
        QCExportModel model = chartDataService.getExportModel(instrumentId, testId, controlLevel, window.start(),
                window.end(), PdfExportSupport.MAX_EXPORT_ROWS);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + exportFilename(model.instrumentName(), startDate, endDate, "pdf") + "\"");

        QCExportWriter.writePdf(model, PdfExportSupport.labName(configurationProperties), startDate, endDate,
                response.getOutputStream());
    }

    /**
     * A rejected date window or source is the caller's mistake, not a server error.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    /**
     * Descriptive download name: {@code qc-<analyzer>-<start>_<end>.<ext>} (e.g.
     * {@code qc-Cepheid-GeneXpert-ASTM-Mode-2025-09-13_2026-07-20.pdf}). The
     * analyzer name is reduced to filename/header-safe characters.
     */
    private String exportFilename(String instrumentName, String startDate, String endDate, String extension) {
        String analyzer = (instrumentName == null ? "" : instrumentName).replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (analyzer.isEmpty()) {
            analyzer = "instrument";
        }
        return "qc-" + analyzer + "-" + startDate.replaceAll("[^0-9-]", "") + "_" + endDate.replaceAll("[^0-9-]", "")
                + "." + extension;
    }
}
