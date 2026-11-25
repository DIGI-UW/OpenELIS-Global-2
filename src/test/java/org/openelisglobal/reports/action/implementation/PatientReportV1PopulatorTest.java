package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.accreditation.service.AccreditingBodyService;
import org.openelisglobal.accreditation.service.TestAccreditationService;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.accreditation.valueholder.LogoVisibilityMode;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.service.AnalysisServiceImpl;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IReportTrackingService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.internationalization.UiMessages;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.openelisglobal.reports.action.implementation.reportBeans.PatientReportOrder;
import org.openelisglobal.reports.action.implementation.reportBeans.ReportSampleLine;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.testsupport.PdfText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * What the patient report gathers from the database for one order (FRS
 * OGC-1111, V1): the Samples block, each row's method, sample number, status
 * and completion time, the order's validators, its corrected-report reference,
 * the callbacks under critical rows and the accreditation marks of its own
 * tests.
 */
public class PatientReportV1PopulatorTest extends BaseWebContextSensitiveTest {

    private static final String ORDER = "V1RPT0000009301";

    @Autowired
    private AccreditingBodyService accreditingBodyService;

    @Autowired
    private TestAccreditationService testAccreditationService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IReportTrackingService reportTrackingService;

    private String originalBillingLabel;

    @Before
    public void loadOrder() throws Exception {
        executeDataSetWithStateManagement("testdata/patient-report-v1.xml");
        cleanAccreditation();
        originalBillingLabel = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL);
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL, "9309");
    }

    @After
    public void cleanAccreditation() {
        jdbcTemplate.update("DELETE FROM clinlims.test_accreditation");
        jdbcTemplate.update("DELETE FROM clinlims.accrediting_body");
    }

    @After
    public void restoreBillingLabel() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL,
                originalBillingLabel);
    }

    @Test
    public void samplesBlockListsThePrimarySamplesWithTheirTimesAndRejection() throws Exception {
        jdbcTemplate.update("INSERT INTO clinlims.dictionary (id, is_active, dict_entry, lastupdated)"
                + " VALUES (99301, 'Y', 'Haemolysed', now())");
        jdbcTemplate.update("UPDATE clinlims.sample_item SET reject_reason_id = 99301 WHERE id = 9303");
        String reason = dictionaryService.getDictionaryById("99301").getLocalizedName();
        assertEquals("Haemolysed", reason);

        PatientReportOrder order = run().getReportOrders().get(ORDER);

        List<ReportSampleLine> samples = order.getSamples();
        assertEquals("voided samples and aliquots get no line of their own: " + samples, 3, samples.size());
        assertEquals(new ReportSampleLine("1", "Serum", time("2026-10-01 08:10:00"), time("2026-10-01 08:50:00"), null,
                "Fasting"), samples.get(0));
        assertEquals("no collection time; received falls back to the order's",
                new ReportSampleLine("2", "Urine", null, time("2026-10-01 09:00:00"), null, null), samples.get(1));
        assertEquals(new ReportSampleLine("3", "Whole blood", time("2026-10-01 07:55:00"), time("2026-10-01 08:50:00"),
                reason, null), samples.get(2));
        assertEquals(time("2026-10-01 07:55:00"), order.getEarliestCollection());
        assertTrue(order.isCollectionsDiffer());
    }

    @Test
    public void rowsCarryTheirMethodSampleNumberStatusAndCompletionTime() throws Exception {
        PatientCILNSPClinical_vreduit report = run();

        ClinicalPatientData glucose = row(report, "9301");
        assertEquals("Hexokinase", glucose.getMethodName());
        assertEquals("1", glucose.getSampleSortOrder());
        assertTrue(glucose.isReleased());
        assertEquals(time("2026-10-01 10:15:00"), glucose.getCompletedTime());
        assertEquals("E", glucose.getFlagCode());

        ClinicalPatientData urea = row(report, "9302");
        assertFalse(urea.isReleased());
        assertNull(urea.getCompletedTime());
        assertEquals("Awaiting validation", urea.getAnalysisStatus());

        assertTrue(row(report, "9303").isSampleRejected());

        ClinicalPatientData creatinine = row(report, "9305");
        assertEquals("an aliquot's result prints its primary sample's number", "1", creatinine.getSampleSortOrder());
        assertEquals(time("2026-10-01 11:40:00"), creatinine.getCompletedTime());
    }

    @Test
    public void anAliquotWhosePrimarySampleHasNoTestOfItsOwnPrintsThatSamplesNumber() throws Exception {
        jdbcTemplate.update("INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id, typeosamp_id,"
                + " collection_date, lastupdated) VALUES (99306, 9301, 6, 20, 9301, '2026-10-01 08:20:00', now())");
        jdbcTemplate.update("UPDATE clinlims.sample_item SET parent_sample_item_id = 99306 WHERE id = 9305");
        try {
            PatientCILNSPClinical_vreduit report = run();

            assertEquals("6", row(report, "9305").getSampleSortOrder());
            assertTrue(report.getReportOrders().get(ORDER).getSamples().stream()
                    .anyMatch(sample -> "6".equals(sample.number())));
        } finally {
            jdbcTemplate.update("UPDATE clinlims.sample_item SET parent_sample_item_id = 9301 WHERE id = 9305");
            jdbcTemplate.update("DELETE FROM clinlims.sample_item WHERE id = 99306");
        }
    }

    @Test
    public void anOrderIsPartialOnlyWhileAResultAwaitsRelease() throws Exception {
        PatientReportOrder pending = run().getReportOrders().get(ORDER);
        assertFalse(pending.isComplete());
        assertNull(pending.getCompletedTime());

        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = 6, released_date = ? WHERE id = 9302",
                Timestamp.valueOf("2026-10-01 12:05:00"));
        PatientReportOrder released = run().getReportOrders().get(ORDER);

        assertTrue("the rejected sample's test does not keep the report partial", released.isComplete());
        assertEquals(time("2026-10-01 12:05:00"), released.getCompletedTime());
    }

    @Test
    public void validatorsAreTheUsersWhoseAuditEntryRecordsTheRelease() throws Exception {
        history("9301", "9303", "2026-10-01 10:00:00");
        history("9301", "9301", "2026-10-01 10:15:00.400");
        history("9301", "9303", "2026-10-01 13:00:00");
        history("9305", "9302", "2026-10-01 11:40:01");

        PatientReportOrder order = run().getReportOrders().get(ORDER);

        assertEquals(
                List.of("Sarah Chen, " + time("2026-10-01 10:15:00"), "Ben Okello, " + time("2026-10-01 11:40:00")),
                order.getValidators());
    }

    @Test
    public void aCorrectedResultNamesTheReportIssuedBeforeTheCorrection() throws Exception {
        assertFalse(run().getReportOrders().get(ORDER).isCorrected());

        issue("2026-10-01 12:00:00");
        issue("2026-10-03 08:00:00");
        jdbcTemplate.update(
                "INSERT INTO clinlims.note (id, sys_user_id, reference_id, reference_table, note_type, subject, text,"
                        + " lastupdated) VALUES (nextval('clinlims.note_seq'), 9301, 9301, ?, 'E', 'Result Note', ?, ?)",
                Integer.valueOf(AnalysisServiceImpl.getTableReferenceId()),
                MessageUtil.getMessage("note.corrected.result", Locale.ENGLISH),
                Timestamp.valueOf("2026-10-02 09:00:00"));

        PatientReportOrder order = run().getReportOrders().get(ORDER);

        assertTrue("the banner survives every reprint", order.isCorrected());
        assertEquals(time("2026-10-01 12:00:00"), order.getReplacedIssueTime());
    }

    @Test
    public void aCriticalRowPrintsItsLatestCallbackAndCountsTheEarlierOnes() throws Exception {
        callback("UNABLE_TO_REACH", "Ward 4 desk", "2026-10-01 10:18:00");
        callback("CONFIRMED", "Dr Mek", "2026-10-01 10:20:00");
        PatientCILNSPClinical_vreduit report = run();
        ClinicalPatientData glucose = row(report, "9301");
        assertNull("a high, non-critical result prints no callback", glucose.getCallback());

        glucose.setFlagCode("EE");
        report.addCallbacks();

        assertNotNull(glucose.getCallback());
        assertEquals("CONFIRMED", glucose.getCallback().status());
        assertEquals("Dr Mek", glucose.getCallback().recipient());
        assertEquals("Sarah Chen", glucose.getCallback().loggedBy());
        assertEquals(time("2026-10-01 10:20:00"), glucose.getCallback().loggedAt());
        assertEquals(1, glucose.getCallback().earlierAttempts());
    }

    @Test
    public void accreditationMarksAreResolvedForTheOrderAndMarkTestsOutsideTheScope() throws Exception {
        PatientCILNSPClinical_vreduit unmarked = run();
        assertTrue(unmarked.getReportOrders().get(ORDER).getAccreditationLogos().isEmpty());
        assertTrue("no mark, no dagger", row(unmarked, "9305").isInAccreditedScope());

        AccreditingBody input = new AccreditingBody();
        input.setCode("ISO15189");
        input.setName("ISO 15189");
        input.setExpiresOn(LocalDate.of(2030, 1, 1));
        input.setLogoVisibilityMode(LogoVisibilityMode.ANY_ACCREDITED_TEST);
        input.setThresholdPct((short) 80);
        input.setDisplayOrder((short) 1);
        Long body = accreditingBodyService.createBody(input, TEST_SYS_USER_ID).getId();
        testAccreditationService.enroll("9301", body, null, TEST_SYS_USER_ID);
        Image logo = new Image();
        logo.setImage("iso-logo".getBytes());
        logo.setDescription("v1-report-logo");
        accreditingBodyService.setLogo(body, imageService.save(logo).getId(), TEST_SYS_USER_ID);

        PatientCILNSPClinical_vreduit marked = run();

        PatientReportOrder order = marked.getReportOrders().get(ORDER);
        assertEquals(1, order.getAccreditationLogos().size());
        assertEquals(List.of("ISO 15189"), order.getAccreditingBodies());
        assertTrue(row(marked, "9301").isInAccreditedScope());
        assertFalse(row(marked, "9305").isInAccreditedScope());
    }

    @Test
    public void aReferredTestNamesThePerformingLabOnlyOnceItsResultIsBack() throws Exception {
        jdbcTemplate
                .update("INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                        + " VALUES (99301, 'Kokopo Reference Lab', 'N', 'Y', now())");
        jdbcTemplate.update("UPDATE clinlims.analysis SET referred_out = true WHERE id = 9302");
        jdbcTemplate.update("INSERT INTO clinlims.referral (id, analysis_id, organization_id, referral_type_id,"
                + " canceled, lost_status, lastupdated) VALUES (99301, 9302, 99301,"
                + " (SELECT MIN(id) FROM clinlims.referral_type), false, false, now())");
        jdbcTemplate.update("INSERT INTO clinlims.result (id, analysis_id, value, result_type, is_reportable,"
                + " lastupdated) VALUES (99301, 9302, '', 'N', 'Y', now())");
        jdbcTemplate.update("INSERT INTO clinlims.referral_result (id, referral_id, test_id, result_id, lastupdated)"
                + " VALUES (99301, 99301, 9302, 99301, now())");

        ClinicalPatientData pending = referredRow(run());
        assertTrue(pending.isReferralPending());
        assertNull("still out at the reference lab", pending.getPerformingLabName());

        jdbcTemplate.update("UPDATE clinlims.result SET value = '7.4' WHERE id = 99301");
        ClinicalPatientData back = referredRow(run());

        assertFalse(back.isReferralPending());
        assertEquals("Kokopo Reference Lab", back.getPerformingLabName());
    }

    /**
     * A result typed in for a referred test goes back to validation like any other,
     * and the report printed the value while it waited there.
     */
    @Test
    public void aReturnedReferralResultPrintsOnlyOnceItIsReleased() throws Exception {
        jdbcTemplate
                .update("INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                        + " VALUES (99301, 'Kokopo Reference Lab', 'N', 'Y', now())");
        jdbcTemplate.update("UPDATE clinlims.analysis SET referred_out = true WHERE id = 9302");
        jdbcTemplate.update("INSERT INTO clinlims.referral (id, analysis_id, organization_id, referral_type_id,"
                + " canceled, lost_status, lastupdated) VALUES (99301, 9302, 99301,"
                + " (SELECT MIN(id) FROM clinlims.referral_type), false, false, now())");
        jdbcTemplate.update("INSERT INTO clinlims.result (id, analysis_id, value, result_type, is_reportable,"
                + " lastupdated) VALUES (99301, 9302, '7.4', 'N', 'Y', now())");
        jdbcTemplate.update("INSERT INTO clinlims.referral_result (id, referral_id, test_id, result_id, lastupdated)"
                + " VALUES (99301, 99301, 9302, 99301, now())");

        PatientCILNSPClinical_vreduit awaiting = run();
        ClinicalPatientData unreleased = referredRow(awaiting);

        assertNull("an unvalidated value never prints", unreleased.getResult());
        assertFalse(unreleased.isReleased());
        assertEquals(UiMessages.current().get("report.patient.awaitingValidation"), unreleased.getAnalysisStatus());
        assertFalse(awaiting.getReportOrders().get(ORDER).isComplete());

        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = 6, released_date = ? WHERE id = 9302",
                Timestamp.valueOf("2026-10-01 12:00:00"));
        ClinicalPatientData released = referredRow(run());

        assertEquals("7.4", released.getResult());
        assertTrue(released.isReleased());
    }

    private static ClinicalPatientData referredRow(PatientCILNSPClinical_vreduit report) {
        return report.reportItems.stream().filter(item -> "9302".equals(item.getTestId()))
                .filter(item -> item.isReferralPending() || item.getPerformingLabName() != null).findFirst()
                .orElseThrow(() -> new AssertionError("no referred row for Urea: " + report.reportItems.size()));
    }

    @Test
    public void aReportBySiteAndResultDateIncludesResultsFromTheLastDayOfTheRange() throws Exception {
        jdbcTemplate
                .update("INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                        + " VALUES (99302, 'Kokopo Clinic', 'N', 'Y', now())");
        jdbcTemplate.update("INSERT INTO clinlims.sample_requester (id, sample_id, requester_id, requester_type_id,"
                + " lastupdated) VALUES (99302, 9301, 99302, (SELECT id FROM clinlims.requester_type"
                + " WHERE requester_type = 'organization'), now())");
        jdbcTemplate.update("INSERT INTO clinlims.sample_organization (id, samp_id, org_id, lastupdated)"
                + " VALUES (99302, 9301, 99302, now())");
        jdbcTemplate.update("UPDATE clinlims.analysis SET entry_date = '2026-10-01 10:00:00' WHERE id = 9301");
        LocalDate day = LocalDate.of(2026, 10, 1);

        assertTrue(analysisService.getAnalysisForSiteBetweenResultDates("99302", day, day).stream()
                .anyMatch(analysis -> "9301".equals(analysis.getId())));
        assertTrue(analysisService.getStudyAnalysisForSiteBetweenResultDates("99302", day, day).stream()
                .anyMatch(analysis -> "9301".equals(analysis.getId())));
        assertFalse(analysisService.getAnalysisForSiteBetweenResultDates("99302", day.plusDays(1), day.plusDays(1))
                .stream().anyMatch(analysis -> "9301".equals(analysis.getId())));
    }

    @Test
    public void onATwelveHourClockTimesStayShortEnoughForOneLine() throws Exception {
        String clock = ConfigurationProperties.getInstance().getPropertyValue(Property.CLOCK_24);
        try {
            ConfigurationProperties.getInstance().setPropertyValue(Property.CLOCK_24, "false");
            String afternoon = PatientCILNSPClinical_vreduit.reportTime(Timestamp.valueOf("2026-10-01 13:05:00"));
            String noon = PatientCILNSPClinical_vreduit.reportTime(Timestamp.valueOf("2026-10-01 12:00:00"));

            assertTrue(afternoon, afternoon.endsWith(" 1:05PM") && afternoon.length() <= 15);
            assertTrue(noon, noon.endsWith(" 12:00PM"));
        } finally {
            ConfigurationProperties.getInstance().setPropertyValue(Property.CLOCK_24, clock);
        }
    }

    /**
     * A partial report masks a result that awaits release, so changing that result
     * afterwards corrected nothing the patient was given; the report then claimed
     * to replace the partial one.
     */
    @Test
    public void onlyAResultReleasedBeforeTheLastReportCountsAsReported() throws Exception {
        issue("2026-10-01 10:00:00");
        assertFalse("released at 10:15, after the report",
                analysisService.releasedResultHasBeenReported(analysisService.get("9301")));
        assertFalse("never released", analysisService.releasedResultHasBeenReported(analysisService.get("9302")));

        issue("2026-10-01 12:00:00");
        assertTrue("released at 10:15, before the report",
                analysisService.releasedResultHasBeenReported(analysisService.get("9301")));
        assertFalse("still never released", analysisService.releasedResultHasBeenReported(analysisService.get("9302")));

        jdbcTemplate.update("UPDATE clinlims.analysis SET corrected = true WHERE id = 9302");
        assertTrue("a recorded correction stays one",
                analysisService.releasedResultHasBeenReported(analysisService.get("9302")));
    }

    @Test
    public void aRequestedOrderDatePrintsAsADayAndAnOrderWithoutOneShowsItsReceipt() throws Exception {
        assertEquals(time("2026-10-01 09:00:00"), run().getReportOrders().get(ORDER).getOrderDate());

        List<String> typeIds = jdbcTemplate.queryForList(
                "SELECT id FROM clinlims.observation_history_type WHERE type_name = 'requestDate'", String.class);
        String typeId = typeIds.isEmpty() ? "99301" : typeIds.get(0);
        if (typeIds.isEmpty()) {
            jdbcTemplate.update("INSERT INTO clinlims.observation_history_type (id, type_name, description,"
                    + " lastupdated) VALUES (99301, 'requestDate', 'requestDate', now())");
        }
        jdbcTemplate.update(
                "INSERT INTO clinlims.observation_history (id, sample_id, observation_history_type_id,"
                        + " value_type, value, lastupdated) VALUES (99301, 9301, ?, 'L', ?, now())",
                Integer.valueOf(typeId),
                new SimpleDateFormat(DateUtil.getDateFormat()).format(Timestamp.valueOf("2026-09-30 00:00:00")));
        try {
            String requested = run().getReportOrders().get(ORDER).getOrderDate();

            assertEquals(new SimpleDateFormat(DateUtil.getDateFormat().replace("yyyy", "yy"))
                    .format(Timestamp.valueOf("2026-09-30 00:00:00")), requested);
        } finally {
            jdbcTemplate.update("DELETE FROM clinlims.observation_history WHERE id = 99301");
            jdbcTemplate.update("DELETE FROM clinlims.observation_history_type WHERE id = 99301");
        }
    }

    @Test
    public void theReportPrintsTheGatheredOrder() throws Exception {
        PatientCILNSPClinical_vreduit report = run();

        String text = PdfText.of(report.renderReport());

        assertTrue(text, text.contains("Results: partial report"));
        assertTrue(text, text.contains("1 Serum " + time("2026-10-01 08:10:00")));
        assertTrue(text, text.contains("Glucose (Serum) 1 Hexokinase"));
        assertTrue(text, text.contains("↑ H 3.9-5.8 " + time("2026-10-01 10:15:00")));
        assertTrue(text, text.contains("Creatinine (Serum) 1 80 60-110 " + time("2026-10-01 11:40:00")));
        assertTrue(text, text.contains("Urea (Urine) 2 Awaiting validation"));
        assertTrue(text, text.contains("Potassium (Whole blood) 3 Sample rejected"));
        assertFalse(text, text.contains("Specimen"));
    }

    private PatientCILNSPClinical_vreduit run() {
        PatientCILNSPClinical_vreduit report = new PatientCILNSPClinical_vreduit() {
            @Override
            protected List<Analysis> filterAnalysesForReportUser(List<Analysis> analyses) {
                return analyses;
            }
        };
        ReportForm form = new ReportForm();
        form.setAnalysisIds(List.of("9301"));
        report.initializeReport(form);
        return report;
    }

    private static ClinicalPatientData row(PatientCILNSPClinical_vreduit report, String analysisId) {
        return report.reportItems.stream().filter(item -> analysisId.equals(item.getAnalysisId())).findFirst()
                .orElseThrow(() -> new AssertionError("no row for analysis " + analysisId));
    }

    private static String time(String timestamp) {
        return PatientCILNSPClinical_vreduit.reportTime(Timestamp.valueOf(timestamp));
    }

    private void history(String analysisId, String userId, String at) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.history (id, sys_user_id, reference_id, reference_table, timestamp, activity)"
                        + " VALUES (nextval('clinlims.history_seq'), ?, ?, ?, ?, 'U')",
                Integer.valueOf(userId), Integer.valueOf(analysisId),
                Integer.valueOf(AnalysisServiceImpl.getTableReferenceId()), Timestamp.valueOf(at));
    }

    /**
     * A patient-report issue of the order, under the document type id the tracking
     * service read at start-up: another class's fixture may since have truncated
     * document_type, so the row is put back when it is missing.
     */
    private void issue(String at) {
        Object tracking = AopTestUtils.getTargetObject(reportTrackingService);
        String typeId = (String) ReflectionTestUtils.getField(tracking, "PATIENT_DOCUMENT_TYPE_ID");
        jdbcTemplate.update(
                "INSERT INTO clinlims.document_type (id, name, description, lastupdated)"
                        + " SELECT ?, 'patientReport', 'Patient report', now() WHERE NOT EXISTS"
                        + " (SELECT 1 FROM clinlims.document_type WHERE id = ?)",
                Integer.valueOf(typeId), Integer.valueOf(typeId));
        jdbcTemplate.update(
                "INSERT INTO clinlims.document_track (id, table_id, row_id, document_type_id, report_generation_time,"
                        + " name) VALUES (nextval('clinlims.document_track_seq'), ?, 9301, ?, ?, ?)",
                Integer.valueOf(ensureReferenceTable("SAMPLE")), Integer.valueOf(typeId), Timestamp.valueOf(at),
                PatientCILNSPClinical_vreduit.REPORT_NAME);
    }

    private void callback(String status, String recipient, String at) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.critical_callback (id, result_id, analysis_id, result_value, logged_by,"
                        + " logged_at, recipient_name, status) VALUES (?, 9301, 9301, '6.3', 9301, ?, ?, ?)",
                UUID.randomUUID().toString(), Timestamp.valueOf(at), recipient, status);
    }
}
