package org.openelisglobal.testcalculated.action.util;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.testcalculated.service.TestCalculationService;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Where a calculated value lands. A calculation writing another component of
 * the test it reads writes into that analysis, and a calculation whose
 * resulting test was ordered fills the ordered analysis. Both used to get a
 * second analysis of the test, which left the ordered one empty forever and
 * printed the test twice on the report. The value is stored with the
 * destination component's range for the patient on that specimen, not the first
 * range the test happens to have.
 */
public class CalculatedResultTargetIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long PCR_TEST = 95471L;
    private static final long GLUCOSE_TEST = 95472L;
    private static final long SPECIMEN = 95473L;

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
    private PlatformTransactionManager transactionManager;
    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private String specimen;
    private String componentA;
    private String componentB;
    private long itemId;
    private long pcrAnalysis;
    private long glucoseAnalysis;
    private long operandResult;
    private long patientId;
    private long sampleId;
    private String userId;
    private long testResultA;
    private String notStarted;
    private final List<Integer> calculations = new ArrayList<>();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        dropSchemaUpdateUniqueKeys();
        cleanup();
        userId = jdbc.queryForObject("SELECT min(id)::text FROM clinlims.system_user", String.class);
        long specimenName = next("localization_seq");
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, 'CalcTarget', NOW())",
                specimenName);
        jdbc.update("INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active,"
                + " sort_order, name_localization_id, lastupdated) VALUES (?, 'CalcTarget specimen', 'H', 'CTS',"
                + " true, 9473, ?, NOW())", SPECIMEN, specimenName);
        specimen = String.valueOf(SPECIMEN);
        ensureAnalysisStatuses();
        String notStarted = statusService.getStatusID(AnalysisStatus.NotStarted);
        for (long test : new long[] { PCR_TEST, GLUCOSE_TEST }) {
            long name = next("localization_seq");
            jdbc.update(
                    "INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, 'CalcTarget', NOW())",
                    name);
            jdbc.update(
                    "INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                            + " VALUES (nextval('clinlims.localization_value_seq'), ?, 'en', ?, NOW())",
                    name, "CalcTarget" + test);
            jdbc.update("INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated,"
                    + " name_localization_id, reporting_name_localization_id) VALUES (?, ?, ?, 'Y', ?, NOW(), ?, ?)",
                    test, "CalcTarget" + test, "calc target " + test, UUID.randomUUID().toString(), name, name);
        }
        componentA = UUID.randomUUID().toString();
        componentB = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, result_type, is_primary,"
                + " display_order) VALUES (?, ?, 'A', 'A (Ct)', 'N', true, 0), (?, ?, 'B', 'B (Ct)', 'N', false, 1)",
                componentA, PCR_TEST, componentB, PCR_TEST);
        long testResultA = insertTestResult(PCR_TEST, componentA);
        insertTestResult(PCR_TEST, componentB);
        insertTestResult(GLUCOSE_TEST, null);
        insertLimit(PCR_TEST, componentA, null, 20, 30);
        insertLimit(PCR_TEST, componentB, null, 12, 18);
        insertLimit(PCR_TEST, componentB, specimen, 41, 44);

        long personId = next("person_seq");
        patientId = next("patient_seq");
        jdbc.update(
                "INSERT INTO clinlims.person (id, last_name, first_name, lastupdated) VALUES (?, 'CALCTARGET', 'Test', NOW())",
                personId);
        LocalDate birthDate = LocalDate.now().minusYears(40);
        jdbc.update(
                "INSERT INTO clinlims.patient (id, person_id, gender, birth_date, entered_birth_date, is_merged,"
                        + " lastupdated) VALUES (?, ?, 'M', ?, ?, false, NOW())",
                patientId, personId, Timestamp.valueOf(birthDate.atStartOfDay()),
                DateUtil.formatDateAsText(java.sql.Date.valueOf(birthDate)));
        this.testResultA = testResultA;
        this.notStarted = notStarted;
        newOrder("12");
    }

    private void newOrder(String operandValue) {
        sampleId = next("sample_seq");
        jdbc.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date, is_confirmation,"
                + " domain) VALUES (?, ?, NOW(), NOW(), false, 'H')", sampleId, "CALCTGT" + sampleId);
        jdbc.update("INSERT INTO clinlims.sample_human (id, samp_id, patient_id) VALUES (?, ?, ?)",
                next("sample_human_seq"), sampleId, patientId);
        itemId = next("sample_item_seq");
        jdbc.update("INSERT INTO clinlims.sample_item (id, sort_order, status_id, samp_id, typeosamp_id)"
                + " VALUES (?, 1, ?::numeric, ?, ?::numeric)", itemId, notStarted, sampleId, specimen);
        pcrAnalysis = insertAnalysis(PCR_TEST, notStarted);
        glucoseAnalysis = insertAnalysis(GLUCOSE_TEST, notStarted);
        operandResult = next("result_seq");
        jdbc.update("INSERT INTO clinlims.result (id, analysis_id, test_result_id, result_type, value, lastupdated)"
                + " VALUES (?, ?, ?, 'N', ?, NOW())", operandResult, pcrAnalysis, testResultA, operandValue);
    }

    private String componentBValue(long analysisId) {
        return jdbc.queryForObject(
                "SELECT r.value FROM clinlims.result r JOIN clinlims.test_result tr"
                        + " ON tr.id = r.test_result_id WHERE r.analysis_id = ? AND tr.component_id = ?",
                String.class, analysisId, componentB);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    /**
     * The test persistence unit runs hbm2ddl=update, which adds a unique key on
     * test_operations.test_id that the Liquibase schema production runs on does not
     * have; with it only one patient order could ever hold a calculation for a
     * test.
     */
    private void dropSchemaUpdateUniqueKeys() {
        for (String constraint : jdbc.queryForList("SELECT conname FROM pg_constraint WHERE conrelid ="
                + " 'clinlims.test_operations'::regclass AND contype = 'u'", String.class)) {
            jdbc.execute("ALTER TABLE clinlims.test_operations DROP CONSTRAINT \"" + constraint + "\"");
        }
    }

    /**
     * Fixture datasets truncate status_of_sample and reseed only their own rows, so
     * under full-suite ordering the StatusService cache can lack the analysis
     * statuses the calculation path reads. Restore the missing canonical rows and
     * refresh the cache so this class is order-independent.
     */
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

    private long insertTestResult(long test, String componentId) {
        long id = next("test_result_seq");
        jdbc.update("INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, component_id, is_active)"
                + " VALUES (?, ?, 'N', ?, true)", id, test, componentId);
        return id;
    }

    private void insertLimit(long test, String componentId, String sampleTypeId, double low, double high) {
        jdbc.update("INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age, gender,"
                + " low_normal, high_normal, low_valid, high_valid, low_critical, high_critical, component_id,"
                + " sample_type_id, lastupdated) VALUES (nextval('clinlims.result_limits_seq'), ?, (SELECT id FROM"
                + " clinlims.type_of_test_result WHERE test_result_type = 'N'), 0, 'Infinity', 'M', ?, ?, 0, 100,"
                + " 'Infinity', 'Infinity', ?, ?::numeric, NOW())", test, low, high, componentId, sampleTypeId);
    }

    private long insertAnalysis(long test, String status) {
        long id = next("analysis_seq");
        jdbc.update("INSERT INTO clinlims.analysis (id, sampitem_id, test_id, analysis_type, status_id, revision,"
                + " lastupdated) VALUES (?, ?, ?, 'MANUAL', ?::numeric, 0, NOW())", id, itemId, test, status);
        return id;
    }

    private void calculation(long resultingTest, String destinationComponent, String function, String operand) {
        Calculation calculation = new Calculation();
        calculation.setName("CalcTarget " + resultingTest + " " + System.nanoTime());
        calculation.setSampleId(Integer.valueOf(specimen));
        calculation.setTestId((int) resultingTest);
        calculation.setComponentId(destinationComponent);
        calculation.setActive(true);
        calculation.setToggled(true);
        List<Operation> operations = new ArrayList<>();
        Operation reads = new Operation();
        reads.setOrder(0);
        reads.setType(Operation.OperationType.TEST_RESULT);
        reads.setValue(String.valueOf(PCR_TEST));
        reads.setSampleId(Integer.valueOf(specimen));
        reads.setComponentId(componentA);
        operations.add(reads);
        Operation math = new Operation();
        math.setOrder(1);
        math.setType(Operation.OperationType.MATH_FUNCTION);
        math.setValue(function);
        operations.add(math);
        Operation constant = new Operation();
        constant.setOrder(2);
        constant.setType(Operation.OperationType.INTEGER);
        constant.setValue(operand);
        operations.add(constant);
        calculation.setOperations(operations);
        calculations.add(calculationService.save(calculation).getId());
    }

    private void runCalculations() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Map<String, List<String>> reflexes = new HashMap<>();
            ResultSet resultSet = new ResultSet(resultService.get(String.valueOf(operandResult)), null, null,
                    patientService.get(String.valueOf(patientId)), sampleService.get(String.valueOf(sampleId)),
                    reflexes, false);
            testCalculatedUtil.addNewTestsToDBForCalculatedTests(List.of(resultSet), userId);
        });
    }

    private void cleanup() {
        String analyses = "SELECT a.id FROM clinlims.analysis a WHERE a.test_id IN (" + PCR_TEST + ", " + GLUCOSE_TEST
                + ")";
        jdbc.update("DELETE FROM clinlims.note WHERE reference_id IN (" + analyses + ")");
        String ourCalculations = "SELECT rc.id FROM clinlims.result_calculation rc JOIN clinlims.calculation c"
                + " ON c.id = rc.calculation_id WHERE c.name LIKE 'CalcTarget%'";
        jdbc.update("DELETE FROM clinlims.test_operations WHERE result_calculation_id IN (" + ourCalculations + ")");
        jdbc.update("DELETE FROM clinlims.test_result_map WHERE result_calculation_id IN (" + ourCalculations + ")");
        jdbc.update("DELETE FROM clinlims.result_calculation WHERE calculation_id IN (SELECT id FROM"
                + " clinlims.calculation WHERE name LIKE 'CalcTarget%')");
        jdbc.update("DELETE FROM clinlims.calculation_operation WHERE calculation_id IN (SELECT id FROM"
                + " clinlims.calculation WHERE name LIKE 'CalcTarget%')");
        jdbc.update("DELETE FROM clinlims.calculation WHERE name LIKE 'CalcTarget%'");
        jdbc.update("DELETE FROM clinlims.result WHERE analysis_id IN (" + analyses + ")");
        jdbc.update("DELETE FROM clinlims.analysis WHERE test_id IN (?, ?)", PCR_TEST, GLUCOSE_TEST);
        jdbc.update("DELETE FROM clinlims.sample_item WHERE samp_id IN (SELECT id FROM clinlims.sample WHERE"
                + " accession_number LIKE 'CALCTGT%')");
        jdbc.update("DELETE FROM clinlims.sample_human WHERE samp_id IN (SELECT id FROM clinlims.sample WHERE"
                + " accession_number LIKE 'CALCTGT%')");
        jdbc.update("DELETE FROM clinlims.sample WHERE accession_number LIKE 'CALCTGT%'");
        jdbc.update("DELETE FROM clinlims.patient WHERE person_id IN (SELECT id FROM clinlims.person WHERE last_name"
                + " = 'CALCTARGET')");
        jdbc.update("DELETE FROM clinlims.person WHERE last_name = 'CALCTARGET'");
        jdbc.update("DELETE FROM clinlims.result_limits WHERE test_id IN (?, ?)", PCR_TEST, GLUCOSE_TEST);
        jdbc.update("DELETE FROM clinlims.test_result WHERE test_id IN (?, ?)", PCR_TEST, GLUCOSE_TEST);
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id IN (?, ?)", PCR_TEST, GLUCOSE_TEST);
        jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?)", PCR_TEST, GLUCOSE_TEST);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", SPECIMEN);
        jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id IN (SELECT id FROM"
                + " clinlims.localization WHERE description = 'CalcTarget')");
        jdbc.update("DELETE FROM clinlims.localization WHERE description = 'CalcTarget'");
    }

    @Test
    public void aComponentCalculationFillsTheAnalysisItReads() {
        calculation(PCR_TEST, componentB, "+", "30");

        runCalculations();

        assertEquals("no second analysis of the test", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*)::int FROM clinlims.analysis WHERE sampitem_id = ? AND test_id = ?",
                        Integer.class, itemId, PCR_TEST));
        Map<String, Object> written = jdbc.queryForMap("SELECT r.analysis_id, r.value, r.min_normal, r.max_normal"
                + " FROM clinlims.result r JOIN clinlims.test_result tr ON tr.id = r.test_result_id"
                + " WHERE tr.component_id = ?", componentB);
        assertEquals(pcrAnalysis, ((Number) written.get("analysis_id")).longValue());
        assertEquals(42.0, Double.parseDouble((String) written.get("value")), 0.0);
        assertEquals("the component's range on this specimen", 41.0, (Double) written.get("min_normal"), 0.0);
        assertEquals(44.0, (Double) written.get("max_normal"), 0.0);
    }

    @Test
    public void aSecondOrderDoesNotRewriteTheFirstOrdersCalculation() {
        calculation(PCR_TEST, componentB, "+", "30");
        runCalculations();
        long firstOrderAnalysis = pcrAnalysis;
        assertEquals(42.0, Double.parseDouble(componentBValue(firstOrderAnalysis)), 0.0);

        newOrder("20");
        runCalculations();

        assertEquals("the first order keeps its value", 42.0, Double.parseDouble(componentBValue(firstOrderAnalysis)),
                0.0);
        assertEquals("the second order gets its own", 50.0, Double.parseDouble(componentBValue(pcrAnalysis)), 0.0);
    }

    @Test
    public void aCalculationFillsTheAnalysisTheOrderAlreadyCarries() {
        calculation(GLUCOSE_TEST, null, "*", "2");

        runCalculations();

        assertEquals("no second analysis of the ordered test", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*)::int FROM clinlims.analysis WHERE sampitem_id = ? AND test_id = ?",
                        Integer.class, itemId, GLUCOSE_TEST));
        assertEquals(24.0,
                Double.parseDouble(jdbc.queryForObject("SELECT value FROM clinlims.result WHERE analysis_id = ?",
                        String.class, glucoseAnalysis)),
                0.0);
    }
}
