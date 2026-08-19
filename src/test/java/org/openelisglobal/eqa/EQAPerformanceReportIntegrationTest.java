package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.eqa.service.EQAPerformanceReportPDFService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-933 — the printed CPHL-format performance report against the real schema:
 * the summaries, the scoring table and the cycle identifiers a CPHL reviewer
 * signs off on.
 *
 * <p>
 * Z-scores are written straight to the row here. The report only reads
 * {@code z_score}; the production writers are participant-result scoring and
 * the external score intake, not this report.
 *
 * <p>
 * Label text is asserted against the message bundle rather than the rendered
 * PDF: the test context has no message source wired, so {@code MessageUtil}
 * echoes the key. Everything the CPHL reviewer checks — programme, cycle,
 * analyte, z-score, verdict, counts — is data, and is asserted on the rendered
 * page.
 */
public class EQAPerformanceReportIntegrationTest extends EQASpineTestBase {

    private static final long SECTION_ID = 9812L;
    private static final String SECTION_NAME = "EQA Report Section";
    private static final long HIV_ANALYTE = 9820L;
    private static final long CD4_ANALYTE = 9821L;
    private static final long TB_ANALYTE = 9822L;
    private static final long PENDING_ANALYTE = 9823L;
    private static final long ENROLLMENT = 9920L;
    private static final long OTHER_ENROLLMENT = 9921L;

    @Autowired
    private EQAPerformanceReportPDFService reportService;

    private EQAProgram scheme;
    private EQACycle cycle;
    private EQARound round;

    @Before
    public void seedCycleWithScores() {
        jdbc.update("INSERT INTO clinlims.localization (id, description)"
                + " SELECT ?, 'EQA Report Section' WHERE NOT EXISTS"
                + " (SELECT 1 FROM clinlims.localization WHERE id = ?)", SECTION_ID, SECTION_ID);
        jdbc.update(
                "INSERT INTO clinlims.test_section (id, name, description, is_external, sort_order,"
                        + " name_localization_id) SELECT ?, ?, ?, 'N', ?, ? WHERE NOT EXISTS"
                        + " (SELECT 1 FROM clinlims.test_section WHERE id = ?)",
                SECTION_ID, SECTION_NAME, SECTION_NAME, SECTION_ID, SECTION_ID, SECTION_ID);
        seedAnalyte(HIV_ANALYTE, "HIV Viral Load");
        seedAnalyte(CD4_ANALYTE, "CD4 Count");
        seedAnalyte(TB_ANALYTE, "TB Smear Grade");
        seedAnalyte(PENDING_ANALYTE, "Syphilis RPR");
        seedEnrollment(ENROLLMENT, "CPHL HIV Programme");
        seedEnrollment(OTHER_ENROLLMENT, "Another Lab Enrollment");

        scheme = insertScheme("CPHL HIV Viral Load PT", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        // The section FK is set in SQL: the base test runs each service call in its
        // own transaction, so a section loaded through the ORM here is not managed.
        jdbc.update("UPDATE clinlims.eqa_program SET test_section_id = ? WHERE id = ?", SECTION_ID, scheme.getId());
        scheme = eqaProgramService.get(scheme.getId());

        cycle = readBack(insertCycle(scheme, 3));
        cycle.setCycleName("Q3 2026");
        cycle.setPlannedStartDate(Date.valueOf("2026-07-01"));
        cycle.setPlannedEndDate(Date.valueOf("2026-09-30"));
        cycle.setSysUserId(USER);
        eqaCycleDAO.update(cycle);
        round = eqaRoundDAO.get(insertRound(cycle, 1, "OPEN")).orElseThrow(AssertionError::new);

        scored(HIV_ANALYTE, "48000", "copies/mL", new BigDecimal("0.8"), EQAPerformanceStatus.ACCEPTABLE, ENROLLMENT);
        scored(CD4_ANALYTE, "410", "cells/uL", new BigDecimal("2.4"), EQAPerformanceStatus.QUESTIONABLE, ENROLLMENT);
        scored(TB_ANALYTE, "Scanty", null, new BigDecimal("-3.6"), EQAPerformanceStatus.UNACCEPTABLE, ENROLLMENT);
        insertParticipantResult(cycle, round, ENROLLMENT, PENDING_ANALYTE, EQASubmissionStatus.SUBMITTED, "Negative");
        scored(HIV_ANALYTE, "51000", "copies/mL", new BigDecimal("1.1"), EQAPerformanceStatus.ACCEPTABLE,
                OTHER_ENROLLMENT);
    }

    @Test
    public void reportCarriesTheCycleIdentifiersAndSchemeHeader() throws IOException {
        String text = reportText(null);

        assertTrue("the scheme name identifies the programme", text.contains("CPHL HIV Viral Load PT"));
        assertTrue("provider is printed for an external scheme", text.contains("NHLS"));
        assertTrue("the cycle number and name identify the round", text.contains("#3 — Q3 2026"));
        assertTrue("the planned period is printed", text.contains("2026-07-01 — 2026-09-30"));
        assertTrue("scheme type is printed", text.contains("INTERNATIONAL_PT"));
    }

    @Test
    public void programmeSummaryCountsEveryStatusAndRatesOnlyScoredResults() throws IOException {
        String text = reportText(null);

        // 5 rows, 4 scored (one SUBMITTED row is not scored); 2 acceptable,
        // 1 questionable, 1 unacceptable, so 2/4 = 50.0% of scored results.
        assertTrue("the summary row reads 5 results, 4 scored, 2/1/1 and 50%",
                summaryRowIn(text, "5", "4", "2", "1", "1", "50%"));
    }

    @Test
    public void sectionSummaryGroupsByTheSchemeSection() throws IOException {
        String text = reportText(null);

        assertTrue("results with no analysis link fall back to the scheme's section", text.contains(SECTION_NAME));
        assertTrue("the section summary tallies this section's four scored results",
                rowIn(text, SECTION_NAME, "5", "4", "2", "1", "1", "50%"));
    }

    @Test
    public void scoringTableCarriesEveryAnalyteWithItsZScoreAndVerdict() throws IOException {
        String text = reportText(null);

        assertTrue("acceptable row", rowIn(text, "HIV Viral Load", "48000", "copies/mL", "0.8", "ACCEPTABLE"));
        assertTrue("questionable row", rowIn(text, "CD4 Count", "410", "cells/uL", "2.4", "QUESTIONABLE"));
        assertTrue("unacceptable row keeps the sign of the z-score",
                rowIn(text, "TB Smear Grade", "Scanty", "-3.6", "UNACCEPTABLE"));
        assertTrue("an unscored submission is shown, not dropped",
                rowIn(text, "Syphilis RPR", "Negative", "Not scored"));
    }

    @Test
    public void perParticipantVariantExcludesOtherEnrollments() throws IOException {
        String all = reportText(null);
        assertEquals("both enrollments' HIV rows appear in the cycle-wide report", 2,
                occurrences(all, "48000") + occurrences(all, "51000"));

        String mine = reportText(ENROLLMENT);
        assertTrue("this participant's result is present", mine.contains("48000"));
        assertFalse("the other enrollment's result is not", mine.contains("51000"));
        assertTrue("the header names the participant enrollment", mine.contains(String.valueOf(ENROLLMENT)));
        assertTrue("4 results, 3 scored, 1 acceptable, 1 questionable, 1 unacceptable, 33.3%",
                summaryRowIn(mine, "4", "3", "1", "1", "1", "33.3%"));
    }

    @Test
    public void everyReportLabelResolvesInTheMessageBundle() throws IOException {
        Properties bundle = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/languages/message_en.properties")) {
            assertTrue("the backend message bundle is on the classpath", in != null);
            bundle.load(in);
        }
        for (String key : List.of("eqa.report.title", "eqa.report.scheme", "eqa.report.cycle", "eqa.report.period",
                "eqa.report.summary.programme", "eqa.report.summary.section", "eqa.report.summary.acceptableRate",
                "eqa.report.summary.unscored", "eqa.report.table.title", "eqa.report.table.zscore",
                "eqa.report.signoff.title", "eqa.report.unassignedSection")) {
            String value = bundle.getProperty(key);
            assertTrue(key + " must be translated in message_en.properties",
                    value != null && !value.isBlank() && !value.equals(key));
        }
    }

