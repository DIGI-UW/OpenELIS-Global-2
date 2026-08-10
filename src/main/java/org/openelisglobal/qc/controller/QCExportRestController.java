package org.openelisglobal.qc.controller;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.qc.service.QCChartDataService;
import org.openelisglobal.qc.service.QCChartDataService.LotSection;
import org.openelisglobal.qc.service.QCChartDataService.QCExportModel;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.qc.service.SigmaMetrics;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCSource;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * 5 + {@link StringUtil#csvEscape}, not JasperReports. The L-J chart is
 * rendered server-side with JFreeChart (already a dependency) rather than
 * round-tripping the browser SVG. Data is assembled by
 * {@link QCChartDataService#getExportModel} inside its read transaction.
 *
 * <p>
 * This is the first {@code @PreAuthorize} on the {@code /rest/qc/*} surface —
 * the rest of it is currently ungated (tracked as a follow-up finding).
 */
@RestController
@RequestMapping("/rest/qc/export")
public class QCExportRestController {

    /**
     * Row cap on the flat CSV (mirrors the E-Sig export). The PDF is scope-bounded.
     */
    private static final int MAX_EXPORT_ROWS = 10000;
    private static final long MAX_EXPORT_DATE_RANGE_DAYS = 366;
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Autowired
    private QCChartDataService chartDataService;

    @Autowired
    private QCResultService qcResultService;

    @Autowired
    private QCControlLotService controlLotService;

    @Autowired
    private TestService testService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private SystemUserService systemUserService;

    // Injected rather than ConfigurationProperties.getInstance() so test slices
    // don't have to register the static SpringContext holder (mirrors esig).
    @Autowired
    private ConfigurationProperties configurationProperties;

    @GetMapping("/csv")
    @PreAuthorize("hasAuthority('qa.view.qc') or hasRole('GLOBAL_ADMIN')")
    public void exportCsv(@RequestParam String instrumentId, @RequestParam(required = false) String testId,
            @RequestParam(required = false) String controlLevel, @RequestParam String startDate,
            @RequestParam String endDate, HttpServletResponse response) throws IOException {

        ExportWindow window;
        try {
            window = parseWindow(startDate, endDate);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        QCExportModel model = chartDataService.getExportModel(instrumentId, testId, controlLevel, window.start(),
                window.end(), MAX_EXPORT_ROWS);

        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + exportFilename(model.instrumentName(), startDate, endDate, "csv") + "\"");

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_PATTERN);
        PrintWriter writer = response.getWriter();
        // UTF-8 BOM so Excel renders the Unicode subscript rule codes (1₃ₛ, R₄ₛ...).
        writer.write('﻿');
        writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n", m("qc.export.header.instrument"),
                m("qc.export.header.runDateTime"), m("qc.export.header.test"), m("qc.export.header.level"),
                m("qc.export.header.lot"), m("qc.export.header.value"), m("qc.export.header.unit"),
                m("qc.export.header.zscore"), m("qc.export.header.status"), m("qc.export.header.nonConformity"),
                m("qc.export.header.rules"), m("qc.export.header.severity"));

        for (LotSection section : model.sections()) {
            Map<String, List<QCRuleViolation>> byResult = violationsByResult(section);
            for (QCResult result : section.results()) {
                List<QCRuleViolation> violations = byResult.getOrDefault(result.getId(), List.of());
                String rules = violations.stream().map(QCRuleViolation::getRuleCode).collect(Collectors.joining("; "));
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n", StringUtil.csvEscape(model.instrumentName()),
                        StringUtil
                                .csvEscape(result.getRunDateTime() != null ? sdf.format(result.getRunDateTime()) : ""),
                        StringUtil.csvEscape(section.testName()), StringUtil.csvEscape(section.lot().getControlLevel()),
                        StringUtil.csvEscape(section.lot().getLotNumber()),
                        // Numeric columns written raw (not csvEscape'd): BigDecimal.toPlainString()
                        // is injection-safe, and the formula guard would prefix "'" to negatives
                        // (e.g. a -2.9 z-score), forcing Excel/LibreOffice to type them as text.
                        result.getResultValue() != null ? result.getResultValue().toPlainString() : "",
                        StringUtil.csvEscape(result.getUnitOfMeasure()),
                        result.getZScore() != null ? result.getZScore().toPlainString() : "",
                        StringUtil.csvEscape(result.getResultStatus()),
                        StringUtil.csvEscape(Boolean.TRUE.equals(result.getNonConformityFlag()) ? "Y" : "N"),
                        StringUtil.csvEscape(rules), StringUtil.csvEscape(severityOf(violations)));
            }
        }
        if (model.truncated()) {
            // Never drop rows silently in a compliance export (OGC-706 §01 #4).
            writer.printf("%s%n", StringUtil.csvEscape(m("qc.export.truncated") + " (" + MAX_EXPORT_ROWS + ")"));
        }
        writer.flush();
    }

    /**
     * The bench QC register: one row per manual or RDT control run in the window
     * (OGC-1147 FR-D5).
     * /rest/qc/export/bench/csv?startDate=&amp;endDate=[&amp;source=]
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
    @PreAuthorize("hasAuthority('qa.view.qc') or hasRole('GLOBAL_ADMIN')")
    public void exportBenchCsv(@RequestParam String startDate, @RequestParam String endDate,
            @RequestParam(required = false) String source, HttpServletResponse response) throws IOException {

        ExportWindow window;
        QCSource parsedSource = null;
        try {
            window = parseWindow(startDate, endDate);
            if (StringUtils.isNotBlank(source) && !"ALL".equalsIgnoreCase(source)) {
                parsedSource = QCSource.valueOf(source.toUpperCase());
                if (!parsedSource.isBenchEntered()) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "Analyzer QC belongs to the instrument export");
                    return;
                }
            }
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        // One row over the cap tells us the cap bit, without a second count query.
        List<QCResult> results = qcResultService.findBenchResults(window.start(), window.end(), parsedSource,
                MAX_EXPORT_ROWS + 1);
        boolean truncated = results.size() > MAX_EXPORT_ROWS;
        if (truncated) {
            results = results.subList(0, MAX_EXPORT_ROWS);
        }

        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + exportFilename("bench-qc", startDate, endDate, "csv") + "\"");

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_PATTERN);
        PrintWriter writer = response.getWriter();
        writer.write('﻿');
        writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n", m("qc.export.header.runDateTime"),
                m("qc.export.header.source"), m("qc.export.header.labUnit"), m("qc.export.header.test"),
                m("qc.export.header.control"), m("qc.export.header.expected"), m("qc.export.header.uncertainty"),
                m("qc.export.header.value"), m("qc.export.header.outcome"), m("qc.export.header.technician"));

        for (QCResult result : results) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    StringUtil.csvEscape(result.getRunDateTime() != null ? sdf.format(result.getRunDateTime()) : ""),
                    StringUtil.csvEscape(String.valueOf(result.getSource())),
                    StringUtil.csvEscape(benchLabUnitName(result)), StringUtil.csvEscape(benchTestName(result)),
                    StringUtil.csvEscape(benchControlLabel(result)),
                    // Numeric columns raw, for the same reason as the chart export: the
                    // formula guard would type a negative as text in Excel.
                    result.getExpectedValue() != null ? result.getExpectedValue().toPlainString() : "",
                    result.getUncertainty() != null ? result.getUncertainty().toPlainString() : "",
                    result.getResultValue() != null ? result.getResultValue().toPlainString() : "",
                    StringUtil.csvEscape(String.valueOf(result.getQualitativeOutcome())),
                    StringUtil.csvEscape(benchTechnicianName(result)));
        }
        if (truncated) {
            // Never drop rows silently in a compliance export (OGC-706 §01 #4).
            writer.printf("%s%n", StringUtil.csvEscape(m("qc.export.truncated") + " (" + MAX_EXPORT_ROWS + ")"));
        }
        writer.flush();
    }

    /**
     * Names are context on a compliance export, never a reason to fail one. Each
     * lookup is a separate service, so they share this guard rather than four
     * copies of it.
     */
    private String resolveOrBlank(Supplier<String> lookup) {
        try {
            String value = lookup.get();
            return value == null ? "" : value;
        } catch (RuntimeException e) {
            return "";
        }
    }

    private String benchLabUnitName(QCResult result) {
        if (result.getTestSectionId() == null) {
            return "";
        }
        return resolveOrBlank(() -> {
            TestSection section = testSectionService.get(result.getTestSectionId());
            return section == null ? null : section.getLocalizedName();
        });
    }

    private String benchTestName(QCResult result) {
        if (result.getTestId() == null) {
            return "";
        }
        return resolveOrBlank(() -> {
            Test test = testService.get(result.getTestId());
            return test == null ? null : test.getName();
        });
    }

    /** The kit label for an RDT, or the lot number for a levelled control. */
    private String benchControlLabel(QCResult result) {
        if (StringUtils.isNotBlank(result.getControlLabel())) {
            return result.getControlLabel();
        }
        if (result.getControlLotId() == null) {
            return "";
        }
        return resolveOrBlank(() -> {
            QCControlLot lot = controlLotService.get(result.getControlLotId());
            return lot == null ? null : lot.getLotNumber() + " (" + lot.getControlLevel() + ")";
        });
    }

    /**
     * Who ran the control — the point of recording the acting user, not automation.
     */
    private String benchTechnicianName(QCResult result) {
        if (result.getTechnicianId() == null) {
            return "";
        }
        return resolveOrBlank(() -> {
            SystemUser user = systemUserService.get(String.valueOf(result.getTechnicianId()));
            return user == null ? null : user.getDisplayName();
        });
    }

    @GetMapping("/pdf")
    @PreAuthorize("hasAuthority('qa.view.qc') or hasRole('GLOBAL_ADMIN')")
    public void exportPdf(@RequestParam String instrumentId, @RequestParam(required = false) String testId,
            @RequestParam(required = false) String controlLevel, @RequestParam String startDate,
            @RequestParam String endDate, HttpServletResponse response) throws IOException {

        ExportWindow window;
        try {
            window = parseWindow(startDate, endDate);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        QCExportModel model = chartDataService.getExportModel(instrumentId, testId, controlLevel, window.start(),
                window.end(), MAX_EXPORT_ROWS);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + exportFilename(model.instrumentName(), startDate, endDate, "pdf") + "\"");

        // Locale-aware timestamps for the human-facing report (acceptance criterion:
        // locale-aware date formatting), driven by the request/session locale.
        DateFormat sdf = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                LocaleContextHolder.getLocale());
        try {
            Document document = new Document(PageSize.A4, 36, 36, 42, 42);
            PdfWriter pdfWriter = PdfWriter.getInstance(document, response.getOutputStream());
            pdfWriter.setPageEvent(new PageNumberFooter());
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD);
            Font metaFont = new Font(Font.FontFamily.HELVETICA, 9);
            Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.WHITE);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 8);

            document.add(new Phrase(m("qc.export.title") + "\n", titleFont));
            String labName = configurationProperties.getPropertyValue(ConfigurationProperties.Property.SiteName);
            document.add(new Phrase(m("qc.export.labName") + ": " + (labName == null ? "" : labName) + "\n", metaFont));
            document.add(new Phrase(m("qc.export.instrument") + ": " + model.instrumentName() + "\n", metaFont));
            document.add(new Phrase(m("qc.export.dateRange") + ": " + startDate + " — " + endDate + "\n", metaFont));
            document.add(new Phrase(m("qc.export.totalRuns") + ": " + model.totalRuns() + "     "
                    + m("qc.export.totalViolations") + ": " + model.totalViolations() + "\n", metaFont));
            document.add(
                    new Phrase(m("qc.export.generatedAt") + ": " + sdf.format(new java.util.Date()) + "\n", metaFont));
            document.add(Chunk.NEWLINE);

            if (model.sections().isEmpty()) {
                document.add(new Phrase(m("qc.export.noData"), metaFont));
            }

            for (LotSection section : model.sections()) {
                Paragraph heading = new Paragraph(section.testName() + " — " + section.lot().getControlLevel() + " — "
                        + m("qc.export.header.lot") + " " + section.lot().getLotNumber(), sectionFont);
                heading.setSpacingBefore(8);
                heading.setSpacingAfter(4);
                document.add(heading);

                addLeveyJenningsChart(document, section, metaFont);
                addSigmaTable(document, section, headerFont, cellFont, metaFont, sdf);
                addViolationsTable(document, section, sectionFont, headerFont, cellFont, sdf);
            }

            if (model.truncated()) {
                Font warnFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.RED);
                document.add(Chunk.NEWLINE);
                document.add(new Phrase(m("qc.export.truncated") + " (" + MAX_EXPORT_ROWS + ")", warnFont));
            }

            document.close();
        } catch (DocumentException e) {
            LogEvent.logError(e);
            throw new IOException("Error generating PDF", e);
        }
    }

    /**
     * Render the L-J chart for a section and embed it. A single chart failure (e.g.
     * a headless-AWT issue) degrades to a note rather than failing the whole
     * report.
     */
    private void addLeveyJenningsChart(Document document, LotSection section, Font noteFont) {
        try {
            Set<String> violatedResultIds = section.violations().stream().map(QCRuleViolation::getTriggeringResultId)
                    .collect(Collectors.toSet());
            byte[] png = renderLeveyJenningsPng(section.results(), violatedResultIds, 780, 300);
            Image image = Image.getInstance(png);
            image.scaleToFit(520, 220);
            image.setSpacingAfter(4);
            document.add(image);
        } catch (Exception e) {
            LogEvent.logError(e);
            try {
                document.add(new Phrase(m("qc.export.chartUnavailable"), noteFont));
            } catch (DocumentException ignored) {
                // nothing more we can do for this section's chart
            }
        }
    }

    /**
     * Render the Levey-Jennings chart to match the on-screen chart
     * (LeveyJenningsChart.jsx): z-scores on the Y axis with fixed control lines at
     * 0/±1/±2/±3σ, blue points joined by a line, and violated points overlaid as
     * larger red dots. Results without a z-score (e.g. establishment runs before
     * the lot activated) are omitted, exactly as the UI does.
     */
    private byte[] renderLeveyJenningsPng(List<QCResult> results, Set<String> violatedResultIds, int width, int height)
            throws IOException {
        List<QCResult> ordered = results.stream().filter(r -> r.getZScore() != null && r.getRunDateTime() != null)
                .sorted(Comparator.comparing(QCResult::getRunDateTime)).toList();

        XYSeries qc = new XYSeries(m("qc.export.chart.qc"));
        XYSeries violations = new XYSeries(m("qc.export.chart.violation"));
        double minZ = -4.0;
        double maxZ = 4.0;
        for (int i = 0; i < ordered.size(); i++) {
            QCResult r = ordered.get(i);
            double z = r.getZScore().doubleValue();
            qc.add(i + 1, z);
            if (violatedResultIds.contains(r.getId())) {
                violations.add(i + 1, z);
            }
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(qc);
        if (violations.getItemCount() > 0) {
            dataset.addSeries(violations);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(null, m("qc.export.chart.run"), m("qc.export.chart.zscore"),
                dataset);
        XYPlot plot = chart.getXYPlot();

        // Points + connecting line (blue), violated points overlaid as larger red
        // dots — mirrors LeveyJenningsChart.jsx (#0f62fe normal, #da1e28 violation).
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesPaint(0, new Color(15, 98, 254));
        renderer.setSeriesShape(0, new Ellipse2D.Double(-2.5, -2.5, 5, 5));
        if (dataset.getSeriesCount() > 1) {
            renderer.setSeriesLinesVisible(1, false);
            renderer.setSeriesShapesVisible(1, true);
            renderer.setSeriesPaint(1, new Color(218, 30, 40));
            renderer.setSeriesShape(1, new Ellipse2D.Double(-3.5, -3.5, 7, 7));
        }
        plot.setRenderer(renderer);

        // Fixed control lines at 0/±1/±2/±3σ (mean dark, ±1 grey, ±2 amber, ±3 red),
        // matching the UI thresholds.
        plot.addRangeMarker(new ValueMarker(0, new Color(22, 22, 22), new BasicStroke(1.2f)));
        Color[] band = { new Color(168, 168, 168), new Color(241, 194, 27), new Color(218, 30, 40) };
        for (int k = 1; k <= 3; k++) {
            plot.addRangeMarker(new ValueMarker(k, band[k - 1], new BasicStroke(0.8f)));
            plot.addRangeMarker(new ValueMarker(-k, band[k - 1], new BasicStroke(0.8f)));
        }

        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setRange(Math.min(-4.0, Math.floor(minZ - 0.5)), Math.max(4.0, Math.ceil(maxZ + 0.5)));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtilities.writeChartAsPNG(baos, chart, width, height);
        return baos.toByteArray();
    }

    private void addSigmaTable(Document document, LotSection section, Font headerFont, Font cellFont, Font noteFont,
            DateFormat sdf) throws DocumentException {
        QCStatistics stats = section.statistics();
        if (stats == null) {
            document.add(new Phrase(m("qc.export.noStatistics"), noteFont));
            return;
        }
        // CV depends only on mean/SD (like the on-screen chart) — compute it directly
        // rather than reading section.sigma().cv(), which is null whenever TEa is
        // unset (sigma NOT_CALCULABLE) even though CV is perfectly computable.
        Double cv = SigmaMetrics.cv(stats.getMean(), stats.getStandardDeviation());
        Double sigma = section.sigma() != null ? section.sigma().sigma() : null;
        String category = section.sigma() != null ? section.sigma().category() : "";

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(2);
        table.setSpacingAfter(6);
        String[] headers = { m("qc.export.stats.mean"), m("qc.export.stats.sd"), m("qc.export.stats.cv"),
                m("qc.export.stats.n"), m("qc.export.stats.sigma"), m("qc.export.stats.category"),
                m("qc.export.stats.method"), m("qc.export.stats.computedAt") };
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new BaseColor(51, 102, 179));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4);
            table.addCell(cell);
        }
        table.addCell(new Phrase(decimal(stats.getMean()), cellFont));
        table.addCell(new Phrase(decimal(stats.getStandardDeviation()), cellFont));
        table.addCell(new Phrase(cv != null ? fmt(cv) : "—", cellFont));
        table.addCell(new Phrase(stats.getNumValues() != null ? stats.getNumValues().toString() : "—", cellFont));
        table.addCell(new Phrase(sigma != null ? fmt(sigma) : "—", cellFont));
        table.addCell(new Phrase(category == null ? "" : category.replace('_', ' '), cellFont));
        table.addCell(new Phrase(stats.getCalculationMethod() != null ? stats.getCalculationMethod() : "", cellFont));
        table.addCell(
                new Phrase(stats.getCalculationDate() != null ? sdf.format(stats.getCalculationDate()) : "", cellFont));
        document.add(table);
    }

    private void addViolationsTable(Document document, LotSection section, Font titleFont, Font headerFont,
            Font cellFont, DateFormat sdf) throws DocumentException {
        List<QCRuleViolation> violations = section.violations();
        if (violations == null || violations.isEmpty()) {
            return;
        }
        Paragraph title = new Paragraph(m("qc.export.viol.title"), titleFont);
        title.setSpacingBefore(2);
        title.setSpacingAfter(2);
        document.add(title);

        PdfPTable table = new PdfPTable(new float[] { 2.4f, 1.2f, 1.6f, 2f });
        table.setWidthPercentage(100);
        String[] headers = { m("qc.export.viol.dateTime"), m("qc.export.viol.rule"), m("qc.export.viol.severity"),
                m("qc.export.viol.resolution") };
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new BaseColor(51, 102, 179));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4);
            table.addCell(cell);
        }
        for (QCRuleViolation violation : violations) {
            table.addCell(new Phrase(
                    violation.getViolationDateTime() != null ? sdf.format(violation.getViolationDateTime()) : "",
                    cellFont));
            // iText's base-14 Helvetica has no Unicode subscript glyphs, so the raw
            // rule code (e.g. 1₃ₛ) would be stripped to "1". NFKD folds the subscripts
            // to their ASCII forms (1₃ₛ -> 13s), the standard Westgard notation. CSV
            // and the UI keep the Unicode form.
            table.addCell(new Phrase(violation.getRuleCode() != null
                    ? Normalizer.normalize(violation.getRuleCode(), Normalizer.Form.NFKD)
                    : "", cellFont));
            table.addCell(new Phrase(violation.getSeverity() != null ? violation.getSeverity() : "", cellFont));
            table.addCell(new Phrase(violation.getResolutionStatus() != null ? violation.getResolutionStatus() : "",
                    cellFont));
        }
        document.add(table);
    }

    private Map<String, List<QCRuleViolation>> violationsByResult(LotSection section) {
        return section.violations().stream().collect(Collectors.groupingBy(QCRuleViolation::getTriggeringResultId));
    }

    private String severityOf(List<QCRuleViolation> violations) {
        if (violations.stream().anyMatch(v -> "REJECTION".equals(v.getSeverity()))) {
            return "REJECTION";
        }
        return violations.isEmpty() ? "" : "WARNING";
    }

    private String decimal(BigDecimal value) {
        return value != null ? fmt(value.doubleValue()) : "—";
    }

    private String fmt(double value) {
        return String.format(Locale.US, "%.2f", value);
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

    private String m(String key) {
        return MessageUtil.getMessage(key);
    }

    /** Validated export window, shared by the CSV and PDF endpoints. */
    private record ExportWindow(Timestamp start, Timestamp end) {
    }

    private ExportWindow parseWindow(String startDate, String endDate) {
        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(startDate);
            to = LocalDate.parse(endDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date: " + e.getMessage());
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("startDate must not be after endDate");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_EXPORT_DATE_RANGE_DAYS) {
            throw new IllegalArgumentException("Date range must not exceed 1 year");
        }
        return new ExportWindow(Timestamp.valueOf(from.atStartOfDay()), Timestamp.valueOf(to.atTime(LocalTime.MAX)));
    }

    /**
     * Footer with page number on every page of the PDF export (CAP layout, mirrors
     * esig).
     */
    private static class PageNumberFooter extends PdfPageEventHelper {
        private static final Font FOOTER_FONT = new Font(Font.FontFamily.HELVETICA, 8);

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Phrase footer = new Phrase(MessageUtil.getMessage("qc.export.page") + " " + writer.getPageNumber(),
                    FOOTER_FONT);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, footer,
                    (document.right() + document.left()) / 2, document.bottom() - 12, 0);
        }
    }
}
