package org.openelisglobal.testcalculated.action.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.SampleGrouping;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.testcalculated.service.CalculationRuleViolation;
import org.openelisglobal.testcalculated.service.TestCalculationService;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.Operation;
import org.openelisglobal.testresult.service.TestResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class CodedCalculationIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long PCR_TEST = 95491L;
    private static final long SPECIMEN = 95492L;

    @Autowired
    private TestCalculatedUtil testCalculatedUtil;
    @Autowired
    private TestCalculationService calculationService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private AnalyzerResultsService analyzerResultsService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private final String call = UUID.randomUUID().toString();
    private final String control = UUID.randomUUID().toString();
    private final String outcome = UUID.randomUUID().toString();
    private long detected;
    private long notDetected;
    private long invalid;
    private long patientId;
    private long sampleId;
    private long analysisId;
    private long itemId;
    private Long callResult;
    private Long controlResult;
    private String userId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        dropSchemaUpdateUniqueKeys();
        cleanup();
        userId = jdbc.queryForObject("SELECT min(id)::text FROM clinlims.system_user", String.class);
        ensureAnalysisStatuses();
        long specimenName = next("localization_seq");
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, 'CodedCalc', NOW())",
                specimenName);
        jdbc.update("INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active,"
                + " sort_order, name_localization_id, lastupdated) VALUES (?, 'CodedCalc specimen', 'H', 'CCS',"
                + " true, 9492, ?, NOW())", SPECIMEN, specimenName);
        long testName = next("localization_seq");
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, 'CodedCalc', NOW())",
                testName);
        jdbc.update(
                "INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                        + " VALUES (nextval('clinlims.localization_value_seq'), ?, 'en', 'CodedCalc PCR', NOW())",
                testName);
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated,"
                        + " name_localization_id, reporting_name_localization_id) VALUES (?, 'CodedCalc PCR',"
                        + " 'coded calc pcr', 'Y', ?, NOW(), ?, ?)",
                PCR_TEST, UUID.randomUUID().toString(), testName, testName);
        detected = answer("CodedCalc Detected");
        notDetected = answer("CodedCalc Not detected");
        invalid = answer("CodedCalc Invalid");
        component(outcome, "RESULT", true, 0);
        component(call, "CALL", false, 1);
        component(control, "IC", false, 2);
        for (String component : new String[] { call, control, outcome }) {
            testResult(component, detected);
            testResult(component, notDetected);
        }
        testResult(outcome, invalid);

        long personId = next("person_seq");
        patientId = next("patient_seq");
        jdbc.update("INSERT INTO clinlims.person (id, last_name, first_name, lastupdated)"
                + " VALUES (?, 'CODEDCALC', 'Test', NOW())", personId);
        LocalDate birthDate = LocalDate.now().minusYears(30);
        jdbc.update(
                "INSERT INTO clinlims.patient (id, person_id, gender, birth_date, entered_birth_date, is_merged,"
                        + " lastupdated) VALUES (?, ?, 'F', ?, ?, false, NOW())",
                patientId, personId, Timestamp.valueOf(birthDate.atStartOfDay()),
                DateUtil.formatDateAsText(java.sql.Date.valueOf(birthDate)));
        String notStarted = statusService.getStatusID(AnalysisStatus.NotStarted);
        sampleId = next("sample_seq");
        jdbc.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date, is_confirmation,"
                + " domain) VALUES (?, ?, NOW(), NOW(), false, 'H')", sampleId, "CODEDCALC" + sampleId);
        jdbc.update("INSERT INTO clinlims.sample_human (id, samp_id, patient_id) VALUES (?, ?, ?)",
                next("sample_human_seq"), sampleId, patientId);
        itemId = next("sample_item_seq");
        jdbc.update("INSERT INTO clinlims.sample_item (id, sort_order, status_id, samp_id, typeosamp_id)"
                + " VALUES (?, 1, ?::numeric, ?, ?::numeric)", itemId, notStarted, sampleId, SPECIMEN);
        analysisId = next("analysis_seq");
        jdbc.update(
                "INSERT INTO clinlims.analysis (id, sampitem_id, test_id, analysis_type, status_id, revision,"
                        + " lastupdated) VALUES (?, ?, ?, 'MANUAL', ?::numeric, 0, NOW())",
                analysisId, itemId, PCR_TEST, notStarted);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void theCallsGiveTheResult() {
        threeRules();

        record(notDetected, detected);
        assertEquals("target not detected, control detected", notDetected, outcomeValue().longValue());

        record(detected, detected);
        assertEquals("target detected", detected, outcomeValue().longValue());

        record(notDetected, notDetected);
        assertEquals("neither detected", invalid, outcomeValue().longValue());

        record(detected, notDetected);
        assertEquals("a detected target needs no control", detected, outcomeValue().longValue());
    }

    @Test
    public void acceptedAnalyzerCallsGiveTheResult() {
        threeRules();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            SampleGrouping grouping = new SampleGrouping();
            grouping.accepted = true;
            grouping.sample = sampleService.get(String.valueOf(sampleId));
            grouping.sampleItem = sampleItemService.get(String.valueOf(itemId));
            grouping.patient = patientService.get(String.valueOf(patientId));
            Analysis analysis = analysisService.get(String.valueOf(analysisId));
            grouping.analysisList = List.of(analysis, analysis);
            grouping.resultList = List.of(analyzerCall(call, notDetected), analyzerCall(control, detected));
            grouping.noteList = List.of(List.of(), List.of());
            grouping.triggersToSelectedReflexesMap = new HashMap<>();
            analyzerResultsService.persistAnalyzerResults(List.of(), List.of(grouping), userId);
        });

        assertEquals(notDetected, outcomeValue().longValue());
    }

    private Result analyzerCall(String component, long answer) {
        Result result = new Result();
        result.setTestResult(testResultService.get(
                jdbc.queryForObject("SELECT id::text FROM clinlims.test_result WHERE component_id = ? AND value = ?",
                        String.class, component, String.valueOf(answer))));
        result.setResultType("D");
        result.setValue(String.valueOf(answer));
        result.setIsReportable("Y");
        result.setSysUserId(userId);
        return result;
    }

    @Test
    public void theRulesAgreeWhicheverRunsFirst() {
        rule("invalid", invalid, notDetected, notDetected);
        rule("not detected", notDetected, notDetected, detected);
        rule("detected", detected, detected, null);

        record(detected, detected);
        assertEquals(detected, outcomeValue().longValue());
        record(notDetected, detected);
        assertEquals(notDetected, outcomeValue().longValue());
        record(notDetected, notDetected);
        assertEquals(invalid, outcomeValue().longValue());
    }

    @Test
    public void theResultLandsOnTheResultComponentAndTheOrderedAnalysis() {
        threeRules();

        record(notDetected, detected);

        assertEquals(outcome, jdbc.queryForObject("SELECT tr.component_id FROM clinlims.result r JOIN"
                + " clinlims.test_result tr ON tr.id = r.test_result_id WHERE r.analysis_id = ? AND tr.component_id"
                + " NOT IN (?, ?)", String.class, analysisId, call, control));
        assertEquals("no second analysis", Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*)::int FROM clinlims.analysis WHERE sampitem_id = ?", Integer.class, itemId));
    }

    @Test
    public void aMissingCallLeavesTheResultEmpty() {
        threeRules();

        record(notDetected, null);

        assertNull(outcomeValue());
    }

    @Test
    public void rulesWithDifferentAnswersShareTheResultButNotTheSameAnswer() {
        threeRules();

        Calculation another = ruleFor("another invalid", invalid, notDetected, notDetected);
        assertThrows(CalculationRuleViolation.class, () -> calculationService.validateRule(another, true));
        calculationService.validateRule(ruleFor("unknown", answer("CodedCalc Unknown"), detected, null), true);
    }

    private void threeRules() {
        rule("detected", detected, detected, null);
        rule("not detected", notDetected, notDetected, detected);
        rule("invalid", invalid, notDetected, notDetected);
    }

    private void rule(String name, long answer, long callIs, Long controlIs) {
        Calculation calculation = ruleFor(name, answer, callIs, controlIs);
        calculationService.validateRule(calculation, true);
        calculationService.save(calculation);
    }

    private Calculation ruleFor(String name, long answer, long callIs, Long controlIs) {
        Calculation calculation = new Calculation();
        calculation.setName("CodedCalc " + name);
        calculation.setTestId((int) PCR_TEST);
        calculation.setSampleId((int) SPECIMEN);
        calculation.setComponentId(outcome);
        calculation.setResult(String.valueOf(answer));
        calculation.setActive(true);
        calculation.setToggled(true);
        List<Operation> operations = new ArrayList<>();
        compare(operations, call, callIs);
        if (controlIs != null) {
            operations.add(operation(Operation.OperationType.MATH_FUNCTION, "&&", null, operations.size()));
            compare(operations, control, controlIs);
        }
        calculation.setOperations(operations);
        return calculation;
    }

    private void compare(List<Operation> operations, String component, long answer) {
        operations.add(
                operation(Operation.OperationType.TEST_RESULT, String.valueOf(PCR_TEST), component, operations.size()));
        operations.add(operation(Operation.OperationType.MATH_FUNCTION, "==", null, operations.size()));
        operations.add(operation(Operation.OperationType.ANSWER, String.valueOf(answer), null, operations.size()));
    }

    private Operation operation(Operation.OperationType type, String value, String component, int order) {
        Operation operation = new Operation();
        operation.setType(type);
        operation.setValue(value);
        operation.setComponentId(component);
        operation.setOrder(order);
        return operation;
    }

    private void record(long callAnswer, Long controlAnswer) {
        callResult = upsert(callResult, call, callAnswer);
        if (controlAnswer != null) {
            controlResult = upsert(controlResult, control, controlAnswer);
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            List<ResultSet> saved = new ArrayList<>();
            for (Long id : new Long[] { callResult, controlResult }) {
                if (id != null) {
                    saved.add(new ResultSet(resultService.get(String.valueOf(id)), null, null,
                            patientService.get(String.valueOf(patientId)), sampleService.get(String.valueOf(sampleId)),
                            new HashMap<>(), false));
                }
            }
            testCalculatedUtil.addNewTestsToDBForCalculatedTests(saved, userId);
        });
    }

    private Long upsert(Long resultId, String component, long answer) {
        if (resultId != null) {
            jdbc.update("UPDATE clinlims.result SET value = ? WHERE id = ?", String.valueOf(answer), resultId);
            return resultId;
        }
        long id = next("result_seq");
        jdbc.update("INSERT INTO clinlims.result (id, analysis_id, test_result_id, result_type, value, lastupdated)"
                + " VALUES (?, ?, (SELECT id FROM clinlims.test_result WHERE component_id = ? AND value = ?), 'D', ?,"
                + " NOW())", id, analysisId, component, String.valueOf(answer), String.valueOf(answer));
        return id;
    }

    private Long outcomeValue() {
        List<String> values = jdbc.queryForList("SELECT r.value FROM clinlims.result r JOIN clinlims.test_result tr"
                + " ON tr.id = r.test_result_id WHERE r.analysis_id = ? AND tr.component_id = ? AND r.value <> ''",
                String.class, analysisId, outcome);
        assertEquals("one RESULT value at most: " + values, true, values.size() <= 1);
        return values.isEmpty() ? null : Long.valueOf(values.get(0));
    }

    private long answer(String entry) {
        long id = next("dictionary_seq");
        jdbc.update("INSERT INTO clinlims.dictionary (id, dict_entry, is_active, lastupdated) VALUES (?, ?, 'Y',"
                + " NOW())", id, entry);
        return id;
    }

    private void component(String id, String code, boolean primary, int order) {
        jdbc.update(
                "INSERT INTO clinlims.test_result_component (id, test_id, code, label, display_order, result_type,"
                        + " allow_multiple_readings, is_active, is_primary, show_on_report, lastupdated)"
                        + " VALUES (?, ?, ?, ?, ?, 'D', false, 'Y', ?, true, NOW())",
                id, PCR_TEST, code, code, order, primary);
    }

    private void testResult(String component, long answer) {
        jdbc.update("INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, value, sort_order, is_active,"
                + " component_id, lastupdated) VALUES (nextval('clinlims.test_result_seq'), ?, 'D', ?, 1, true, ?,"
                + " NOW())", PCR_TEST, String.valueOf(answer), component);
    }

    private long next(String sequence) {
        return jdbc.queryForObject("SELECT nextval('clinlims." + sequence + "')", Long.class);
    }

    // hbm2ddl=update adds a unique key on test_operations.test_id that the
    // Liquibase schema does not have.
    private void dropSchemaUpdateUniqueKeys() {
        for (String constraint : jdbc.queryForList("SELECT conname FROM pg_constraint WHERE conrelid ="
                + " 'clinlims.test_operations'::regclass AND contype = 'u'", String.class)) {
            jdbc.execute("ALTER TABLE clinlims.test_operations DROP CONSTRAINT \"" + constraint + "\"");
        }
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

    private void cleanup() {
        String analyses = "SELECT a.id FROM clinlims.analysis a WHERE a.test_id = " + PCR_TEST;
        jdbc.update("DELETE FROM clinlims.note WHERE reference_id IN (" + analyses + ")");
        String ours = "SELECT rc.id FROM clinlims.result_calculation rc JOIN clinlims.calculation c"
                + " ON c.id = rc.calculation_id WHERE c.name LIKE 'CodedCalc%'";
        jdbc.update("DELETE FROM clinlims.test_operations WHERE result_calculation_id IN (" + ours + ")");
        jdbc.update("DELETE FROM clinlims.test_result_map WHERE result_calculation_id IN (" + ours + ")");
        jdbc.update("DELETE FROM clinlims.result_calculation WHERE calculation_id IN (SELECT id FROM"
                + " clinlims.calculation WHERE name LIKE 'CodedCalc%')");
        jdbc.update("DELETE FROM clinlims.calculation_operation WHERE calculation_id IN (SELECT id FROM"
                + " clinlims.calculation WHERE name LIKE 'CodedCalc%')");
        jdbc.update("DELETE FROM clinlims.calculation WHERE name LIKE 'CodedCalc%'");
        jdbc.update("DELETE FROM clinlims.result WHERE analysis_id IN (" + analyses + ")");
        jdbc.update("DELETE FROM clinlims.analysis WHERE test_id = ?", PCR_TEST);
        jdbc.update("DELETE FROM clinlims.sample_item WHERE samp_id IN (SELECT id FROM clinlims.sample WHERE"
                + " accession_number LIKE 'CODEDCALC%')");
        jdbc.update("DELETE FROM clinlims.sample_human WHERE samp_id IN (SELECT id FROM clinlims.sample WHERE"
                + " accession_number LIKE 'CODEDCALC%')");
        jdbc.update("DELETE FROM clinlims.sample WHERE accession_number LIKE 'CODEDCALC%'");
        jdbc.update("DELETE FROM clinlims.patient WHERE person_id IN (SELECT id FROM clinlims.person WHERE last_name"
                + " = 'CODEDCALC')");
        jdbc.update("DELETE FROM clinlims.person WHERE last_name = 'CODEDCALC'");
        jdbc.update("DELETE FROM clinlims.test_result WHERE test_id = ?", PCR_TEST);
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", PCR_TEST);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", PCR_TEST);
        jdbc.update("DELETE FROM clinlims.dictionary WHERE dict_entry LIKE 'CodedCalc%'");
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", SPECIMEN);
        jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id IN (SELECT id FROM"
                + " clinlims.localization WHERE description = 'CodedCalc')");
        jdbc.update("DELETE FROM clinlims.localization WHERE description = 'CodedCalc'");
    }
}
