package org.openelisglobal.common.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.testcalculated.action.util.TestCalculatedUtil;
import org.openelisglobal.testreflex.action.util.TestReflexBean;
import org.openelisglobal.testreflex.action.util.TestReflexUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A PCR test whose primary component is coded (Positive / Negative) and whose
 * Ct is a numeric component, as the molecular catalogue ships it. A reflex rule
 * and a calculation that read the Ct are judged by the Ct's own numeric type,
 * not by the type the test happens to list first: the reflex fires only when
 * its numeric condition holds, and the calculation computes from the Ct value.
 */
public class RuleEnginesOnComponentResultsIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long FLU = 96311L;
    private static final long RSV = 96312L;
    private static final long SPECIMEN = 96313L;
    private static final long POSITIVE = 963110L;
    private static final long ANALYTE = 963111L;
    private static final long TEST_ANALYTE = 963112L;
    private static final long REFLEX = 963113L;
    private static final int RULE = 963114;
    private static final int CALCULATION = 963115;
    private static final String FLU_PRIMARY = "rules-it-flu-primary";
    private static final String FLU_CT = "rules-it-flu-ct";
    private static final String RSV_PRIMARY = "rules-it-rsv-primary";
    private static final String RSV_CT = "rules-it-rsv-ct";

    @Autowired
    private ResultService resultService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private String userId;
    private long fluCtTestResult;
    private long patientId;
    private long sampleId;
    private long itemId;
    private long fluAnalysis;
    private long ctResult;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        userId = jdbc.queryForObject("SELECT min(id)::text FROM clinlims.system_user", String.class);
        ensureAnalysisStatuses();
        seedCatalogue();
        seedRules();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void aNumericReflexConditionOnACtComponentIsHonoured() {
        newOrder("35");
        List<Analysis> added = runReflexes();
        assertTrue("35 is not below 25, so the reflex must not fire: " + added, added.isEmpty());
        assertEquals(0, rsvAnalyses());

        newOrder("20");
        added = runReflexes();
        assertEquals("20 is below 25, so the reflex adds RSV once", 1, added.size());
        assertEquals(1, rsvAnalyses());
    }

    @Test
    public void aCtThatLeavesAndReentersTheRangeOrdersTheReflexTestOnce() {
        newOrder("20");
        assertEquals(1, runReflexes().size());
        jdbc.update("UPDATE clinlims.result SET value = '30' WHERE id = ?", ctResult);
        assertTrue(runReflexes().isEmpty());
        jdbc.update("UPDATE clinlims.result SET value = '20' WHERE id = ?", ctResult);
        assertTrue("the reflex test is already on the order", runReflexes().isEmpty());
        assertEquals(1, rsvAnalyses());
    }

    @Test
    public void aCalculationReadingACtComponentComputesFromItsValue() {
        newOrder("20");
        List<Analysis> added = inTransaction(() -> new TestCalculatedUtil()
                .addNewTestsToDBForCalculatedTests(List.of(resultSetFor(ctResult)), userId));

        assertEquals(1, added.size());
        assertEquals("40.0", rsvCtValue(added.get(0).getId()));
        assertTrue(jdbc.queryForObject("SELECT result_calculated FROM clinlims.analysis WHERE id = ?::numeric",
                Boolean.class, added.get(0).getId()));
    }

    private List<Analysis> runReflexes() {
        return inTransaction(() -> {
            TestReflexBean bean = new TestReflexBean();
            bean.setResult(resultService.get(String.valueOf(ctResult)));
            bean.setSample(sampleService.get(String.valueOf(sampleId)));
            bean.setPatient(patientService.get(String.valueOf(patientId)));
            bean.setTriggersToSelectedReflexesMap(new HashMap<>());
            return new TestReflexUtil().addNewTestsToDBForReflexTests(new java.util.ArrayList<>(List.of(bean)), userId);
        });
    }

    private ResultSet resultSetFor(long resultId) {
        return new ResultSet(resultService.get(String.valueOf(resultId)), null, null,
                patientService.get(String.valueOf(patientId)), sampleService.get(String.valueOf(sampleId)),
                new HashMap<>(), false);
    }

    private <T> T inTransaction(java.util.function.Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> work.get());
    }

    private int rsvAnalyses() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.analysis a JOIN clinlims.sample_item si"
                        + " ON si.id = a.sampitem_id WHERE si.samp_id = ? AND a.test_id = ?",
                Integer.class, sampleId, RSV);
    }

    private String rsvCtValue(String analysisId) {
        return jdbc.queryForObject(
                "SELECT r.value FROM clinlims.result r JOIN clinlims.test_result tr"
                        + " ON tr.id = r.test_result_id WHERE r.analysis_id = ?::numeric AND tr.component_id = ?",
                String.class, analysisId, RSV_CT);
    }

    private void seedCatalogue() {
        long specimenName = next("localization_seq");
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, 'RulesIT', NOW())",
                specimenName);
        jdbc.update("INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active,"
                + " sort_order, name_localization_id, lastupdated) VALUES (?, 'RulesIT swab', 'H', 'RIS', true,"
                + " 9631, ?, NOW())", SPECIMEN, specimenName);
        jdbc.update("INSERT INTO clinlims.dictionary (id, dict_entry, is_active, lastupdated)"
                + " VALUES (?, 'RulesIT Positive', 'Y', NOW())", POSITIVE);
        for (long test : new long[] { FLU, RSV }) {
            long name = next("localization_seq");
            jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, 'RulesIT', NOW())",
                    name);
            jdbc.update(
                    "INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                            + " VALUES (nextval('clinlims.localization_value_seq'), ?, 'en', ?, NOW())",
                    name, "RulesIT" + test);
            jdbc.update("INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated,"
                    + " name_localization_id, reporting_name_localization_id) VALUES (?, ?, ?, 'Y', ?, NOW(), ?, ?)",
                    test, "RulesIT" + test, "rules it " + test, UUID.randomUUID().toString(), name, name);
            jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id, is_panel)"
                    + " VALUES (nextval('clinlims.sample_type_test_seq'), ?, ?, false)", SPECIMEN, test);
        }
        component(FLU_PRIMARY, FLU, "PRIMARY", "Influenza", "D", true, 0);
        component(FLU_CT, FLU, "Ct", "Flu Ct", "N", false, 1);
        component(RSV_PRIMARY, RSV, "PRIMARY", "RSV", "D", true, 0);
        component(RSV_CT, RSV, "RSV Ct", "RSV Ct", "N", false, 1);
        // The numeric Ct rows come first, as the catalogue loader writes them.
        fluCtTestResult = testResult(FLU, FLU_CT, "N", null);
        testResult(RSV, RSV_CT, "N", null);
        testResult(FLU, FLU_PRIMARY, "D", String.valueOf(POSITIVE));
        testResult(RSV, RSV_PRIMARY, "D", String.valueOf(POSITIVE));
    }

    private void seedRules() {
        jdbc.update("INSERT INTO clinlims.analyte (id, name, is_active, lastupdated) VALUES (?, 'RulesIT reflex', 'Y',"
                + " NOW())", ANALYTE);
        jdbc.update("INSERT INTO clinlims.test_analyte (id, test_id, analyte_id, result_group, testalyt_type,"
                + " lastupdated) VALUES (?, ?, ?, '30', 'R', NOW())", TEST_ANALYTE, FLU, ANALYTE);
        jdbc.update("INSERT INTO clinlims.reflex_rule (id, rule_name, overall, toggled, active, analyte_id)"
                + " VALUES (?, 'RulesIT reflex', 'ANY', true, true, ?)", RULE, (int) ANALYTE);
        jdbc.update("INSERT INTO clinlims.test_reflex (id, test_id, tst_rslt_id, add_test_id, relation,"
                + " non_dictionary_value, component_id, sample_type_id, add_sample_type_id, internal_note,"
                + " test_analyte_id, lastupdated) VALUES (?, ?, ?, ?, 'LESS_THAN', '25.0', ?, ?, ?, 'rules it', ?,"
                + " NOW())", REFLEX, FLU, fluCtTestResult, RSV, FLU_CT, SPECIMEN, SPECIMEN, TEST_ANALYTE);
        jdbc.update("INSERT INTO clinlims.calculation (id, name, sample_id, test_id, result, toggled, active,"
                + " component_id, last_updated) VALUES (?, 'RulesIT Ct doubled', ?, ?, 'RulesIT', true, true, ?,"
                + " NOW())", CALCULATION, (int) SPECIMEN, (int) RSV, RSV_CT);
        jdbc.update(
                "INSERT INTO clinlims.calculation_operation (id, type, sample_id, operation_order, value,"
                        + " calculation_id, component_id, sample_type_id) VALUES (?, 'TEST_RESULT', ?, 0, ?, ?, ?, ?)",
                CALCULATION * 10, (int) SPECIMEN, String.valueOf(FLU), CALCULATION, FLU_CT, SPECIMEN);
        jdbc.update("INSERT INTO clinlims.calculation_operation (id, type, operation_order, value, calculation_id)"
                + " VALUES (?, 'MATH_FUNCTION', 1, '*', ?)", CALCULATION * 10 + 1, CALCULATION);
        jdbc.update("INSERT INTO clinlims.calculation_operation (id, type, operation_order, value, calculation_id)"
                + " VALUES (?, 'INTEGER', 2, '2', ?)", CALCULATION * 10 + 2, CALCULATION);
    }

    private void newOrder(String ctValue) {
        if (patientId == 0) {
            long personId = next("person_seq");
            patientId = next("patient_seq");
            jdbc.update("INSERT INTO clinlims.person (id, last_name, first_name, lastupdated)"
                    + " VALUES (?, 'RULESIT', 'Test', NOW())", personId);
            LocalDate birthDate = LocalDate.now().minusYears(30);
            jdbc.update(
                    "INSERT INTO clinlims.patient (id, person_id, gender, birth_date, entered_birth_date,"
                            + " is_merged, lastupdated) VALUES (?, ?, 'F', ?, ?, false, NOW())",
                    patientId, personId, Timestamp.valueOf(birthDate.atStartOfDay()),
                    DateUtil.formatDateAsText(java.sql.Date.valueOf(birthDate)));
        }
        String notStarted = statusService.getStatusID(AnalysisStatus.NotStarted);
        String accepted = statusService.getStatusID(AnalysisStatus.TechnicalAcceptance);
        sampleId = next("sample_seq");
        jdbc.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date, is_confirmation,"
                + " domain) VALUES (?, ?, NOW(), NOW(), false, 'H')", sampleId, "RULESIT" + sampleId);
        jdbc.update("INSERT INTO clinlims.sample_human (id, samp_id, patient_id) VALUES (?, ?, ?)",
                next("sample_human_seq"), sampleId, patientId);
        itemId = next("sample_item_seq");
        jdbc.update("INSERT INTO clinlims.sample_item (id, sort_order, status_id, samp_id, typeosamp_id)"
                + " VALUES (?, 1, ?::numeric, ?, ?::numeric)", itemId, notStarted, sampleId, SPECIMEN);
        fluAnalysis = next("analysis_seq");
        jdbc.update("INSERT INTO clinlims.analysis (id, sampitem_id, test_id, status_id, revision, analysis_type,"
                + " started_date, is_reportable, lastupdated) VALUES (?, ?, ?, ?::numeric, 1, 'ROUTINE', NOW(), 'Y',"
                + " NOW())", fluAnalysis, itemId, FLU, accepted);
        ctResult = next("result_seq");
        jdbc.update(
                "INSERT INTO clinlims.result (id, analysis_id, test_result_id, result_type, value, analyte_id,"
                        + " sort_order, lastupdated) VALUES (?, ?, ?, 'N', ?, ?, 0, NOW())",
                ctResult, fluAnalysis, fluCtTestResult, ctValue, ANALYTE);
    }

    private void component(String id, long test, String code, String label, String resultType, boolean primary,
            int order) {
        jdbc.update(
                "INSERT INTO clinlims.test_result_component (id, test_id, code, label, display_order,"
                        + " result_type, allow_multiple_readings, is_active, is_primary, show_on_report, lastupdated)"
                        + " VALUES (?, ?, ?, ?, ?, ?, false, 'Y', ?, true, NOW())",
                id, test, code, label, order, resultType, primary);
    }

    private long testResult(long test, String componentId, String type, String value) {
        long id = next("test_result_seq");
        jdbc.update(
                "INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, value, sort_order, is_active,"
                        + " component_id, lastupdated) VALUES (?, ?, ?, ?, 1, true, ?, NOW())",
                id, test, type, value, componentId);
        return id;
    }

    private void ensureAnalysisStatuses() {
        for (String name : new String[] { "Not Tested", "Test Canceled", "Technical Acceptance", "Technical Rejected",
                "Biologist Rejection", "Finalized" }) {
            jdbc.update("INSERT INTO clinlims.status_of_sample (id, description, code, status_type, name, is_active,"
                    + " lastupdated) SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM clinlims.status_of_sample), ?, '1',"
                    + " 'ANALYSIS', ?, 'Y', NOW() WHERE NOT EXISTS (SELECT 1 FROM clinlims.status_of_sample WHERE"
                    + " status_type = 'ANALYSIS' AND name = ?)", name, name, name);
        }
        statusService.refreshCache();
    }

    private long next(String sequence) {
        return jdbc.queryForObject("SELECT nextval('clinlims." + sequence + "')", Long.class);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test_result_map WHERE result_calculation_id IN (SELECT id FROM"
                + " clinlims.result_calculation WHERE calculation_id = ?)", CALCULATION);
        jdbc.update("DELETE FROM clinlims.test_operations WHERE result_calculation_id IN (SELECT id FROM"
                + " clinlims.result_calculation WHERE calculation_id = ?)", CALCULATION);
        jdbc.update("DELETE FROM clinlims.result_calculation WHERE calculation_id = ?", CALCULATION);
        jdbc.update("DELETE FROM clinlims.calculation_operation WHERE calculation_id = ?", CALCULATION);
        jdbc.update("DELETE FROM clinlims.calculation WHERE id = ?", CALCULATION);
        jdbc.update("DELETE FROM clinlims.note WHERE reference_id IN (SELECT a.id FROM clinlims.analysis a JOIN"
                + " clinlims.sample_item si ON si.id = a.sampitem_id JOIN clinlims.sample s ON s.id = si.samp_id"
                + " WHERE s.accession_number LIKE 'RULESIT%')");
        jdbc.update("DELETE FROM clinlims.result WHERE analysis_id IN (SELECT a.id FROM clinlims.analysis a JOIN"
                + " clinlims.sample_item si ON si.id = a.sampitem_id JOIN clinlims.sample s ON s.id = si.samp_id"
                + " WHERE s.accession_number LIKE 'RULESIT%')");
        jdbc.update("DELETE FROM clinlims.analysis WHERE sampitem_id IN (SELECT si.id FROM clinlims.sample_item si"
                + " JOIN clinlims.sample s ON s.id = si.samp_id WHERE s.accession_number LIKE 'RULESIT%')");
        jdbc.update("DELETE FROM clinlims.sample_item WHERE samp_id IN (SELECT id FROM clinlims.sample WHERE"
                + " accession_number LIKE 'RULESIT%')");
        jdbc.update("DELETE FROM clinlims.sample_human WHERE samp_id IN (SELECT id FROM clinlims.sample WHERE"
                + " accession_number LIKE 'RULESIT%')");
        jdbc.update("DELETE FROM clinlims.sample WHERE accession_number LIKE 'RULESIT%'");
        jdbc.update("DELETE FROM clinlims.patient WHERE person_id IN (SELECT id FROM clinlims.person WHERE"
                + " last_name = 'RULESIT')");
        jdbc.update("DELETE FROM clinlims.person WHERE last_name = 'RULESIT'");
        jdbc.update("DELETE FROM clinlims.test_reflex WHERE id = ?", REFLEX);
        jdbc.update("DELETE FROM clinlims.reflex_rule WHERE id = ?", RULE);
        jdbc.update("DELETE FROM clinlims.test_analyte WHERE id = ?", TEST_ANALYTE);
        jdbc.update("DELETE FROM clinlims.analyte WHERE id = ?", ANALYTE);
        jdbc.update("DELETE FROM clinlims.test_result WHERE test_id IN (?, ?)", FLU, RSV);
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id IN (?, ?)", FLU, RSV);
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id IN (?, ?)", FLU, RSV);
        jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?)", FLU, RSV);
        jdbc.update("DELETE FROM clinlims.dictionary WHERE id = ?", POSITIVE);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", SPECIMEN);
        jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id IN (SELECT id FROM"
                + " clinlims.localization WHERE description = 'RulesIT')");
        jdbc.update("DELETE FROM clinlims.localization WHERE description = 'RulesIT'");
        patientId = 0;
    }
}
