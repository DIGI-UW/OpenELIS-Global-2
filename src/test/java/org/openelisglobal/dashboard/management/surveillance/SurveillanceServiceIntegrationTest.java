package org.openelisglobal.dashboard.management.surveillance;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.dashboard.management.surveillance.SurveillanceResponse.Metric;
import org.openelisglobal.dashboard.management.surveillance.SurveillanceResponse.Program;
import org.springframework.beans.factory.annotation.Autowired;

public class SurveillanceServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final ScopeRange MARCH = new ScopeRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 1, 29), LocalDate.of(2026, 2, 28));
    private static final String IN_SCOPE = "2026-03-15 10:00:00";
    private static final String PRIOR = "2026-02-15 10:00:00";

    private static final String RAPID = "8901";
    private static final String VL_PLASMA = "8902";
    private static final String VL_SERUM = "8903";
    private static final String RIF = "8904";
    private static final String POSITIVE = "8901";
    private static final String NEGATIVE = "8902";
    private static final String INVALID = "8903";

    private final AtomicInteger ids = new AtomicInteger(89100);

    @Autowired
    private SurveillanceService surveillanceService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/surveillance-cards.xml");
        jdbcTemplate
                .update("DELETE FROM clinlims.surveillance_program WHERE code NOT IN ('TB', 'HIV', 'MALARIA', 'AMR')");
        jdbcTemplate.update("DELETE FROM clinlims.sample_item WHERE id = 8901");
        jdbcTemplate.update("DELETE FROM clinlims.sample WHERE id = 8901");
        jdbcTemplate.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date)"
                + " VALUES (8901, 'SURV-8901', now(), now())");
        jdbcTemplate.update("INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id)"
                + " SELECT 8901, 8901, 1, min(id) FROM clinlims.status_of_sample");
    }

    @Test
    public void specificCodesCountChosenValuesCaseInsensitively() {
        map("HIV", RAPID, "HIV_POSITIVE", 1, "SPECIFIC_CODES", "positive", null, null);
        coded(RAPID, IN_SCOPE, POSITIVE, "8901");
        coded(RAPID, IN_SCOPE, POSITIVE, "8901");
        coded(RAPID, IN_SCOPE, NEGATIVE, "8902");
        coded(RAPID, IN_SCOPE, INVALID, "8903");

        assertEquals(new Metric("HIV_POSITIVE", "HIV_POSITIVE label", 50.0, 2, 4, null), metric("HIV", 0));
    }

    @Test
    public void allAbnormalReadsTheOptionFlagForCodedAndTheLimitsForNumeric() {
        map("HIV", RAPID, "HIV_ABNORMAL", 1, "ALL_ABNORMAL", null, null, null);
        map("HIV", VL_PLASMA, "VL_ABNORMAL", 2, "ALL_ABNORMAL", null, null, null);
        coded(RAPID, IN_SCOPE, POSITIVE, "8901");
        coded(RAPID, IN_SCOPE, NEGATIVE, "8902");
        coded(RAPID, IN_SCOPE, INVALID, "8903");
        coded(RAPID, IN_SCOPE, NEGATIVE, "8901");
        numeric(VL_PLASMA, IN_SCOPE, "120", 0.0, 100.0);
        numeric(VL_PLASMA, IN_SCOPE, "50", 0.0, 100.0);

        assertEquals(new Metric("HIV_ABNORMAL", "HIV_ABNORMAL label", 25.0, 1, 4, null), metric("HIV", 0));
        assertEquals(new Metric("VL_ABNORMAL", "VL_ABNORMAL label", 50.0, 1, 2, null), metric("HIV", 1));
    }

    @Test
    public void aValueWithSeveralOptionsIsAbnormalOnlyWhenEveryActiveOneSaysSo() {
        jdbcTemplate.update("INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, value, is_normal, is_active)"
                + " VALUES (8911, 8901, 'D', '8901', true, false), (8912, 8901, 'D', '8902', false, true)");
        map("HIV", RAPID, "HIV_ABNORMAL", 1, "ALL_ABNORMAL", null, null, null);
        coded(RAPID, IN_SCOPE, POSITIVE, "8901");
        coded(RAPID, IN_SCOPE, NEGATIVE, "8902");

        assertEquals(new Metric("HIV_ABNORMAL", "HIV_ABNORMAL label", 50.0, 1, 2, null), metric("HIV", 0));
    }

    @Test
    public void numericBelowReadsMarkedAndSeparatedNumbersAndDropsTheRest() {
        map("HIV", VL_PLASMA, "VL_SUPPRESSED", 1, "NUMERIC_BELOW", null, null, "1000");
        numeric(VL_PLASMA, IN_SCOPE, "<40", null, null);
        numeric(VL_PLASMA, IN_SCOPE, "1,200", null, null);
        numeric(VL_PLASMA, IN_SCOPE, "500", null, null);
        numeric(VL_PLASMA, IN_SCOPE, "Target not detected", null, null);

        assertEquals(new Metric("VL_SUPPRESSED", "VL_SUPPRESSED label", 66.7, 2, 3, null), metric("HIV", 0));
    }

    @Test
    public void testsSharingAKeyPoolIntoOneMetricWithADeltaAgainstThePriorScope() {
        map("HIV", VL_PLASMA, "VL_SUPPRESSED", 1, "NUMERIC_BELOW", null, null, "1000");
        map("HIV", VL_SERUM, "VL_SUPPRESSED", 1, "NUMERIC_BELOW", null, null, "1000");
        numeric(VL_PLASMA, IN_SCOPE, "300", null, null);
        numeric(VL_SERUM, IN_SCOPE, "5000", null, null);
        numeric(VL_PLASMA, PRIOR, "100", null, null);

        Program hiv = program("HIV");
        assertEquals(List.of(new Metric("VL_SUPPRESSED", "VL_SUPPRESSED label", 50.0, 1, 2, -50.0)), hiv.metrics());
        assertEquals(2, hiv.testCount());
    }

    @Test
    public void denominatorCodesNarrowWhatCountsAsTested() {
        map("TB", RIF, "RIF_RESISTANT", 1, "SPECIFIC_CODES", "RIF DETECTED", "RIF DETECTED,RIF NOT DETECTED", null);
        coded(RIF, IN_SCOPE, "8904", "8905");
        coded(RIF, IN_SCOPE, "8905", "8906");
        coded(RIF, IN_SCOPE, "8906", "8907");

        assertEquals(new Metric("RIF_RESISTANT", "RIF_RESISTANT label", 50.0, 1, 2, null), metric("TB", 0));
    }

    @Test
    public void aCardPerProgramWithAnActiveMetricInDisplayOrderNamedByKeyOrText() {
        jdbcTemplate.update("INSERT INTO clinlims.surveillance_program (id, code, display_name, display_order)"
                + " VALUES (nextval('clinlims.surveillance_program_seq'), 'COVID', 'COVID-19', 0)");
        map("COVID", RAPID, "COVID_POSITIVE", 1, "ALL_ABNORMAL", null, null, null);
        map("HIV", RAPID, "HIV_POSITIVE", 1, "ALL_ABNORMAL", null, null, null);
        map("MALARIA", RAPID, "RDT_POSITIVE", 1, "ALL_ABNORMAL", null, null, null);
        jdbcTemplate.update("UPDATE clinlims.test_surveillance_mapping SET is_active = false"
                + " WHERE metric_key = 'RDT_POSITIVE'");

        List<Program> programs = surveillanceService.getSurveillance(MARCH).programs();

        assertEquals(List.of("COVID COVID-19", "HIV label.mgmt.surveillance.hiv", "AMR label.mgmt.surveillance.amr"),
                programs.stream().map(p -> p.code() + " " + p.name()).toList());
        assertEquals(new Metric("COVID_POSITIVE", "COVID_POSITIVE label", null, 0, 0, null),
                programs.get(0).metrics().get(0));
    }

    @Test
    public void aCardShowsItsFirstThreeMetrics() {
        map("HIV", RAPID, "FOURTH", 4, "ALL_ABNORMAL", null, null, null);
        map("HIV", RAPID, "SECOND", 2, "ALL_ABNORMAL", null, null, null);
        map("HIV", RAPID, "FIRST", 1, "ALL_ABNORMAL", null, null, null);
        map("HIV", RAPID, "THIRD", 3, "ALL_ABNORMAL", null, null, null);

        assertEquals(List.of("FIRST", "SECOND", "THIRD"), program("HIV").metrics().stream().map(Metric::key).toList());
    }

    private Program program(String code) {
        return surveillanceService.getSurveillance(MARCH).programs().stream().filter(p -> p.code().equals(code))
                .findFirst().orElseThrow();
    }

    private Metric metric(String code, int index) {
        return program(code).metrics().get(index);
    }

    private void map(String programCode, String testId, String key, int order, String mode, String positive,
            String denominator, String threshold) {
        jdbcTemplate.update("INSERT INTO clinlims.test_surveillance_mapping (id, test_id, program_id, metric_label,"
                + " metric_key, match_mode, positive_result_codes, denominator_result_codes, numeric_threshold,"
                + " display_order) SELECT nextval('clinlims.test_surveillance_mapping_seq'), CAST(? AS numeric), p.id,"
                + " ?, ?, ?, ?, ?, CAST(? AS numeric), ? FROM clinlims.surveillance_program p WHERE p.code = ?", testId,
                key + " label", key, mode, positive, denominator, threshold, order, programCode);
    }

    private void coded(String testId, String released, String dictionaryId, String testResultId) {
        result(testId, released, "D", dictionaryId, testResultId, null, null);
    }

    private void numeric(String testId, String released, String value, Double min, Double max) {
        result(testId, released, value.matches("[0-9.]+") ? "N" : "A", value, null, min, max);
    }

    private void result(String testId, String released, String type, String value, String testResultId, Double min,
            Double max) {
        int analysisId = ids.incrementAndGet();
        jdbcTemplate.update(
                "INSERT INTO clinlims.analysis (id, sampitem_id, analysis_type, test_id, released_date,"
                        + " lastupdated) VALUES (?, 8901, 'MANUAL', CAST(? AS numeric), CAST(? AS timestamp), now())",
                analysisId, testId, released);
        jdbcTemplate.update(
                "INSERT INTO clinlims.result (id, analysis_id, result_type, value, test_result_id,"
                        + " min_normal, max_normal, lastupdated) VALUES (?, ?, ?, ?, CAST(? AS numeric), ?, ?, now())",
                ids.incrementAndGet(), analysisId, type, value, testResultId, min, max);
    }
}
