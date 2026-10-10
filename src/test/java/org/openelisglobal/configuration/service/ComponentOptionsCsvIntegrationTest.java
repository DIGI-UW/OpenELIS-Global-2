package org.openelisglobal.configuration.service;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A test-results row that names a component puts its answer on that component;
 * a row without one keeps putting it on the test's primary result.
 */
public class ComponentOptionsCsvIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String PREFIX = "ITCO ";
    private static final String SECTION = PREFIX + "Molecular";
    private static final String TEST = PREFIX + "Viral load";
    private static final String CATEGORY = PREFIX + "answers";
    private static final String RESULTS_HEADER = "testName,resultType,resultValue,dictionaryCategory,sortOrder,"
            + "isQuantifiable,isActive,isNormal,significantDigits,flags,componentCode\n";

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
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() throws Exception {
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        load(sectionHandler, "testSectionName,isActive,sortOrder,isExternal,localization:en\n" + SECTION + ",Y,90,N,"
                + SECTION + "\n", "sections.csv");
        load(sampleTypeHandler,
                "description,localAbbreviation,domain,isActive,sortOrder\n" + PREFIX + "Plasma,ITCOPL,H,Y,91\n",
                "sample-types.csv");
        load(testHandler, "testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,unitOfMeasure\n" + TEST
                + "," + SECTION + "," + PREFIX + "Plasma,20447-9,Y,Y,90,\n", "tests.csv");
        load(dictionaryHandler,
                "category,dictEntry,localAbbreviation,isActive,sortOrder\n" + CATEGORY + ",Detected,,Y,1\n" + CATEGORY
                        + ",Not detected,,Y,2\n" + CATEGORY + ",PASS,,Y,3\n" + CATEGORY + ",FAIL,,Y,4\n",
                "dictionaries.csv");
        load(componentHandler,
                "testName,code,label,resultType,significantDigits,isPrimary,showOnReport\n" + TEST
                        + ",PRIMARY,Viral load,N,0,Y,Y\n" + TEST + ",call,Call,D,,N,Y\n" + TEST
                        + ",IQS-H,IQS-H,D,,N,N\n" + TEST + ",IQS-L,IQS-L,D,,N,N\n",
                "result-components.csv");
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void eachAnswerLandsOnTheComponentItsRowNames() throws Exception {
        load(testResultHandler, answers(), "test-results.csv");

        assertEquals(Map.of("PRIMARY", List.of("N:"), "call", List.of("D:Detected", "D:Not detected"), "IQS-H",
                List.of("D:FAIL", "D:PASS"), "IQS-L", List.of("D:PASS")), optionsByComponent());
        assertEquals("the primary result stays numeric", "N", primaryResultType());
    }

    @Test
    public void reimportingUpdatesEachComponentsAnswersInPlace() throws Exception {
        load(testResultHandler, answers(), "test-results.csv");
        Map<String, List<String>> first = optionsByComponent();

        load(testResultHandler, answers(), "test-results.csv");

        assertEquals(first, optionsByComponent());
    }

    @Test
    public void aRowNamingAComponentTheTestLacksAddsNothing() throws Exception {
        load(testResultHandler, RESULTS_HEADER + TEST + ",D,PASS," + CATEGORY + ",1,N,Y,N,,,IQS-X\n",
                "test-results.csv");

        assertEquals(Map.of("PRIMARY", List.of("N:")), optionsByComponent());
    }

    private static String answers() {
        return RESULTS_HEADER + TEST + ",N,,,1,Y,Y,N,0,,\n" + TEST + ",D,Detected," + CATEGORY + ",1,N,Y,N,,,call\n"
                + TEST + ",D,Not detected," + CATEGORY + ",2,N,Y,Y,,,call\n" + TEST + ",D,PASS," + CATEGORY
                + ",1,N,Y,Y,,,IQS-H\n" + TEST + ",D,FAIL," + CATEGORY + ",2,N,Y,N,,,IQS-H\n" + TEST + ",D,PASS,"
                + CATEGORY + ",1,N,Y,Y,,,IQS-L\n";
    }

    /** Each component's active answers as type:entry, sorted. */
    private Map<String, List<String>> optionsByComponent() {
        Map<String, List<String>> options = new TreeMap<>();
        jdbc.query("SELECT c.code, tr.tst_rslt_type, d.dict_entry FROM clinlims.test_result tr"
                + " JOIN clinlims.test t ON t.id = tr.test_id"
                + " LEFT JOIN clinlims.test_result_component c ON c.id = tr.component_id"
                + " LEFT JOIN clinlims.dictionary d ON tr.tst_rslt_type = 'D' AND d.id = CAST(tr.value AS numeric)"
                + " WHERE t.description = ? AND tr.is_active = true ORDER BY c.code, d.dict_entry", rs -> {
                    options.computeIfAbsent(String.valueOf(rs.getString(1)), k -> new java.util.ArrayList<>())
                            .add(rs.getString(2) + ":" + (rs.getString(3) == null ? "" : rs.getString(3)));
                }, TEST);
        return options;
    }

    private String primaryResultType() {
        return jdbc.queryForObject(
                "SELECT c.result_type FROM clinlims.test_result_component c"
                        + " JOIN clinlims.test t ON t.id = c.test_id WHERE t.description = ? AND c.is_primary",
                String.class, TEST);
    }

    private static void load(DomainConfigurationHandler handler, String csv, String fileName) throws Exception {
        handler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), fileName);
    }

    private void cleanup() {
        for (Long testId : jdbc.queryForList("SELECT id FROM clinlims.test WHERE description LIKE 'ITCO %'",
                Long.class)) {
            jdbc.update("DELETE FROM clinlims.test_terminology_mapping WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.test_result WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.test WHERE id = ?", testId);
        }
        jdbc.update("DELETE FROM clinlims.dictionary WHERE dictionary_category_id IN"
                + " (SELECT id FROM clinlims.dictionary_category WHERE name = ?)", CATEGORY);
        jdbc.update("DELETE FROM clinlims.dictionary_category WHERE name = ?", CATEGORY);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE description LIKE 'ITCO %'");
        jdbc.update("DELETE FROM clinlims.test_section WHERE name = ?", SECTION);
    }
}
