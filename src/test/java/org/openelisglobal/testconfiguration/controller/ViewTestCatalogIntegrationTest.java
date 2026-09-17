package org.openelisglobal.testconfiguration.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.test.valueholder.TestCatalog;
import org.openelisglobal.testconfiguration.controller.rest.TestCatalogRestController;
import org.openelisglobal.testconfiguration.form.TestCatalogForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Legacy View Test Catalog (/rest/TestCatalog) — the whole catalog must survive
 * unresolvable dictionary references.
 *
 * <p>
 * A dictionary-variant test result can point at a dictionary entry that was
 * removed (historic data ships that way), or carry no value at all. That used
 * to NPE inside the value lookup and fail the entire response with a 500, which
 * the page reads as "no test sections" and — once a section is picked — as a
 * crash, because its list state stays undefined.
 */
public class ViewTestCatalogIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 96301L;

    private static final long DICT_ID = 963010L;

    private static final long MISSING_DICT_ID = 963099L;

    private static final long VALID_RESULT_ID = 963020L;

    private static final long ORPHAN_RESULT_ID = 963021L;

    private static final long BLANK_RESULT_ID = 963022L;

    private static final String DICT_ENTRY = "ViewCatalogITPositive";

    @Autowired
    private org.openelisglobal.test.service.TestService testService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private TestCatalogRestController controller;

    private JdbcTemplate jdbc;

    private Long dictionaryCategoryId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        // The legacy controller is field-injected in production and has no
        // constructor to call; wire the three services it uses directly.
        controller = new TestCatalogRestController();
        inject("testService", testService);
        inject("dictionaryService", dictionaryService);
        inject("localizationService", localizationService);
        cleanup();
        dictionaryCategoryId = jdbc.queryForObject("SELECT min(id) FROM clinlims.dictionary_category", Long.class);
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "ViewCatalogIT", "ViewCatalogIT desc", UUID.randomUUID().toString());
        jdbc.update("INSERT INTO clinlims.dictionary (id, dict_entry, is_active, dictionary_category_id, lastupdated)"
                + " VALUES (?, ?, 'Y', ?, NOW())", DICT_ID, DICT_ENTRY, dictionaryCategoryId);
        seedDictionaryResult(VALID_RESULT_ID, String.valueOf(DICT_ID), 1);
        // the two rows that used to break the endpoint
        seedDictionaryResult(ORPHAN_RESULT_ID, String.valueOf(MISSING_DICT_ID), 2);
        seedDictionaryResult(BLANK_RESULT_ID, null, 3);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void showTestCatalog_skipsUnresolvableDictionaryValues_insteadOfFailingTheCatalog() {
        TestCatalogForm form = controller.showTestCatalog(new MockHttpServletRequest());

        assertNotNull(form.getTestCatalogList());
        assertNotNull(form.getTestSectionList());
        TestCatalog seeded = form.getTestCatalogList().stream()
                .filter(catalog -> String.valueOf(TEST_ID).equals(catalog.getId())).findFirst().orElse(null);
        assertNotNull("the seeded test is missing from the catalog", seeded);
        assertTrue(seeded.isHasDictionaryValues());
        assertTrue("the resolvable dictionary value should still be listed",
                seeded.getDictionaryValues().stream().anyMatch(value -> value.contains(DICT_ENTRY)));
        assertFalse("an unresolvable dictionary reference must not be listed", seeded.getDictionaryValues().stream()
                .anyMatch(value -> value.contains(String.valueOf(MISSING_DICT_ID))));
    }

    private void seedDictionaryResult(long id, String value, int sortOrder) {
        jdbc.update(
                "INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, value, sort_order, is_quantifiable,"
                        + " is_active, lastupdated) VALUES (?, ?, 'D', ?, ?, false, true, NOW())",
                id, TEST_ID, value, sortOrder);
    }

    private void inject(String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = TestCatalogRestController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test_result WHERE id IN (?, ?, ?)", VALID_RESULT_ID, ORPHAN_RESULT_ID,
                BLANK_RESULT_ID);
        jdbc.update("DELETE FROM clinlims.test_result WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.dictionary WHERE id = ?", DICT_ID);
    }
}
