package org.openelisglobal.dashboard.management.surveillance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.StringReader;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dashboard.management.surveillance.service.SurveillanceImportResult;
import org.openelisglobal.dashboard.management.surveillance.service.SurveillanceImportResult.SkippedRow;
import org.openelisglobal.dashboard.management.surveillance.service.SurveillanceMappingService;
import org.springframework.beans.factory.annotation.Autowired;

public class SurveillanceMappingServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String HEADER = "program_code,test,metric_label,metric_key,match_mode,positive_result_codes,"
            + "denominator_result_codes,numeric_threshold,display_order\n";

    private static final String SIX_ROWS = HEADER //
            + "TB,85362-2,MTB detected,MTB_DETECTION,ALL_ABNORMAL,,,,1\n"
            + "TB,Xpert MTB/RIF(Sputum),MTB detected or trace,MTB_CODES,SPECIFIC_CODES,\"MTB DETECTED, MTB TRACE\",,,2\n"
            + "TB,85362-2,RIF resistance,RIF_RESISTANCE,SPECIFIC_CODES,RIF DETECTED,MTB DETECTED,,3\n"
            + "HIV,HIVVIRALLOAD(Plasma),VL suppressed,VL_SUPPRESSED,NUMERIC_BELOW,,,1000,1\n"
            + "HIV,hiv rapid test,HIV positive,HIV_POSITIVE,all_abnormal,,,,2\n"
            + "HIV,HIVVIRALLOAD(Serum),VL high,VL_HIGH,NUMERIC_ABOVE,,,1000,3\n";

    @Autowired
    private SurveillanceMappingService surveillanceMappingService;

    private String userId;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/surveillance-mappings.xml");
        jdbcTemplate
                .update("DELETE FROM clinlims.surveillance_program WHERE code NOT IN ('TB', 'HIV', 'MALARIA', 'AMR')");
        userId = jdbcTemplate.queryForObject("SELECT id FROM clinlims.system_user WHERE login_name = 'admin'",
                String.class);
    }

    @Test
    public void seedsTheFourPrograms() {
        assertEquals(List.of("TB", "HIV", "MALARIA", "AMR"), jdbcTemplate
                .queryForList("SELECT code FROM clinlims.surveillance_program ORDER BY display_order", String.class));
    }

    @Test
    public void seedMapsActiveTestsByLoincButNotVectorAssaysAndOnlyOnce() {
        executeSqlScript("classpath:liquibase/dashboards/002-surveillance-mapping-seed.sql", false);
        executeSqlScript("classpath:liquibase/dashboards/002-surveillance-mapping-seed.sql", false);

        assertEquals(
                List.of("HIV 8802 VL_SUPPRESSED NUMERIC_BELOW 1000", "HIV 8803 VL_SUPPRESSED NUMERIC_BELOW 1000",
                        "HIV 8806 HIV_POSITIVE ALL_ABNORMAL ", "TB 8801 MTB_DETECTION ALL_ABNORMAL "),
                jdbcTemplate.queryForList(
                        "SELECT p.code || ' ' || m.test_id || ' ' || m.metric_key || ' '"
                                + " || m.match_mode || ' ' || coalesce(trim_scale(m.numeric_threshold)::text, '')"
                                + " FROM clinlims.test_surveillance_mapping m"
                                + " JOIN clinlims.surveillance_program p ON p.id = m.program_id ORDER BY 1",
                        String.class));
    }

    @Test
    public void reimportUpdatesOnlyTheChangedRow() {
        SurveillanceImportResult first = importCsv(SIX_ROWS);
        SurveillanceImportResult second = importCsv(SIX_ROWS.replace("VL high", "VL not suppressed"));

        assertEquals(new SurveillanceImportResult(6, 0, List.of()), first);
        assertEquals(new SurveillanceImportResult(0, 1, List.of()), second);
        assertEquals(6, mappingCount());
        assertEquals("VL not suppressed",
                jdbcTemplate.queryForObject(
                        "SELECT metric_label FROM clinlims.test_surveillance_mapping WHERE metric_key = 'VL_HIGH'",
                        String.class));
        assertEquals("MTB DETECTED,MTB TRACE", jdbcTemplate.queryForObject(
                "SELECT positive_result_codes FROM clinlims.test_surveillance_mapping WHERE metric_key = 'MTB_CODES'",
                String.class));
    }

    @Test
    public void aLoincRowMapsEveryActiveTestCarryingIt() {
        SurveillanceImportResult result = importCsv(
                HEADER + "HIV,10351-5,VL suppressed,VL_SUPPRESSED,NUMERIC_BELOW,,,1000,1\n");

        assertEquals(new SurveillanceImportResult(2, 0, List.of()), result);
    }

    @Test
    public void skipsEachInvalidRowWithItsReason() {
        SurveillanceImportResult result = importCsv(HEADER //
                + "TB,85362-2,MTB detected,MTB_DETECTION,ALL_ABNORMAL,,,,1\n" // 2: valid
                + "TB,85362-2,MTB again,MTB_DETECTION,ALL_ABNORMAL,,,,1\n" // 3
                + "COVID,85362-2,Positive,COVID_POSITIVE,ALL_ABNORMAL,,,,1\n" // 4
                + "TB,No Such Test,Positive,POSITIVE,ALL_ABNORMAL,,,,1\n" // 5
                + "MALARIA,Malaria Rapid Test,RDT positive,RDT_POSITIVE,ALL_ABNORMAL,,,,1\n" // 6
                + "TB,85362-2,,EMPTY_LABEL,ALL_ABNORMAL,,,,1\n" // 7
                + "TB,85362-2,Lower key,mtb_lower,ALL_ABNORMAL,,,,1\n" // 8
                + "TB,85362-2,Bad mode,BAD_MODE,SOME_MODE,,,,1\n" // 9
                + "TB,85362-2,No codes,NO_CODES,SPECIFIC_CODES,,,,1\n" // 10
                + "TB,85362-2,Stray codes,STRAY_CODES,ALL_ABNORMAL,POSITIVE,,,1\n" // 11
                + "HIV,HIV Rapid Test,No threshold,NO_THRESHOLD,NUMERIC_BELOW,,,,1\n" // 12
                + "HIV,HIV Rapid Test,Stray threshold,STRAY_THRESHOLD,ALL_ABNORMAL,,,1000,1\n" // 13
                + "HIV,HIV Rapid Test,Word threshold,WORD_THRESHOLD,NUMERIC_ABOVE,,,a lot,1\n" // 14
                + "HIV,HIV Rapid Test,Negative order,NEGATIVE_ORDER,ALL_ABNORMAL,,,,-1\n" // 15
                + "HIV,HIV Rapid Test,Short row\n"); // 16

        assertEquals(1, result.rowsAdded());
        assertEquals(List.of(new SkippedRow(3, "Repeats row 2"), new SkippedRow(4, "Unknown program: COVID"),
                new SkippedRow(5, "Unknown test: No Such Test"), new SkippedRow(6, "Unknown test: Malaria Rapid Test"),
                new SkippedRow(7, "metric_label is required, at most 128 characters"),
                new SkippedRow(8,
                        "metric_key must be upper-case letters, digits and underscores, at most 64: mtb_lower"),
                new SkippedRow(9, "Unknown match_mode: SOME_MODE"),
                new SkippedRow(10, "SPECIFIC_CODES needs positive_result_codes"),
                new SkippedRow(11, "positive_result_codes apply only to SPECIFIC_CODES"),
                new SkippedRow(12, "NUMERIC_BELOW needs numeric_threshold"),
                new SkippedRow(13, "numeric_threshold applies only to NUMERIC_BELOW and NUMERIC_ABOVE"),
                new SkippedRow(14, "numeric_threshold is not a number: a lot"),
                new SkippedRow(15, "display_order must not be negative"),
                new SkippedRow(16, "Expected 9 columns, found 3")), result.rowsSkipped());
        assertEquals(1, mappingCount());
    }

    @Test
    public void aNamedUnknownProgramIsCreatedOnceAfterTheSeededOnes() {
        String csv = HEADER.replace("display_order\n", "display_order,program_name\n")
                + "COVID,85362-2,SARS-CoV-2 detected,COVID_DETECTED,ALL_ABNORMAL,,,,1,COVID-19\n"
                + "COVID,HIV Rapid Test,Reactive,COVID_REACTIVE,ALL_ABNORMAL,,,,2,COVID-19\n"
                + "HEPB,No Such Test,Positive,HEPB_POSITIVE,ALL_ABNORMAL,,,,1,Hepatitis B\n"
                + "HEPC,85362-2,Positive,HEPC_POSITIVE,ALL_ABNORMAL,,,,1,\n";

        SurveillanceImportResult result = importCsv(csv);

        assertEquals(new SurveillanceImportResult(2, 0,
                List.of(new SkippedRow(4, "Unknown test: No Such Test"), new SkippedRow(5, "Unknown program: HEPC"))),
                result);
        assertEquals(List.of("COVID COVID-19 5 true"), jdbcTemplate.queryForList(
                "SELECT code || ' ' || display_name || ' ' || display_order || ' ' || is_active"
                        + " FROM clinlims.surveillance_program WHERE code NOT IN ('TB', 'HIV', 'MALARIA', 'AMR')",
                String.class));
    }

    @Test
    public void rejectsAFileMissingAColumn() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> importCsv("program_code,test,metric_label,metric_key,match_mode\n"));

        assertEquals("Missing columns: positive_result_codes, denominator_result_codes, numeric_threshold,"
                + " display_order", e.getMessage());
    }

    private SurveillanceImportResult importCsv(String csv) {
        return surveillanceMappingService.importCsv(new StringReader(csv), "mappings.csv", userId);
    }

    private int mappingCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.test_surveillance_mapping", Integer.class);
    }
}