    @Test
    public void anUnknownCycleIsRejected() {
        try {
            reportService.generatePerformanceReport(987654L, null);
            fail("expected an unknown cycle to be refused");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("987654"));
        }
    }

    // ---- helpers ----

    private void scored(long analyteId, String value, String unit, BigDecimal zScore, EQAPerformanceStatus performance,
            long enrollmentId) {
        Long id = insertParticipantResult(cycle, round, enrollmentId, analyteId, EQASubmissionStatus.SUBMITTED, value);
        EQAParticipantResult result = eqaParticipantResultDAO.get(id).orElseThrow(AssertionError::new);
        result.setResultUnit(unit);
        result.setZScore(zScore);
        result.setPerformanceStatus(performance);
        result.setSubmissionStatus(EQASubmissionStatus.SCORED);
        result.setAssignedAnalystId(ADMIN_USER_ID);
        result.setSysUserId(USER);
        eqaParticipantResultDAO.update(result);
    }

    private void seedAnalyte(long id, String name) {
        jdbc.update("INSERT INTO clinlims.analyte (id, name, is_active, lastupdated)"
                + " SELECT ?, ?, 'Y', now() WHERE NOT EXISTS" + " (SELECT 1 FROM clinlims.analyte WHERE id = ?)", id,
                name, id);
    }

    private String reportText(Long labEnrollmentId) throws IOException {
        byte[] pdf = reportService.generatePerformanceReport(cycle.getId(), labEnrollmentId);
        assertTrue("a PDF was produced", pdf.length > 0);
        PdfReader reader = new PdfReader(pdf);
        try {
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(PdfTextExtractor.getTextFromPage(reader, page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    /** True when one line of the extracted text holds every token, in order. */
    private boolean rowIn(String text, String... tokens) {
        for (String line : text.split("\n")) {
            int cursor = 0;
            boolean matched = true;
            for (String token : tokens) {
                int found = line.indexOf(token, cursor);
                if (found < 0) {
                    matched = false;
                    break;
                }
                cursor = found + token.length();
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private boolean summaryRowIn(String text, String... tokens) {
        return rowIn(text, tokens);
    }

    private int occurrences(String text, String token) {
        int count = 0;
        for (int at = text.indexOf(token); at >= 0; at = text.indexOf(token, at + token.length())) {
            count++;
        }
        return count;
    }
}
