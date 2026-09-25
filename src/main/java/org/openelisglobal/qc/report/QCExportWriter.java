package org.openelisglobal.qc.report;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPTable;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.qc.report.QCBenchExportService.BenchExport;
import org.openelisglobal.qc.report.QCBenchExportService.BenchExportRow;
import org.openelisglobal.qc.service.QCChartDataService.LotSection;
import org.openelisglobal.qc.service.QCChartDataService.QCExportModel;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Renders the QC inspector export (OGC-706) onto a stream: the flat CSV run
 * detail, the bench QC register (OGC-1147) and the formatted PDF with embedded
 * Levey-Jennings charts and sigma tables. Follows the report-writer precedent
 * of {@link org.openelisglobal.inventory.report.InventoryReportWriter} — the
 * controller parses parameters and sets response headers, rendering lives here.
 */
public final class QCExportWriter {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private QCExportWriter() {
    }

    /** Run/violation detail, one row per QC result across every control lot. */
    public static void writeCsv(QCExportModel model, OutputStream out) {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_PATTERN);
        // UTF-8 BOM so Excel renders the Unicode subscript rule codes (1₃ₛ, R₄ₛ...).
        writer.write('﻿');
        writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n", m("qc.export.header.instrument"),
                m("qc.export.header.runDateTime"), m("qc.export.header.test"), m("qc.export.header.level"),
                m("qc.export.header.lot"), m("qc.export.header.value"), m("qc.export.header.unit"),
                m("qc.export.chart.zscore"), m("qc.export.header.status"), m("qc.export.header.nonConformity"),
                m("qc.export.header.rules"), m("qc.export.viol.severity"));

        for (LotSection section : model.sections()) {
            Map<String, List<QCRuleViolation>> byResult = section.violations().stream()
                    .collect(Collectors.groupingBy(QCRuleViolation::getTriggeringResultId));
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
                        StringUtil.csvEscape(rules), StringUtil.csvEscape(QCRuleViolation.worstSeverity(violations)));
            }
        }
        writeTruncationNotice(writer, model.truncated());
        writer.flush();
    }

    /** The bench QC register: one row per manual or RDT control run (OGC-1147). */
    public static void writeBenchCsv(BenchExport bench, OutputStream out) {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_PATTERN);
        writer.write('﻿');
        writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n", m("qc.export.header.runDateTime"),
                m("qc.export.header.source"), m("qc.export.header.labUnit"), m("qc.export.header.test"),
                m("qc.export.header.control"), m("qc.export.header.expected"), m("qc.export.header.uncertainty"),
                m("qc.export.header.value"), m("qc.export.header.outcome"), m("qc.export.header.technician"));

        for (BenchExportRow row : bench.rows()) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    StringUtil.csvEscape(row.runDateTime() != null ? sdf.format(row.runDateTime()) : ""),
                    StringUtil.csvEscape(row.source()), StringUtil.csvEscape(row.labUnitName()),
                    StringUtil.csvEscape(row.testName()), StringUtil.csvEscape(row.controlLabel()),
                    // Numeric columns raw, for the same reason as the chart export: the
                    // formula guard would type a negative as text in Excel.
                    row.expectedValue() != null ? row.expectedValue().toPlainString() : "",
                    row.uncertainty() != null ? row.uncertainty().toPlainString() : "",
                    row.resultValue() != null ? row.resultValue().toPlainString() : "",
                    StringUtil.csvEscape(row.outcome()), StringUtil.csvEscape(row.technicianName()));
        }
        writeTruncationNotice(writer, bench.truncated());
        writer.flush();
    }

    /** Never drop rows silently in a compliance export (OGC-706). */
    private static void writeTruncationNotice(PrintWriter writer, boolean truncated) {
        if (truncated) {
            writer.printf("%s%n",
                    StringUtil.csvEscape(m("qc.export.truncated") + " (" + PdfExportSupport.MAX_EXPORT_ROWS + ")"));
        }
    }

    /**
     * The inspector report: one section per control lot, each with its
     * Levey-Jennings chart, sigma table and violation table.
     */
    public static void writePdf(QCExportModel model, String labName, String startDate, String endDate, OutputStream out)
            throws IOException {
        // Locale-aware timestamps for the human-facing report (acceptance criterion:
        // locale-aware date formatting), driven by the request/session locale.
        DateFormat sdf = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                LocaleContextHolder.getLocale());
        try {
            Document document = new Document(PageSize.A4, 36, 36, 42, 42);
            PdfExportSupport.openWithPageNumbers(document, out, "export.page");

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD);
            Font metaFont = new Font(Font.FontFamily.HELVETICA, 9);
            Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.WHITE);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 8);

            PdfExportSupport.addHeading(document, m("qc.export.title"), titleFont, metaFont,
                    m("export.labName") + ": " + labName + "\n",
                    m("qc.export.header.instrument") + ": " + model.instrumentName() + "\n",
                    m("export.dateRange") + ": " + startDate + " — " + endDate + "\n",
                    m("qc.export.totalRuns") + ": " + model.totalRuns() + "     " + m("qc.export.totalViolations")
                            + ": " + model.totalViolations() + "\n",
                    m("export.generatedAt") + ": " + sdf.format(new java.util.Date()) + "\n");
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
                document.add(
                        new Phrase(m("qc.export.truncated") + " (" + PdfExportSupport.MAX_EXPORT_ROWS + ")", warnFont));
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
    private static void addLeveyJenningsChart(Document document, LotSection section, Font noteFont) {
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
    private static byte[] renderLeveyJenningsPng(List<QCResult> results, Set<String> violatedResultIds, int width,
            int height) throws IOException {
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

    private static void addSigmaTable(Document document, LotSection section, Font headerFont, Font cellFont,
            Font noteFont, DateFormat sdf) throws DocumentException {
        QCStatistics stats = section.statistics();
        if (stats == null) {
            document.add(new Phrase(m("qc.export.noStatistics"), noteFont));
            return;
        }
        Double cv = section.sigma() != null ? section.sigma().cv() : null;
        Double sigma = section.sigma() != null ? section.sigma().sigma() : null;
        String category = section.sigma() != null ? section.sigma().category() : "";

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setSpacingBefore(2);
        table.setSpacingAfter(6);
        PdfExportSupport.addHeaderRow(table, headerFont, 4, m("qc.export.stats.mean"), m("qc.export.stats.sd"),
                m("qc.export.stats.cv"), m("qc.export.stats.n"), m("qc.export.stats.sigma"),
                m("qc.export.stats.category"), m("qc.export.stats.method"), m("qc.export.stats.computedAt"));
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

    private static void addViolationsTable(Document document, LotSection section, Font titleFont, Font headerFont,
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
        PdfExportSupport.addHeaderRow(table, headerFont, 4, m("qc.export.viol.dateTime"), m("qc.export.viol.rule"),
                m("qc.export.viol.severity"), m("qc.export.viol.resolution"));
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

    private static String decimal(BigDecimal value) {
        return value != null ? fmt(value.doubleValue()) : "—";
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String m(String key) {
        return MessageUtil.getMessage(key);
    }
}
