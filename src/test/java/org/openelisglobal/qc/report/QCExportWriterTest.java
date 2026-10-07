package org.openelisglobal.qc.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.qc.report.QCBenchExportService.BenchExport;
import org.openelisglobal.qc.report.QCBenchExportService.BenchExportRow;
import org.openelisglobal.qc.service.QCChartDataService.LotSection;
import org.openelisglobal.qc.service.QCChartDataService.QCExportModel;
import org.openelisglobal.qc.service.SigmaMetrics;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * What the OGC-706 inspector export actually renders, written straight onto a
 * stream. The writer is a pure function of its export model, so this needs no
 * web layer and no service: the characteristics worth pinning — the BOM Excel
 * needs before it will show Unicode rule codes, the formula-injection guard,
 * numeric columns left raw so a spreadsheet types them as numbers, the honest
 * truncation notice, and the ASCII folding iText's base-14 fonts force on
 * subscript rule codes — are all properties of the rendering alone.
 */
public class QCExportWriterTest {

    // U+2083 U+209B = the "1₃ₛ" Westgard rule code (subscript 3, subscript s)
    private static final String RULE_1_3S = "1₃ₛ";

    /**
     * Both writers resolve their column headings through the static MessageUtil, so
     * back it with the real bundle and the assertions check shipped strings.
     */
    @BeforeClass
    public static void useTheRealMessageBundle() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:languages/message");
        messageSource.setDefaultEncoding("UTF-8");
        MessageUtil.setMessageSource(messageSource);
    }

    @Test
    public void csvLeadsWithABomAndEscapesEachRun() {
        String csv = writeCsv(model(false));

        assertTrue("UTF-8 BOM must lead the file so Excel renders subscript rule codes", csv.startsWith("﻿"));
        String[] lines = csv.split("\\R");
        assertTrue("header carries the inspector columns", lines[0].contains("Instrument")
                && lines[0].contains("Violated Rules") && lines[0].contains("Severity"));
        assertEquals("header plus one row per result", 3, lines.length);
        assertTrue("comma-bearing test name is quoted", csv.contains("\"Glucose, serum\""));
        assertTrue("the instrument name appears on the data rows", csv.contains("Cobas 6000"));
        assertTrue("the rejection row carries the Unicode rule code and REJECTION severity",
                csv.contains(RULE_1_3S) && csv.contains("REJECTION"));
        assertTrue("a formula-injection unit is neutralised with a leading apostrophe", csv.contains("'=danger"));
        assertTrue("a negative z-score stays a raw number so Excel types it numerically",
                csv.contains("-2.5") && !csv.contains("'-2.5"));
    }

    @Test
    public void csvAppendsATruncationNoticeWhenCapped() {
        assertTrue("a capped export must surface a truncation notice, never drop rows silently",
                writeCsv(model(true)).contains("Export truncated at the maximum row limit"));
    }

    @Test
    public void benchCsvWritesOneRegisterRowPerControlRun() {
        BenchExportRow manual = new BenchExportRow(Timestamp.valueOf("2026-06-15 09:00:00"), "MANUAL", "Haematology",
                "Bench Haemoglobin", "BENCH-A (NORMAL)", new BigDecimal("100.00"), new BigDecimal("5.00"),
                new BigDecimal("112.50"), "PASS", "Bench Tech");
        // An RDT line has no lot, no number and no target. The register still has to
        // carry it, which is why bench QC is a flat export rather than one section per
        // control lot.
        BenchExportRow rdt = new BenchExportRow(Timestamp.valueOf("2026-06-15 10:00:00"), "RDT", "Parasitology",
                "Bench Malaria RDT", "Malaria RDT, LOT-1", null, null, null, "INVALID", "Bench Tech");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        QCExportWriter.writeBenchCsv(new BenchExport(List.of(manual, rdt), false), out);
        String csv = out.toString(StandardCharsets.UTF_8);

        String[] lines = csv.split("\\R");
        assertEquals("header plus one row per run", 3, lines.length);
        assertTrue("header carries the register columns",
                lines[0].contains("Lab Unit") && lines[0].contains("Outcome") && lines[0].contains("Performed By"));
        assertTrue("the manual run carries its measured value and its target",
                lines[1].contains("112.50") && lines[1].contains("100.00") && lines[1].contains("PASS"));
        assertTrue("a comma in the control label is quoted", csv.contains("\"Malaria RDT, LOT-1\""));
        assertTrue("an RDT run leaves the numeric columns empty rather than inventing a value",
                lines[2].contains("INVALID") && lines[2].contains(",,,"));
    }

    @Test
    public void pdfRendersTheReportWithItsInstrumentAndSigma() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        QCExportWriter.writePdf(model(false), "Kisumu Reference Lab", "2026-06-01", "2026-06-30", out);
        byte[] pdf = out.toByteArray();

        assertEquals("body should start with the PDF magic bytes", "%PDF-", new String(pdf, 0, 5));
        PdfReader reader = new PdfReader(pdf);
        String text = PdfTextExtractor.getTextFromPage(reader, 1);
        reader.close();

        assertTrue("report title present", text.contains("Quality Control Inspector Report"));
        assertTrue("lab name present", text.contains("Kisumu Reference Lab"));
        assertTrue("instrument name present", text.contains("Cobas 6000"));
        assertTrue("sigma interpretation present", text.contains("ACCEPTABLE"));
        // The CV is printed to two decimals and is carried even where sigma itself is
        // not calculable, so the column is never silently blank.
        assertTrue("CV rendered to two decimals", text.contains("4.12"));
        // The Unicode rule code 1₃ₛ is NFKD-folded to ASCII so iText (no subscript
        // glyphs) renders it legibly rather than stripping it to "1".
        assertTrue("rule code rendered as folded ASCII (13s)", text.contains("13s"));
    }

    private static String writeCsv(QCExportModel model) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        QCExportWriter.writeCsv(model, out);
        return out.toString(StandardCharsets.UTF_8);
    }

    /** One lot section: two runs, the second of which tripped a rejection rule. */
    private static QCExportModel model(boolean truncated) {
        QCControlLot lot = new QCControlLot();
        lot.setControlLevel("NORMAL");
        lot.setLotNumber("LOT-A");
        lot.setTestId("412");

        QCResult accepted = new QCResult();
        accepted.setId("r1");
        accepted.setResultValue(new BigDecimal("100.0"));
        accepted.setZScore(new BigDecimal("-2.5"));
        accepted.setUnitOfMeasure("mg/dL");
        accepted.setResultStatus("ACCEPTED");
        accepted.setNonConformityFlag(Boolean.FALSE);
        accepted.setRunDateTime(Timestamp.valueOf("2026-06-15 09:00:00"));

        QCResult violating = new QCResult();
        violating.setId("r2");
        violating.setResultValue(new BigDecimal("108.0"));
        violating.setZScore(new BigDecimal("3.6"));
        violating.setUnitOfMeasure("=danger"); // formula-injection probe
        violating.setResultStatus("ACCEPTED");
        violating.setNonConformityFlag(Boolean.TRUE);
        violating.setRunDateTime(Timestamp.valueOf("2026-06-16 09:00:00"));

        QCRuleViolation violation = new QCRuleViolation();
        violation.setTriggeringResultId("r2");
        violation.setRuleCode(RULE_1_3S);
        violation.setSeverity("REJECTION");
        violation.setResolutionStatus("UNRESOLVED");
        violation.setViolationDateTime(Timestamp.valueOf("2026-06-16 09:00:00"));

        QCStatistics stats = new QCStatistics();
        stats.setMean(new BigDecimal("200.0"));
        stats.setStandardDeviation(new BigDecimal("8.0"));
        stats.setNumValues(30);
        stats.setCalculationMethod("ROLLING");
        stats.setCalculationDate(Timestamp.valueOf("2026-06-30 12:00:00"));

        LotSection section = new LotSection(lot, "Glucose, serum", List.of(accepted, violating), List.of(violation),
                stats, new SigmaMetrics.SigmaResult(4.1234, 5.0, SigmaMetrics.ACCEPTABLE));
        return new QCExportModel("Cobas 6000", List.of(section), 2, 1, truncated);
    }
}
