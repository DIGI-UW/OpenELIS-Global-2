package org.openelisglobal.configuration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-1193 (Story A of the CSV catalog loader): a tests.csv row is one test
 * linked to every listed specimen and described by its plain name; identity is
 * the local code, then the plain name, never a translation; records an earlier
 * loader wrote as {@code Name(SampleType)} are recognised rather than
 * duplicated; a rejected row is skipped alone; panels resolve plain names; and
 * every file closes with a SUMMARY line.
 */
public class CatalogCsvLoaderIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String PREFIX = "IT1193 ";
    private static final String SECTION = PREFIX + "Chem";
    private static final String TESTS_HEADER = "testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,"
            + "unitOfMeasure,isReportable,notifyResults,localCode,localization:en,localization:fr\n";

    // Handlers may be @Transactional JDK proxies, so inject them by their
    // interface.
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
    @Qualifier("panelConfigurationHandler")
    private DomainConfigurationHandler panelHandler;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() throws Exception {
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        load(sectionHandler, "testSectionName,isActive,sortOrder,isExternal,localization:en\n" + SECTION + ",Y,90,N,"
                + SECTION + "\n", "sections.csv");
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void sampleTypes_aRejectedRowIsSkippedAloneAndTheRestOfTheFileLoads() throws Exception {
        String csv = "description,localAbbreviation,domain,isActive,sortOrder\n" + PREFIX + "Bad,TOOLONGABBREV,H,Y,90\n"
                + PREFIX + "Serum,I93SER,H,Y,91\n" + PREFIX + "Plasma,I93PLA,H,Y,92\n" + PREFIX
                + "Blood,I93BLD,H,Y,93\n";

        load(sampleTypeHandler, csv, "sample-types.csv");

        assertEquals("SUMMARY file=sample-types.csv domain=sample-types created=3 updated=0 skipped=1",
                sampleTypeHandler.getLastSummary().toLine());
        assertEquals(Integer.valueOf(3), count("type_of_sample", "description LIKE 'IT1193%'"));
        assertEquals("the over-long abbreviation must not be persisted", Integer.valueOf(0),
                count("type_of_sample", "description = '" + PREFIX + "Bad'"));
    }

    @Test
    public void tests_oneTestPerRowLinkedToEverySpecimen_identityByCodeThenPlainName() throws Exception {
        loadSampleTypes();
        String rowGlucose = PREFIX + "Glucose," + SECTION + "," + PREFIX + "Serum|" + PREFIX + "Plasma|" + PREFIX
                + "Blood,2345-7,Y,Y,901,,N,Y,I93GLU," + PREFIX + "Glucose,Glycemie IT1193\n";
        String rowAmylase = PREFIX + "Amylase," + SECTION + "," + PREFIX + "Serum|" + PREFIX
                + "Plasma,1798-8,Y,N,902,,,,I93AMY,,\n";
        String rowOrphan = PREFIX + "Orphan,No Such Section IT," + PREFIX + "Serum,,Y,Y,903,,,,,,\n";
        String rowSodium = PREFIX + "Sodium," + SECTION + "," + PREFIX + "Serum,2951-2,N,Y,904,,,,,,\n";
        String csv = TESTS_HEADER + rowGlucose + rowAmylase + rowOrphan + rowSodium;

        load(testHandler, csv, "tests.csv");

        assertEquals("SUMMARY file=tests.csv domain=tests created=3 updated=0 skipped=1",
                testHandler.getLastSummary().toLine());
        assertEquals(Integer.valueOf(3), count("test", "description LIKE 'IT1193%'"));
        assertEquals("no Name(SampleType) fan-out", Integer.valueOf(0), count("test", "description LIKE 'IT1193%(%'"));

        Map<String, Object> glucose = testRow(PREFIX + "Glucose");
        long glucoseId = ((Number) glucose.get("id")).longValue();
        assertEquals(Integer.valueOf(3), count("sampletype_test", "test_id = " + glucoseId));
        assertEquals("I93GLU", glucose.get("local_code"));
        assertEquals("N", glucose.get("is_reportable"));
        assertEquals(Boolean.TRUE, glucose.get("notify_results"));
        assertEquals("Y", glucose.get("is_active"));
        assertEquals(Boolean.TRUE, glucose.get("orderable"));
        assertEquals("2345-7", glucose.get("loinc"));
        assertEquals(sectionId(), ((Number) glucose.get("test_section_id")).longValue());
        assertEquals("Glycemie IT1193", translation(((Number) glucose.get("name_localization_id")).longValue(), "fr"));

        Map<String, Object> amylase = testRow(PREFIX + "Amylase");
        long amylaseId = ((Number) amylase.get("id")).longValue();
        assertEquals(Integer.valueOf(2), count("sampletype_test", "test_id = " + amylaseId));
        assertEquals("isReportable defaults to Y", "Y", amylase.get("is_reportable"));
        assertEquals(Boolean.FALSE, amylase.get("orderable"));
        assertNotEquals(Boolean.TRUE, amylase.get("notify_results"));

        assertEquals("N", testRow(PREFIX + "Sodium").get("is_active"));
        assertEquals(Integer.valueOf(0), count("test", "description = '" + PREFIX + "Orphan'"));

        // The same file again: every test is updated in place, nothing is duplicated.
        load(testHandler, csv, "tests.csv");
        assertEquals("SUMMARY file=tests.csv domain=tests created=0 updated=3 skipped=1",
                testHandler.getLastSummary().toLine());
        assertEquals(Integer.valueOf(3), count("test", "description LIKE 'IT1193%'"));
        assertEquals(Integer.valueOf(3), count("sampletype_test", "test_id = " + glucoseId));

        // Without the translation columns the tests are still found by name.
        String bareHeader = "testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,unitOfMeasure,"
                + "isReportable,notifyResults,localCode\n";
        String bare = bareHeader + PREFIX + "Glucose," + SECTION + "," + PREFIX + "Serum|" + PREFIX + "Plasma|" + PREFIX
                + "Blood,2345-7,Y,Y,901,,N,Y,I93GLU\n" + PREFIX + "Amylase," + SECTION + "," + PREFIX + "Serum|"
                + PREFIX + "Plasma,1798-8,Y,N,902,,,,I93AMY\n" + PREFIX + "Sodium," + SECTION + "," + PREFIX
                + "Serum,2951-2,N,Y,904,,,,\n";
        load(testHandler, bare, "tests.csv");
        assertEquals("SUMMARY file=tests.csv domain=tests created=0 updated=3 skipped=0",
                testHandler.getLastSummary().toLine());
        assertEquals(Integer.valueOf(3), count("test", "description LIKE 'IT1193%'"));

        // A renamed row keeps its identity through the local code.
        load(testHandler, TESTS_HEADER + PREFIX + "Glucose Fasting," + SECTION + "," + PREFIX
                + "Serum,2345-7,Y,Y,901,,N,Y,I93GLU,,\n", "tests.csv");
        assertEquals("SUMMARY file=tests.csv domain=tests created=0 updated=1 skipped=0",
                testHandler.getLastSummary().toLine());
        assertEquals(glucoseId, ((Number) testRow(PREFIX + "Glucose Fasting").get("id")).longValue());
        assertEquals(Integer.valueOf(3), count("test", "description LIKE 'IT1193%'"));
    }

    @Test
    public void legacyVariants_areUpdatedInPlaceAndJoinPanelsForTheirSpecimens() throws Exception {
        loadSampleTypes();
        // The shape an earlier loader left behind: one record per specimen, both
        // displayed as "IT1193 Legacy".
        long serumVariant = 95193L;
        long plasmaVariant = 95194L;
        insertLegacyVariant(serumVariant, PREFIX + "Legacy(" + PREFIX + "Serum)", 951931L);
        insertLegacyVariant(plasmaVariant, PREFIX + "Legacy(" + PREFIX + "Plasma)", 951932L);

        load(testHandler,
                TESTS_HEADER + PREFIX + "Legacy," + SECTION + "," + PREFIX + "Serum|" + PREFIX
                        + "Plasma,,Y,Y,905,,,,,,\n" + PREFIX + "Amylase," + SECTION + "," + PREFIX
                        + "Serum,1798-8,Y,Y,902,,,,,,\n",
                "tests.csv");

        assertEquals("SUMMARY file=tests.csv domain=tests created=1 updated=1 skipped=0",
                testHandler.getLastSummary().toLine());
        assertEquals("no plain-named duplicate of the legacy records", Integer.valueOf(0),
                count("test", "description = '" + PREFIX + "Legacy'"));
        assertEquals(Integer.valueOf(1), count("sampletype_test", "test_id = " + serumVariant));
        assertEquals(Integer.valueOf(1), count("sampletype_test", "test_id = " + plasmaVariant));
        assertEquals(sectionId(),
                ((Number) testRow(PREFIX + "Legacy(" + PREFIX + "Serum)").get("test_section_id")).longValue());

        load(panelHandler,
                "panelName,sampleTypes,tests,isActive,sortOrder\n" + PREFIX + "Panel," + PREFIX + "Serum|" + PREFIX
                        + "Plasma," + PREFIX + "Amylase|" + PREFIX + "Legacy,Y,90\n" + PREFIX + "Serum Panel," + PREFIX
                        + "Serum," + PREFIX + "Legacy,Y,91\n",
                "panels.csv");

        assertEquals("SUMMARY file=panels.csv domain=panels created=2 updated=0 skipped=0",
                panelHandler.getLastSummary().toLine());
        Long panelId = jdbc.queryForObject("SELECT id FROM clinlims.panel WHERE name = ?", Long.class,
                PREFIX + "Panel");
        assertEquals("the plain test plus the variants for both of the panel's specimens", Integer.valueOf(3),
                count("panel_item", "panel_id = " + panelId));
        Long serumPanelId = jdbc.queryForObject("SELECT id FROM clinlims.panel WHERE name = ?", Long.class,
                PREFIX + "Serum Panel");
        assertEquals("a Serum panel takes only the Serum variant", Integer.valueOf(1),
                count("panel_item", "panel_id = " + serumPanelId + " AND test_id = " + serumVariant));
        assertEquals(Integer.valueOf(1), count("panel_item", "panel_id = " + serumPanelId));
    }

    @Test
    public void legacyVariantsSpelledDifferently_areRecognisedRenamedAndKeepTheirResultDefinitions() throws Exception {
        loadSampleTypes();
        // The shape the shipped catalog has for a viral load: one record per
        // specimen, spelled without spaces, each with a numeric result at two
        // decimals.
        long serumVariant = 95196L;
        long plasmaVariant = 95197L;
        long bloodVariant = 95198L;
        insertLegacyVariant(serumVariant, "IT1193VL(" + PREFIX + "Serum)", 951934L);
        insertLegacyVariant(plasmaVariant, "IT1193VL(" + PREFIX + "Plasma)", 951935L);
        insertLegacyVariant(bloodVariant, "IT1193VL(" + PREFIX + "Blood)", 951936L);
        insertNumericResult(9519601L, serumVariant, 2);
        insertNumericResult(9519602L, plasmaVariant, 2);

        load(testHandler, TESTS_HEADER + PREFIX + "VL," + SECTION + "," + PREFIX + "Serum|" + PREFIX
                + "Plasma,20447-9,Y,Y,907,,,,,,\n", "tests.csv");

        assertEquals("SUMMARY file=tests.csv domain=tests created=0 updated=1 skipped=0",
                testHandler.getLastSummary().toLine());
        assertEquals("no plain-named duplicate", Integer.valueOf(0), count("test", "description = '" + PREFIX + "VL'"));
        assertEquals("the record takes the row's spelling and keeps its specimen suffix", Long.valueOf(serumVariant),
                Long.valueOf(((Number) testRow(PREFIX + "VL(" + PREFIX + "Serum)").get("id")).longValue()));
        assertEquals("20447-9", testRow(PREFIX + "VL(" + PREFIX + "Plasma)").get("loinc"));
        assertEquals(Integer.valueOf(1), count("sampletype_test", "test_id = " + serumVariant));
        assertEquals(Integer.valueOf(1), count("sampletype_test", "test_id = " + plasmaVariant));
        assertEquals("the numeric result definition survives the update", Integer.valueOf(1),
                count("test_result", "test_id = " + serumVariant + " AND significant_digits = 2 AND is_active"));
        Map<String, Object> unlisted = testRow("IT1193VL(" + PREFIX + "Blood)");
        assertEquals("a variant for a specimen the row does not list is a different test and stays untouched",
                Long.valueOf(bloodVariant), Long.valueOf(((Number) unlisted.get("id")).longValue()));
        assertEquals(null, unlisted.get("loinc"));

        load(panelHandler, "panelName,sampleTypes,tests,isActive,sortOrder\n" + PREFIX + "VL Panel," + PREFIX + "Serum,"
                + PREFIX + "VL,Y,91\n", "panels.csv");

        Long panelId = jdbc.queryForObject("SELECT id FROM clinlims.panel WHERE name = ?", Long.class,
                PREFIX + "VL Panel");
        assertEquals("a Serum panel naming the plain test takes the Serum variant", Integer.valueOf(1),
                count("panel_item", "panel_id = " + panelId));
    }

    @Test
    public void collapsedLegacyRecord_isLinkedToEveryListedSpecimen() throws Exception {
        loadSampleTypes();
        long collapsed = 95195L;
        insertLegacyVariant(collapsed, PREFIX + "Collapsed(" + PREFIX + "Serum)", 951933L);

        load(testHandler, TESTS_HEADER + PREFIX + "Collapsed," + SECTION + "," + PREFIX + "Serum|" + PREFIX + "Plasma|"
                + PREFIX + "Blood,,Y,Y,906,,,,I93COL,,\n", "tests.csv");

        assertEquals("SUMMARY file=tests.csv domain=tests created=0 updated=1 skipped=0",
                testHandler.getLastSummary().toLine());
        assertEquals(Integer.valueOf(0), count("test", "description = '" + PREFIX + "Collapsed'"));
        assertEquals(Integer.valueOf(3), count("sampletype_test", "test_id = " + collapsed));
        assertEquals("I93COL", testRow(PREFIX + "Collapsed(" + PREFIX + "Serum)").get("local_code"));
    }

    private void loadSampleTypes() throws Exception {
        load(sampleTypeHandler, "description,localAbbreviation,domain,isActive,sortOrder\n" + PREFIX
                + "Serum,I93SER,H,Y,91\n" + PREFIX + "Plasma,I93PLA,H,Y,92\n" + PREFIX + "Blood,I93BLD,H,Y,93\n",
                "sample-types.csv");
    }

    private void insertLegacyVariant(long testId, String description, long localizationId) {
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, 'test name', NOW())",
                localizationId);
        jdbc.update("INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                + " VALUES (?, ?, 'en', ?, NOW())", localizationId, localizationId, PREFIX + "Legacy");
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated, sort_order,"
                        + " name_localization_id) VALUES (?, ?, ?, 'Y', ?, NOW(), 950, ?)",
                testId, PREFIX + "Legacy", description, UUID.randomUUID().toString(), localizationId);
    }

    private void insertNumericResult(long id, long testId, int significantDigits) {
        jdbc.update(
                "INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, significant_digits, sort_order,"
                        + " is_active, lastupdated) VALUES (?, ?, 'N', ?, 1, true, NOW())",
                id, testId, significantDigits);
    }

    private void load(DomainConfigurationHandler handler, String csv, String fileName) throws Exception {
        handler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), fileName);
    }

    private Integer count(String table, String where) {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims." + table + " WHERE " + where, Integer.class);
    }

    private Map<String, Object> testRow(String description) {
        return jdbc.queryForMap("SELECT id, description, local_code, is_reportable, notify_results, is_active,"
                + " orderable, loinc, test_section_id, name_localization_id FROM clinlims.test WHERE description = ?",
                description);
    }

    private long sectionId() {
        return jdbc.queryForObject("SELECT id FROM clinlims.test_section WHERE name = ?", Long.class, SECTION);
    }

    private String translation(long localizationId, String locale) {
        return jdbc.queryForObject(
                "SELECT value FROM clinlims.localization_value WHERE localization_id = ? AND locale = ?", String.class,
                localizationId, locale);
    }

    private void cleanup() {
        List<Long> testLocalizations = jdbc.queryForList(
                "SELECT name_localization_id FROM clinlims.test"
                        + " WHERE description LIKE 'IT1193%' AND name_localization_id IS NOT NULL"
                        + " UNION SELECT reporting_name_localization_id FROM clinlims.test"
                        + " WHERE description LIKE 'IT1193%' AND reporting_name_localization_id IS NOT NULL",
                Long.class);
        for (Long testId : jdbc.queryForList("SELECT id FROM clinlims.test WHERE description LIKE 'IT1193%'",
                Long.class)) {
            jdbc.update("DELETE FROM clinlims.panel_item WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.test_terminology_mapping WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.test_result WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = ?", testId);
            jdbc.update("DELETE FROM clinlims.test WHERE id = ?", testId);
        }
        deleteLocalizations(testLocalizations);

        List<Long> panelLocalizations = jdbc.queryForList("SELECT name_localization_id FROM clinlims.panel"
                + " WHERE name LIKE 'IT1193%' AND name_localization_id IS NOT NULL", Long.class);
        for (Long panelId : jdbc.queryForList("SELECT id FROM clinlims.panel WHERE name LIKE 'IT1193%'", Long.class)) {
            jdbc.update("DELETE FROM clinlims.panel_item WHERE panel_id = ?", panelId);
            jdbc.update("DELETE FROM clinlims.sampletype_panel WHERE panel_id = ?", panelId);
            jdbc.update("DELETE FROM clinlims.panel WHERE id = ?", panelId);
        }
        deleteLocalizations(panelLocalizations);

        List<Long> sampleTypeLocalizations = jdbc
                .queryForList("SELECT name_localization_id FROM clinlims.type_of_sample"
                        + " WHERE description LIKE 'IT1193%' AND name_localization_id IS NOT NULL", Long.class);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE description LIKE 'IT1193%'");
        deleteLocalizations(sampleTypeLocalizations);

        List<Long> sectionLocalizations = jdbc.queryForList("SELECT name_localization_id FROM clinlims.test_section"
                + " WHERE name = ? AND name_localization_id IS NOT NULL", Long.class, SECTION);
        jdbc.update("DELETE FROM clinlims.test_section WHERE name = ?", SECTION);
        deleteLocalizations(sectionLocalizations);
    }

    private void deleteLocalizations(List<Long> localizationIds) {
        for (Long localizationId : localizationIds) {
            jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id = ?", localizationId);
            jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", localizationId);
        }
    }
}
