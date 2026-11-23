package org.openelisglobal.configuration.service;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

public class CalculatedValueCsvIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String PREFIX = "ITCV ";
    private static final String SECTION = PREFIX + "Molecular";
    private static final String TEST = PREFIX + "Ebola PCR";
    private static final String CATEGORY = PREFIX + "answers";
    private static final String HEADER = "name,testName,sampleType,componentCode,result,expression,note,isActive\n";
    private static final String RULES = HEADER + PREFIX + "detected," + TEST
            + ",,RESULT,Detected,[EBOV] == 'Detected',,Y\n" + PREFIX + "not detected," + TEST
            + ",,RESULT,Not detected,[EBOV] == 'Not detected' && [IC] == 'Detected',,Y\n" + PREFIX + "invalid," + TEST
            + ",,RESULT,\"Invalid, retest\",[EBOV] == 'Not detected' && [IC] == 'Not detected',,Y\n";

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
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() throws Exception {
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        load(sectionHandler, "testSectionName,isActive,sortOrder,isExternal,localization:en\n" + SECTION + ",Y,90,N,"
                + SECTION + "\n", "sections.csv");
        load(sampleTypeHandler,
                "description,localAbbreviation,domain,isActive,sortOrder\n" + PREFIX + "Plasma,ITCVPL,H,Y,91\n",
                "sample-types.csv");
        load(testHandler, "testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,unitOfMeasure\n" + TEST
                + "," + SECTION + "," + PREFIX + "Plasma,86518-8,Y,Y,90,\n", "tests.csv");
        load(dictionaryHandler, "category,dictEntry,localAbbreviation,isActive,sortOrder\n" + CATEGORY
                + ",Detected,,Y,1\n" + CATEGORY + ",Not detected,,Y,2\n" + CATEGORY + ",\"Invalid, retest\",,Y,3\n",
                "dictionaries.csv");
        load(componentHandler, "testName,code,label,resultType,isPrimary,isRequired\n" + TEST + ",RESULT,Result,D,Y,Y\n"
                + TEST + ",EBOV,Ebola,D,N,N\n" + TEST + ",IC,Internal control,D,N,N\n", "result-components.csv");
        StringBuilder answers = new StringBuilder("testName,resultType,resultValue,dictionaryCategory,sortOrder,"
                + "isQuantifiable,isActive,isNormal,componentCode\n");
        for (String component : new String[] { "RESULT", "EBOV", "IC" }) {
            answers.append(TEST + ",D,Detected," + CATEGORY + ",1,N,Y,N," + component + "\n");
            answers.append(TEST + ",D,Not detected," + CATEGORY + ",2,N,Y,Y," + component + "\n");
        }
        answers.append(TEST + ",D,\"Invalid, retest\"," + CATEGORY + ",3,N,Y,N,RESULT\n");
        load(testResultHandler, answers.toString(), "test-results.csv");
    }

    @After
    public void tearDown() {
        ImportRunContext.clearPending();
        cleanup();
    }

    @Test
    public void eachRowBecomesARuleWritingItsAnswerToTheNamedComponent() throws Exception {
        load(calculatedValueHandler, RULES, "calculated-values.csv");

        assertEquals(
                List.of("ITCV detected: Detected", "ITCV invalid: Invalid, retest", "ITCV not detected: Not detected"),
                rules());
        assertEquals("each operand reads the test's specimen, as the page needs to show it", List.of(PREFIX + "Plasma"),
                jdbc.queryForList("SELECT DISTINCT s.description FROM clinlims.calculation_operation o"
                        + " JOIN clinlims.type_of_sample s ON s.id = o.sample_id WHERE o.type = 'TEST_RESULT'"
                        + " AND o.calculation_id IN (SELECT id FROM clinlims.calculation WHERE name LIKE 'ITCV %')",
                        String.class));
        assertEquals(List.of("TEST_RESULT:EBOV", "MATH_FUNCTION:==", "ANSWER:Not detected", "MATH_FUNCTION:&&",
                "TEST_RESULT:IC", "MATH_FUNCTION:==", "ANSWER:Detected"), operations(PREFIX + "not detected"));
    }

    @Test
    public void reloadingTheSameRulesKeepsThem() throws Exception {
        load(calculatedValueHandler, RULES, "calculated-values.csv");
        List<Long> ids = ruleIds();
        List<Long> operationIds = operationIds();

        load(calculatedValueHandler, RULES, "calculated-values.csv");

        assertEquals(ids, ruleIds());
        assertEquals("orders in progress stay bound to the operations", operationIds, operationIds());
    }

    @Test
    public void reloadingRewritesTheRulesInPlace() throws Exception {
        load(calculatedValueHandler, RULES, "calculated-values.csv");
        List<Long> ids = ruleIds();

        load(calculatedValueHandler,
                RULES.replace("[EBOV] == 'Detected'", "[EBOV] == 'Detected' || [IC] != 'Detected'"),
                "calculated-values.csv");

        assertEquals(ids, ruleIds());
        assertEquals(7, operations(PREFIX + "detected").size());
    }

    @Test
    public void aRuleTheEngineCouldNotRunIsNotLoaded() throws Exception {
        load(calculatedValueHandler,
                HEADER + PREFIX + "unknown answer," + TEST + ",,RESULT,Detected,[EBOV] == 'Maybe',,Y\n" + PREFIX
                        + "ordered answer," + TEST + ",,RESULT,Detected,[EBOV] >= 'Detected',,Y\n" + PREFIX
                        + "unknown component," + TEST + ",,RESULT,Detected,[MARV] == 'Detected',,Y\n" + PREFIX
                        + "garbled," + TEST + ",,RESULT,Detected,[EBOV] = 'Detected',,Y\n" + PREFIX
                        + "arithmetic on an answer," + TEST + ",,RESULT,Detected,[EBOV] == 'Detected' + 1,,Y\n",
                "calculated-values.csv");

        assertEquals(List.of(), rules());
    }

    private List<String> rules() {
        return jdbc.queryForList("SELECT c.name || ': ' || d.dict_entry FROM clinlims.calculation c"
                + " JOIN clinlims.dictionary d ON d.id::text = c.result"
                + " JOIN clinlims.test_result_component rc ON rc.id = c.component_id AND rc.code = 'RESULT'"
                + " WHERE c.name LIKE 'ITCV %' ORDER BY c.name", String.class);
    }

    private List<Long> ruleIds() {
        return jdbc.queryForList("SELECT id FROM clinlims.calculation WHERE name LIKE 'ITCV %' ORDER BY name",
                Long.class);
    }

    private List<Long> operationIds() {
        return jdbc.queryForList("SELECT o.id FROM clinlims.calculation_operation o JOIN clinlims.calculation c"
                + " ON c.id = o.calculation_id WHERE c.name LIKE 'ITCV %' ORDER BY o.id", Long.class);
    }

    private List<String> operations(String rule) {
        return jdbc.queryForList("SELECT o.type || ':' || CASE o.type WHEN 'TEST_RESULT' THEN rc.code"
                + " WHEN 'ANSWER' THEN d.dict_entry ELSE o.value END FROM clinlims.calculation_operation o"
                + " JOIN clinlims.calculation c ON c.id = o.calculation_id"
                + " LEFT JOIN clinlims.test_result_component rc ON rc.id = o.component_id"
                + " LEFT JOIN clinlims.dictionary d ON o.type = 'ANSWER' AND d.id::text = o.value"
                + " WHERE c.name = ? ORDER BY o.operation_order", String.class, rule);
    }

    private static void load(DomainConfigurationHandler handler, String csv, String fileName) throws Exception {
        handler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), fileName);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.unresolved_reference WHERE domain = 'calculated-values'");
        jdbc.update("DELETE FROM clinlims.calculation_operation WHERE calculation_id IN"
                + " (SELECT id FROM clinlims.calculation WHERE name LIKE 'ITCV %') OR calculation_id IS NULL");
        jdbc.update("DELETE FROM clinlims.calculation WHERE name LIKE 'ITCV %'");
        for (Long testId : jdbc.queryForList("SELECT id FROM clinlims.test WHERE description LIKE 'ITCV %'",
                Long.class)) {
            jdbc.update("DELETE FROM clinlims.test_terminology_mapping WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.result_limits WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.test_result WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.test WHERE id = ?", testId);
        }
        jdbc.update("DELETE FROM clinlims.dictionary WHERE dictionary_category_id IN"
                + " (SELECT id FROM clinlims.dictionary_category WHERE name = ?)", CATEGORY);
        jdbc.update("DELETE FROM clinlims.dictionary_category WHERE name = ?", CATEGORY);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE description LIKE 'ITCV %'");
        jdbc.update("DELETE FROM clinlims.test_section WHERE name = ?", SECTION);
    }
}
