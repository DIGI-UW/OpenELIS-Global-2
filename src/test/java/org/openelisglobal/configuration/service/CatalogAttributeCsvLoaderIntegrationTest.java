package org.openelisglobal.configuration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.configuration.valueholder.UnresolvedReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-1194: the catalog attributes a CSV could not carry before (result
 * components, reference ranges, terminology codes, sample handling and reflex
 * rules) load through the same services the Test Catalog editor writes with; a
 * preview computes the same outcomes and keeps nothing; and a name the catalog
 * does not know waits in the decision queue until it is resolved, after which
 * the remembered spelling resolves by itself.
 */
public class CatalogAttributeCsvLoaderIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String PREFIX = "IT1194 ";
    private static final String SECTION = PREFIX + "Chem";
    private static final String GLUCOSE = PREFIX + "Glucose";
    private static final String HBA1C = PREFIX + "HbA1c";

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
    @Qualifier("resultComponentConfigurationHandler")
    private DomainConfigurationHandler componentHandler;

    @Autowired
    @Qualifier("resultLimitConfigurationHandler")
    private DomainConfigurationHandler rangeHandler;

    @Autowired
    @Qualifier("terminologyConfigurationHandler")
    private DomainConfigurationHandler terminologyHandler;

    @Autowired
    @Qualifier("sampleHandlingConfigurationHandler")
    private DomainConfigurationHandler handlingHandler;

    @Autowired
    @Qualifier("reflexRuleConfigurationHandler")
    private DomainConfigurationHandler reflexHandler;

    @Autowired
    private UnresolvedReferenceService unresolvedReferenceService;

    @Autowired
    private ReferenceAliasService referenceAliasService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() throws Exception {
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        load(sectionHandler, "testSectionName,isActive,sortOrder,isExternal,localization:en\n" + SECTION + ",Y,94,N,"
                + SECTION + "\n", "sections.csv");
        load(sampleTypeHandler, "description,localAbbreviation,domain,isActive,sortOrder\n" + PREFIX
                + "Serum,I94SER,H,Y,94\n" + PREFIX + "Plasma,I94PLA,H,Y,95\n", "sample-types.csv");
        load(testHandler,
                "testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,unitOfMeasure,localCode\n"
                        + GLUCOSE + "," + SECTION + "," + PREFIX + "Serum|" + PREFIX + "Plasma,2345-7,Y,Y,940,,I94GLU\n"
                        + HBA1C + "," + SECTION + "," + PREFIX + "Serum,4548-4,Y,Y,941,,I94HBA\n",
                "tests.csv");
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void resultComponents_areCreatedThenMergedByCode() throws Exception {
        load(componentHandler,
                "testName,code,label,resultType,significantDigits,displayOrder,isPrimary,showOnReport\n" + GLUCOSE
                        + ",PRIMARY," + PREFIX + "Glucose,N,2,0,Y,Y\n" + GLUCOSE
                        + ",FASTING,Fasting glucose,N,1,1,N,Y\n",
                "components.csv");

        assertEquals("SUMMARY file=components.csv domain=result-components created=1 updated=1 skipped=0",
                componentHandler.getLastSummary().toLine());
        long testId = testId(GLUCOSE);
        assertEquals(Integer.valueOf(2),
                count("test_result_component", "test_id = " + testId + " AND is_active = 'Y'"));
        Map<String, Object> primary = component(testId, "PRIMARY");
        assertEquals("N", primary.get("result_type"));
        assertEquals(2, ((Number) primary.get("significant_digits")).intValue());
        assertEquals(Boolean.TRUE, primary.get("is_primary"));
        assertEquals(1, ((Number) component(testId, "FASTING").get("significant_digits")).intValue());

        // A second load of the same codes keeps two components and takes the new
        // label; a blank cell leaves what is stored alone.
        load(componentHandler, "testName,code,label,significantDigits\n" + GLUCOSE + ",FASTING,Fasting,\n",
                "components.csv");
        assertEquals("SUMMARY file=components.csv domain=result-components created=0 updated=1 skipped=0",
                componentHandler.getLastSummary().toLine());
        assertEquals(Integer.valueOf(2),
                count("test_result_component", "test_id = " + testId + " AND is_active = 'Y'"));
        assertEquals("Fasting", component(testId, "FASTING").get("label"));
        assertEquals(1, ((Number) component(testId, "FASTING").get("significant_digits")).intValue());
    }

    @Test
    public void ranges_terminologyAndSampleHandling_loadThroughTheEditorServices() throws Exception {
        load(componentHandler,
                "testName,code,label,resultType,significantDigits,isPrimary\n" + GLUCOSE + ",PRIMARY,Glucose,N,2,Y\n",
                "components.csv");
        long testId = testId(GLUCOSE);

        load(rangeHandler,
                "testName,sampleType,componentCode,gender,minAge,maxAge,lowNormal,highNormal,lowCritical,highCritical\n"
                        + GLUCOSE + "," + PREFIX + "Serum,PRIMARY,M,0,120,3.9,5.5,2.2,25\n" + GLUCOSE + "," + PREFIX
                        + "Serum,PRIMARY,F,0,120,3.5,5.5,2.2,25\n",
                "ranges.csv");
        assertEquals("SUMMARY file=ranges.csv domain=result-limits created=2 updated=0 skipped=0",
                rangeHandler.getLastSummary().toLine());
        assertEquals(Integer.valueOf(2), count("result_limits", "test_id = " + testId));
        Map<String, Object> maleRange = jdbc
                .queryForMap("SELECT low_normal, high_normal, sample_type_id, component_id FROM clinlims.result_limits"
                        + " WHERE test_id = ? AND gender = 'M'", testId);
        assertEquals(3.9d, ((Number) maleRange.get("low_normal")).doubleValue(), 0.0001d);
        assertNotNull("the range is scoped to the specimen the row names", maleRange.get("sample_type_id"));
        assertNotNull("and to the component it names", maleRange.get("component_id"));

        // The file is the test's complete set of numeric ranges: dropping a row
        // drops its range.
        load(rangeHandler, "testName,sampleType,componentCode,gender,minAge,maxAge,lowNormal,highNormal\n" + GLUCOSE
                + "," + PREFIX + "Serum,PRIMARY,M,0,120,4,5.4\n", "ranges.csv");
        assertEquals(Integer.valueOf(1), count("result_limits", "test_id = " + testId));

        // The tests.csv row already bridged LOINC 2345-7 as a test-level mapping;
        // naming it again updates that one, and the SNOMED row is new.
        load(terminologyHandler,
                "testName,componentCode,source,code,relationship,displayName\n" + GLUCOSE
                        + ",,LOINC,2345-7,SAME_AS,Glucose [Mass/volume]\n" + GLUCOSE + ",PRIMARY,SNOMED,33747003,,\n",
                "terminology.csv");
        assertEquals("SUMMARY file=terminology.csv domain=terminology created=1 updated=1 skipped=0",
                terminologyHandler.getLastSummary().toLine());
        assertEquals("the LOINC the tests.csv row already bridged is updated, the SNOMED code added",
                Integer.valueOf(2), count("test_terminology_mapping", "test_id = " + testId + " AND is_active = 'Y'"));
        assertEquals("Glucose [Mass/volume]", jdbc.queryForObject(
                "SELECT display_name FROM clinlims.test_terminology_mapping WHERE test_id = ? AND source = 'LOINC'"
                        + " AND is_active = 'Y'",
                String.class, testId));
        assertEquals("SAME_AS", jdbc.queryForObject(
                "SELECT relationship FROM clinlims.test_terminology_mapping WHERE test_id = ? AND source = 'SNOMED'",
                String.class, testId));

        load(handlingHandler,
                "testName,storageCondition,storageDuration,storageDurationUnit,protectFromLight,disposalMethod\n"
                        + GLUCOSE + ",REFRIGERATED,48,HOURS,Y,INCINERATION\n",
                "handling.csv");
        assertEquals("SUMMARY file=handling.csv domain=sample-handling created=1 updated=0 skipped=0",
                handlingHandler.getLastSummary().toLine());
        Map<String, Object> handling = jdbc.queryForMap(
                "SELECT storage_condition, storage_duration, protect_from_light FROM clinlims.test_sample_handling"
                        + " WHERE test_id = ?",
                testId);
        assertEquals("REFRIGERATED", handling.get("storage_condition"));
        assertEquals(48, ((Number) handling.get("storage_duration")).intValue());
        assertEquals(Boolean.TRUE, handling.get("protect_from_light"));

        load(handlingHandler, "testName,storageDuration\n" + GLUCOSE + ",72\n", "handling.csv");
        assertEquals("SUMMARY file=handling.csv domain=sample-handling created=0 updated=1 skipped=0",
                handlingHandler.getLastSummary().toLine());
        assertEquals("a blank cell leaves the stored value alone", "REFRIGERATED", jdbc.queryForObject(
                "SELECT storage_condition FROM clinlims.test_sample_handling WHERE test_id = ?", String.class, testId));
    }

    @Test
    public void reflexRules_areBuiltFromTheirRowsAndIdentifiedByName() throws Exception {
        load(componentHandler, "testName,code,label,resultType,isPrimary\n" + GLUCOSE + ",PRIMARY,Glucose,N,Y\n",
                "components.csv");

        String rules = "ruleName,overall,active,conditionTest,conditionSampleType,conditionComponent,relation,value,"
                + "reflexTest,reflexSampleType,internalNote\n" + PREFIX + "High glucose,ANY,Y," + GLUCOSE + "," + PREFIX
                + "Serum,PRIMARY,GREATER_THAN,11," + HBA1C + "," + PREFIX + "Serum,Confirm with HbA1c\n";
        load(reflexHandler, rules, "reflex.csv");

        assertEquals(reasons(reflexHandler),
                "SUMMARY file=reflex.csv domain=reflex-rules created=1 updated=0 skipped=0",
                reflexHandler.getLastSummary().toLine());
        Integer ruleId = jdbc.queryForObject("SELECT id FROM clinlims.reflex_rule WHERE rule_name = ?", Integer.class,
                PREFIX + "High glucose");
        assertNotNull(ruleId);
        assertEquals(Integer.valueOf(1), count("reflex_rule_condition", "reflex_rule_id = " + ruleId));
        assertEquals(Integer.valueOf(1), count("reflex_rule_action", "reflex_rule_id = " + ruleId));
        assertEquals("GREATER_THAN", jdbc.queryForObject(
                "SELECT relation FROM clinlims.reflex_rule_condition WHERE reflex_rule_id = ?", String.class, ruleId));

        // The same name is the same rule: the threshold moves, no second rule.
        load(reflexHandler, rules.replace(",GREATER_THAN,11,", ",GREATER_THAN,14,"), "reflex.csv");
        assertEquals("SUMMARY file=reflex.csv domain=reflex-rules created=0 updated=1 skipped=0",
                reflexHandler.getLastSummary().toLine());
        assertEquals(Integer.valueOf(1), count("reflex_rule", "rule_name = '" + PREFIX + "High glucose'"));
        assertEquals("14", jdbc.queryForObject(
                "SELECT value FROM clinlims.reflex_rule_condition WHERE reflex_rule_id = ?", String.class, ruleId));
    }

    @Test
    public void aPreviewComputesTheSameOutcomesAndKeepsNothing() throws Exception {
        String csv = "testName,code,label,resultType,significantDigits,isPrimary\n" + GLUCOSE
                + ",PRIMARY,Glucose,N,2,Y\n" + "No Such Test IT1194,PRIMARY,Nope,N,2,Y\n";

        componentHandler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                "components.csv", true);

        // createInactiveTest pre-seeds a PRIMARY component, so naming it updates
        // that row rather than adding one.
        assertEquals("SUMMARY file=components.csv domain=result-components created=0 updated=1 skipped=1",
                componentHandler.getLastSummary().toLine());
        List<CsvLoadSummary.RowOutcome> rows = componentHandler.getLastSummary().getRows();
        assertEquals(2, rows.size());
        assertEquals(CsvLoadSummary.Outcome.SKIPPED, rows.get(1).outcome());
        assertTrue(rows.get(1).reason().contains("No Such Test IT1194"));
        assertEquals("the preview kept nothing: the pre-seeded component still has no result type", Integer.valueOf(0),
                count("test_result_component",
                        "test_id = " + testId(GLUCOSE) + " AND code = 'PRIMARY' AND result_type = 'N'"));

        // The same file applied for real writes the component the preview promised.
        load(componentHandler, csv, "components.csv");
        assertEquals("SUMMARY file=components.csv domain=result-components created=0 updated=1 skipped=1",
                componentHandler.getLastSummary().toLine());
        assertEquals(Integer.valueOf(1), count("test_result_component",
                "test_id = " + testId(GLUCOSE) + " AND code = 'PRIMARY' AND result_type = 'N'"));
    }

    @Test
    public void anUnknownNameWaitsForADecisionAndARememberedAliasResolvesItNextTime() throws Exception {
        load(componentHandler, "testName,code,label,resultType,isPrimary\n" + GLUCOSE + ",PRIMARY,Glucose,N,Y\n",
                "components.csv");
        String ranges = "testName,sampleType,componentCode,minAge,maxAge,lowNormal,highNormal\n" + GLUCOSE + ","
                + PREFIX + "Plasme,PRIMARY,0,120,3.9,5.5\n";

        load(rangeHandler, ranges, "ranges.csv");

        assertEquals("SUMMARY file=ranges.csv domain=result-limits created=0 updated=0 skipped=1",
                rangeHandler.getLastSummary().toLine());
        UnresolvedReference waiting = open(PREFIX + "Plasme");
        assertNotNull("the misspelled specimen is queued for a decision", waiting);
        assertEquals(UnresolvedReference.TYPE_SAMPLE_TYPE, waiting.getReferenceType());
        assertEquals("result-limits", waiting.getDomain());
        assertEquals(Integer.valueOf(0), count("result_limits", "test_id = " + testId(GLUCOSE)));

        // The same misspelling in a later file folds into the open item.
        load(rangeHandler, ranges, "ranges.csv");
        assertEquals(2, open(PREFIX + "Plasme").getOccurrences());

        unresolvedReferenceService.resolve(waiting.getId(), UnresolvedReference.RESOLUTION_ALIAS,
                sampleTypeId(PREFIX + "Plasma"), "1");
        assertNull("resolving closes the item", open(PREFIX + "Plasme"));
        assertEquals(sampleTypeId(PREFIX + "Plasma"),
                referenceAliasService.resolve(UnresolvedReference.TYPE_SAMPLE_TYPE, PREFIX + "Plasme"));

        load(rangeHandler, ranges, "ranges.csv");

        assertEquals("the remembered spelling resolves by itself",
                "SUMMARY file=ranges.csv domain=result-limits created=1 updated=0 skipped=0",
                rangeHandler.getLastSummary().toLine());
        assertEquals(Integer.valueOf(1), count("result_limits",
                "test_id = " + testId(GLUCOSE) + " AND sample_type_id = " + sampleTypeId(PREFIX + "Plasma")));
    }

    /**
     * Every skip reason of the last load, so a failure says why a row was dropped.
     */
    private static String reasons(DomainConfigurationHandler handler) {
        StringBuilder text = new StringBuilder();
        for (CsvLoadSummary.RowOutcome row : handler.getLastSummary().getRows()) {
            if (row.reason() != null) {
                text.append("line ").append(row.lineNumber()).append(": ").append(row.reason()).append('\n');
            }
        }
        return text.toString();
    }

    @Test
    public void aPreviewJudgesEachRowAgainstTheCatalogAsItStands() throws Exception {
        String newTest = PREFIX + "Amylase";
        String tests = "testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder\n" + newTest + ","
                + SECTION + "," + PREFIX + "Serum,1798-8,Y,Y,942\n";
        String components = "testName,code,label,resultType,significantDigits,isPrimary\n" + newTest
                + ",PRIMARY,Amylase,N,1,Y\n";

        testHandler.processConfiguration(stream(tests), "tests.csv", true);
        componentHandler.processConfiguration(stream(components), "components.csv", true);

        assertEquals("SUMMARY file=tests.csv domain=tests created=1 updated=0 skipped=0",
                testHandler.getLastSummary().toLine());
        assertEquals("the preview kept nothing", Integer.valueOf(0), count("test", "description = '" + newTest + "'"));
        // Nothing was kept, so a file that needs the test the previous file would
        // create says so rather than pretending: applying the files in load order
        // is what resolves it.
        assertEquals("SUMMARY file=components.csv domain=result-components created=0 updated=0 skipped=1",
                componentHandler.getLastSummary().toLine());
        assertTrue(componentHandler.getLastSummary().getRows().get(0).reason().contains(newTest));

        load(testHandler, tests, "tests.csv");
        load(componentHandler, components, "components.csv");
        assertEquals("SUMMARY file=components.csv domain=result-components created=0 updated=1 skipped=0",
                componentHandler.getLastSummary().toLine());
    }

    private UnresolvedReference open(String value) {
        return unresolvedReferenceService.getOpen().stream()
                .filter(reference -> value.equals(reference.getReferenceValue())).findFirst().orElse(null);
    }

    private void load(DomainConfigurationHandler handler, String csv, String fileName) throws Exception {
        handler.processConfiguration(stream(csv), fileName);
    }

    private static ByteArrayInputStream stream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    private Integer count(String table, String where) {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims." + table + " WHERE " + where, Integer.class);
    }

    private long testId(String description) {
        return jdbc.queryForObject("SELECT id FROM clinlims.test WHERE description = ?", Long.class, description);
    }

    private String sampleTypeId(String description) {
        return jdbc.queryForObject("SELECT id::text FROM clinlims.type_of_sample WHERE description = ?", String.class,
                description);
    }

    private Map<String, Object> component(long testId, String code) {
        return jdbc.queryForMap("SELECT id, code, label, result_type, significant_digits, is_primary"
                + " FROM clinlims.test_result_component WHERE test_id = ? AND code = ?", testId, code);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.reference_alias WHERE target_id IN"
                + " (SELECT id::text FROM clinlims.type_of_sample WHERE description LIKE 'IT1194%')");
        jdbc.update("DELETE FROM clinlims.unresolved_reference WHERE reference_value LIKE 'IT1194%'");
        for (Integer ruleId : jdbc.queryForList("SELECT id FROM clinlims.reflex_rule WHERE rule_name LIKE 'IT1194%'",
                Integer.class)) {
            jdbc.update("DELETE FROM clinlims.reflex_rule_condition WHERE reflex_rule_id = ?", ruleId);
            jdbc.update("DELETE FROM clinlims.reflex_rule_action WHERE reflex_rule_id = ?", ruleId);
            jdbc.update("DELETE FROM clinlims.reflex_rule WHERE id = ?", ruleId);
        }
        List<Long> testLocalizations = jdbc.queryForList(
                "SELECT name_localization_id FROM clinlims.test"
                        + " WHERE description LIKE 'IT1194%' AND name_localization_id IS NOT NULL"
                        + " UNION SELECT reporting_name_localization_id FROM clinlims.test"
                        + " WHERE description LIKE 'IT1194%' AND reporting_name_localization_id IS NOT NULL",
                Long.class);
        // test_reflex points at both tests and their analytes, so it goes first.
        jdbc.update("DELETE FROM clinlims.test_reflex WHERE test_id IN (SELECT id FROM clinlims.test"
                + " WHERE description LIKE 'IT1194%') OR add_test_id IN (SELECT id FROM clinlims.test"
                + " WHERE description LIKE 'IT1194%') OR test_analyte_id IN (SELECT id FROM clinlims.test_analyte"
                + " WHERE test_id IN (SELECT id FROM clinlims.test WHERE description LIKE 'IT1194%'))");
        jdbc.update("DELETE FROM clinlims.test_analyte WHERE test_id IN (SELECT id FROM clinlims.test"
                + " WHERE description LIKE 'IT1194%')");
        for (Long testId : jdbc.queryForList("SELECT id FROM clinlims.test WHERE description LIKE 'IT1194%'",
                Long.class)) {
            jdbc.update("DELETE FROM clinlims.test_sample_handling WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.result_limits WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.test_terminology_mapping WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.test_result WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.test WHERE id = ?", testId);
        }
        deleteLocalizations(testLocalizations);

        List<Long> sampleTypeLocalizations = jdbc
                .queryForList("SELECT name_localization_id FROM clinlims.type_of_sample"
                        + " WHERE description LIKE 'IT1194%' AND name_localization_id IS NOT NULL", Long.class);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE description LIKE 'IT1194%'");
        deleteLocalizations(sampleTypeLocalizations);

        List<Long> sectionLocalizations = jdbc.queryForList("SELECT name_localization_id FROM clinlims.test_section"
                + " WHERE name = ? AND name_localization_id IS NOT NULL", Long.class, SECTION);
        jdbc.update("DELETE FROM clinlims.test_section WHERE name = ?", SECTION);
        deleteLocalizations(sectionLocalizations);
        jdbc.update("DELETE FROM clinlims.analyte WHERE name LIKE 'IT1194%'");
    }

    private void deleteLocalizations(List<Long> localizationIds) {
        for (Long localizationId : localizationIds) {
            jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id = ?", localizationId);
            jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", localizationId);
        }
    }
}
