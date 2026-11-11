package org.openelisglobal.result.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
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
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.configuration.service.ImportRunContext;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.result.action.util.ResultEntryAlert;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.testalertrule.service.TestAlertEvaluationService;
import org.openelisglobal.testresult.service.TestResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class CriticalAnswerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String PREFIX = "ITCA ";
    private static final String SECTION = PREFIX + "Molecular";
    private static final String TEST = PREFIX + "Ebola PCR";
    private static final String SPECIMEN = PREFIX + "Plasma";
    private static final String CATEGORY = PREFIX + "answers";
    private static final String ANSWERS_HEADER = "testName,resultType,resultValue,dictionaryCategory,componentCode,"
            + "isCritical\n";

    @Autowired
    @Qualifier("testSectionConfigurationHandler")
    private DomainConfigurationHandler sectionHandler;
    @Autowired
    @Qualifier("typeOfSampleConfigurationHandler")
    private DomainConfigurationHandler sampleTypeHandler;
    @Autowired
    @Qualifier("testConfigurationHandler")
    private DomainConfigurationHandler testHandler;
    @Autowired
    @Qualifier("dictionaryConfigurationHandler")
    private DomainConfigurationHandler dictionaryHandler;
    @Autowired
    @Qualifier("resultComponentConfigurationHandler")
    private DomainConfigurationHandler componentHandler;
    @Autowired
    @Qualifier("testResultConfigurationHandler")
    private DomainConfigurationHandler testResultHandler;
    @Autowired
    @Qualifier("calculatedValueConfigurationHandler")
    private DomainConfigurationHandler calculatedValueHandler;

    @Autowired
    private ResultEntryAcknowledgementService acknowledgementService;
    @Autowired
    private TestAlertEvaluationService testAlertEvaluationService;
    @Autowired
    private AnalyzerResultsService analyzerResultsService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private String userId;
    private String testId;
    private String detected;
    private String notDetected;
    private long patientId;
    private long sampleId;
    private long itemId;
    private long analysisId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        dropSchemaUpdateUniqueKeys();
        cleanup();
        userId = jdbc.queryForObject("SELECT min(id)::text FROM clinlims.system_user", String.class);
        load(sectionHandler, "testSectionName,isActive,sortOrder,isExternal,localization:en\n" + SECTION + ",Y,90,N,"
                + SECTION + "\n", "sections.csv");
        load(sampleTypeHandler,
                "description,localAbbreviation,domain,isActive,sortOrder\n" + SPECIMEN + ",ITCAPL,H,Y,91\n",
                "sample-types.csv");
        load(testHandler, "testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,unitOfMeasure\n" + TEST
                + "," + SECTION + "," + SPECIMEN + ",86518-8,Y,Y,90,\n", "tests.csv");
        load(dictionaryHandler, "category,dictEntry,localAbbreviation,isActive,sortOrder\n" + CATEGORY
                + ",Detected,,Y,1\n" + CATEGORY + ",Not detected,,Y,2\n" + CATEGORY + ",\"Invalid, retest\",,Y,3\n",
                "dictionaries.csv");
        load(componentHandler, "testName,code,label,resultType,isPrimary,isRequired\n" + TEST + ",RESULT,Result,D,Y,Y\n"
                + TEST + ",EBOV,Ebola,D,N,N\n" + TEST + ",IC,Internal control,D,N,N\n", "result-components.csv");
        StringBuilder answers = new StringBuilder(ANSWERS_HEADER);
        for (String component : new String[] { "RESULT", "EBOV", "IC" }) {
            answers.append(TEST + ",D,Detected," + CATEGORY + "," + component + ","
                    + ("IC".equals(component) ? "N" : "Y") + "\n");
            answers.append(TEST + ",D,Not detected," + CATEGORY + "," + component + ",\n");
        }
        answers.append(TEST + ",D,\"Invalid, retest\"," + CATEGORY + ",RESULT,\n");
        load(testResultHandler, answers.toString(), "test-results.csv");
        load(calculatedValueHandler,
                "name,testName,sampleType,componentCode,result,expression,note,isActive\n" + PREFIX + "detected," + TEST
                        + ",,RESULT,Detected,[EBOV] == 'Detected',,Y\n" + PREFIX + "not detected," + TEST
                        + ",,RESULT,Not detected,[EBOV] == 'Not detected' && [IC] == 'Detected',,Y\n",
                "calculated-values.csv");

        testId = jdbc.queryForObject("SELECT id::text FROM clinlims.test WHERE description = ?", String.class, TEST);
        detected = answerId("Detected");
        notDetected = answerId("Not detected");
        order();
    }

    @After
    public void tearDown() {
        ImportRunContext.clearPending();
        cleanup();
    }

    @Test
    public void theCatalogMarksAnAnswerCriticalOnItsOwnComponent() throws Exception {
        assertEquals(List.of("EBOV Detected", "RESULT Detected"), criticalAnswers());

        load(testResultHandler, ANSWERS_HEADER + TEST + ",D,Detected," + CATEGORY + ",EBOV,\n", "test-results.csv");
        assertEquals("a blank cell keeps the flag", List.of("EBOV Detected", "RESULT Detected"), criticalAnswers());

        load(testResultHandler, ANSWERS_HEADER + TEST + ",D,Detected," + CATEGORY + ",EBOV,N\n", "test-results.csv");
        assertEquals(List.of("RESULT Detected"), criticalAnswers());
    }

    @Test
    public void aCriticalAnswerIsOwedAnAcknowledgmentOnlyWhereTheCatalogMarksIt() {
        List<ResultEntryAlert> owed = acknowledgementService.alertsForItems(List.of(entry("EBOV", detected, null)));
        assertEquals(1, owed.size());
        assertEquals(ResultEntryAlert.KIND_CRITICAL, owed.get(0).getKind());
        assertEquals("Detected", owed.get(0).getValue());

        assertEquals("the control's Detected is not critical", List.of(),
                acknowledgementService.alertsForItems(List.of(entry("IC", detected, null))));
        assertEquals(List.of(), acknowledgementService.alertsForItems(List.of(entry("EBOV", notDetected, null))));

        TestResultItem acknowledged = entry("EBOV", detected, null);
        acknowledged.setCriticalAcknowledged(true);
        assertTrue(acknowledgementService.alertsForItems(List.of(acknowledged)).get(0).isAcknowledged());
    }

    @Test
    public void theAnswerAlreadyStoredIsNotOwedAgain() {
        String stored = storedResult("EBOV", detected);

        assertEquals(List.of(), acknowledgementService.alertsForItems(List.of(entry("EBOV", detected, stored))));
    }

    @Test
    public void aCriticalAnswerCalculatedFromAcceptedAnalyzerCallsRaisesTheCriticalResultAlert() {
        jdbc.update("UPDATE clinlims.test_result SET is_critical = false WHERE component_id = ?", component("EBOV"));

        List<Result> recorded = new TransactionTemplate(transactionManager).execute(status -> {
            SampleGrouping grouping = new SampleGrouping();
            grouping.accepted = true;
            grouping.sample = sampleService.get(String.valueOf(sampleId));
            grouping.sampleItem = sampleItemService.get(String.valueOf(itemId));
            grouping.patient = patientService.get(String.valueOf(patientId));
            Analysis analysis = analysisService.get(String.valueOf(analysisId));
            grouping.analysisList = List.of(analysis, analysis);
            grouping.resultList = List.of(analyzerCall("EBOV", detected), analyzerCall("IC", detected));
            grouping.noteList = List.of(List.of(), List.of());
            grouping.triggersToSelectedReflexesMap = new HashMap<>();
            return analyzerResultsService.persistAnalyzerResults(List.of(), List.of(grouping), userId);
        });
        assertEquals("both calls and the calculated result", 3, recorded.size());

        for (Result result : recorded) {
            testAlertEvaluationService.evaluateAndDispatch(result, userId);
        }

        List<java.util.Map<String, Object>> alerts = jdbc.queryForList("SELECT context_data->>'value' AS value,"
                + " duplicate_count FROM clinlims.alert WHERE alert_type = 'CRITICAL_RESULT' AND alert_entity_type ="
                + " 'ANALYSIS' AND alert_entity_id = ?", analysisId);
        assertEquals(1, alerts.size());
        assertEquals("Detected", alerts.get(0).get("value"));
        assertEquals("only the calculated result is critical", 0,
                ((Number) alerts.get(0).get("duplicate_count")).intValue());
    }

    private TestResultItem entry(String code, String answer, String resultId) {
        TestResultItem item = new TestResultItem();
        item.setAnalysisId(String.valueOf(analysisId));
        item.setResultId(resultId);
        item.setTestResultComponentId(component(code));
        item.setResultType("D");
        item.setResultValue(answer);
        return item;
    }

    private Result analyzerCall(String code, String answer) {
        Result result = new Result();
        result.setTestResult(testResultService.getTestResultsByTestAndDictonaryResult(testId, answer, component(code)));
        result.setResultType("D");
        result.setValue(answer);
        result.setIsReportable("Y");
        result.setSysUserId(userId);
        return result;
    }

    private String storedResult(String code, String answer) {
        long id = next("result_seq");
        jdbc.update("INSERT INTO clinlims.result (id, analysis_id, test_result_id, result_type, value, lastupdated)"
                + " VALUES (?, ?, (SELECT id FROM clinlims.test_result WHERE component_id = ? AND value = ?), 'D', ?,"
                + " NOW())", id, analysisId, component(code), answer, answer);
        return String.valueOf(id);
    }

    private List<String> criticalAnswers() {
        return jdbc.queryForList(
                "SELECT rc.code || ' ' || d.dict_entry FROM clinlims.test_result tr"
                        + " JOIN clinlims.test_result_component rc ON rc.id = tr.component_id"
                        + " JOIN clinlims.dictionary d ON d.id::text = tr.value"
                        + " WHERE tr.test_id = ?::numeric AND tr.is_active AND tr.is_critical ORDER BY 1",
                String.class, testId);
    }

    private String component(String code) {
        return jdbc.queryForObject(
                "SELECT id FROM clinlims.test_result_component WHERE test_id = ?::numeric AND code = ?", String.class,
                testId, code);
    }

    private String answerId(String entry) {
        return jdbc.queryForObject(
                "SELECT d.id::text FROM clinlims.dictionary d JOIN clinlims.dictionary_category c"
                        + " ON c.id = d.dictionary_category_id WHERE c.name = ? AND d.dict_entry = ?",
                String.class, CATEGORY, entry);
    }

    private void order() {
        ensureAnalysisStatuses();
        long personId = next("person_seq");
        patientId = next("patient_seq");
        jdbc.update("INSERT INTO clinlims.person (id, last_name, first_name, lastupdated)"
                + " VALUES (?, 'ITCRITICAL', 'Test', NOW())", personId);
        LocalDate birthDate = LocalDate.now().minusYears(30);
        jdbc.update(
                "INSERT INTO clinlims.patient (id, person_id, gender, birth_date, entered_birth_date, is_merged,"
                        + " lastupdated) VALUES (?, ?, 'F', ?, ?, false, NOW())",
                patientId, personId, Timestamp.valueOf(birthDate.atStartOfDay()),
                DateUtil.formatDateAsText(java.sql.Date.valueOf(birthDate)));
        String notStarted = statusService.getStatusID(AnalysisStatus.NotStarted);
        sampleId = next("sample_seq");
        jdbc.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date, is_confirmation,"
                + " domain) VALUES (?, ?, NOW(), NOW(), false, 'H')", sampleId, "ITCRITICAL" + sampleId);
        jdbc.update("INSERT INTO clinlims.sample_human (id, samp_id, patient_id) VALUES (?, ?, ?)",
                next("sample_human_seq"), sampleId, patientId);
        itemId = next("sample_item_seq");
        jdbc.update(
                "INSERT INTO clinlims.sample_item (id, sort_order, status_id, samp_id, typeosamp_id) VALUES (?, 1,"
                        + " ?::numeric, ?, (SELECT id FROM clinlims.type_of_sample WHERE description = ?))",
                itemId, notStarted, sampleId, SPECIMEN);
        analysisId = next("analysis_seq");
        jdbc.update(
                "INSERT INTO clinlims.analysis (id, sampitem_id, test_id, analysis_type, status_id, revision,"
                        + " lastupdated) VALUES (?, ?, ?::numeric, 'MANUAL', ?::numeric, 0, NOW())",
                analysisId, itemId, testId, notStarted);
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

    // hbm2ddl=update adds a unique key on test_operations.test_id that the
    // Liquibase schema does not have.
    private void dropSchemaUpdateUniqueKeys() {
        for (String constraint : jdbc.queryForList("SELECT conname FROM pg_constraint WHERE conrelid ="
                + " 'clinlims.test_operations'::regclass AND contype = 'u'", String.class)) {
            jdbc.execute("ALTER TABLE clinlims.test_operations DROP CONSTRAINT \"" + constraint + "\"");
        }
    }

    private long next(String sequence) {
        return jdbc.queryForObject("SELECT nextval('clinlims." + sequence + "')", Long.class);
    }

    private static void load(DomainConfigurationHandler handler, String csv, String fileName) throws Exception {
        handler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), fileName);
    }

    private void cleanup() {
        String analyses = "SELECT a.id FROM clinlims.analysis a JOIN clinlims.test t ON t.id = a.test_id"
                + " WHERE t.description LIKE 'ITCA %'";
        jdbc.update("DELETE FROM clinlims.alert WHERE alert_entity_type = 'ANALYSIS' AND alert_entity_id IN ("
                + analyses + ")");
        jdbc.update("DELETE FROM clinlims.note WHERE reference_id IN (" + analyses + ")");
        String ours = "SELECT rc.id FROM clinlims.result_calculation rc JOIN clinlims.calculation c"
                + " ON c.id = rc.calculation_id WHERE c.name LIKE 'ITCA %'";
        jdbc.update("DELETE FROM clinlims.test_operations WHERE result_calculation_id IN (" + ours + ")");
        jdbc.update("DELETE FROM clinlims.test_result_map WHERE result_calculation_id IN (" + ours + ")");
        jdbc.update("DELETE FROM clinlims.result_calculation WHERE calculation_id IN (SELECT id FROM"
                + " clinlims.calculation WHERE name LIKE 'ITCA %')");
        jdbc.update("DELETE FROM clinlims.unresolved_reference WHERE domain = 'calculated-values'");
        jdbc.update("DELETE FROM clinlims.calculation_operation WHERE calculation_id IN"
                + " (SELECT id FROM clinlims.calculation WHERE name LIKE 'ITCA %') OR calculation_id IS NULL");
        jdbc.update("DELETE FROM clinlims.calculation WHERE name LIKE 'ITCA %'");
        jdbc.update("DELETE FROM clinlims.result WHERE analysis_id IN (" + analyses + ")");
        jdbc.update("DELETE FROM clinlims.analysis WHERE id IN (" + analyses + ")");
        String samples = "SELECT id FROM clinlims.sample WHERE accession_number LIKE 'ITCRITICAL%'";
        jdbc.update("DELETE FROM clinlims.sample_item WHERE samp_id IN (" + samples + ")");
        jdbc.update("DELETE FROM clinlims.sample_human WHERE samp_id IN (" + samples + ")");
        jdbc.update("DELETE FROM clinlims.sample WHERE id IN (" + samples + ")");
        jdbc.update("DELETE FROM clinlims.patient WHERE person_id IN (SELECT id FROM clinlims.person WHERE last_name"
                + " = 'ITCRITICAL')");
        jdbc.update("DELETE FROM clinlims.person WHERE last_name = 'ITCRITICAL'");
        for (Long id : jdbc.queryForList("SELECT id FROM clinlims.test WHERE description LIKE 'ITCA %'", Long.class)) {
            jdbc.update("DELETE FROM clinlims.test_terminology_mapping WHERE test_id = ?", id);
            jdbc.update("DELETE FROM clinlims.test_result WHERE test_id = ?", id);
            jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", id);
            jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = ?", id);
            jdbc.update("DELETE FROM clinlims.test WHERE id = ?", id);
        }
        jdbc.update("DELETE FROM clinlims.dictionary WHERE dictionary_category_id IN"
                + " (SELECT id FROM clinlims.dictionary_category WHERE name = ?)", CATEGORY);
        jdbc.update("DELETE FROM clinlims.dictionary_category WHERE name = ?", CATEGORY);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE description LIKE 'ITCA %'");
        jdbc.update("DELETE FROM clinlims.test_section WHERE name = ?", SECTION);
    }
}
