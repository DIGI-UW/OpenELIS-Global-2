package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.form.AnalyzerResultValueOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Guards catalog binding against the real ORM/query path. Mocked service tests
 * cannot prove that inactive options or options from another component stay
 * outside the mapped analyzer test's result catalog.
 */
public class AnalyzerResultValueOptionServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long ANALYZER_ID = 95101L;
    private static final long MAPPED_TEST_ID = 95101L;
    private static final long OTHER_TEST_ID = 95102L;
    private static final long VALID_OPTION_ID = 95101L;
    private static final long INACTIVE_OPTION_ID = 95102L;
    private static final long OTHER_TEST_OPTION_ID = 95103L;
    private static final String MAPPED_COMPONENT_ID = "95101000-0000-0000-0000-000000000001";
    private static final String OTHER_COMPONENT_ID = "95102000-0000-0000-0000-000000000001";

    @Autowired
    @Qualifier("analyzerResultValueOptionServiceImpl")
    private AnalyzerResultValueOptionService resultValueOptionService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        cleanup();

        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW()), (?, ?, ?, 'Y', ?, NOW())",
                MAPPED_TEST_ID, "Analyzer catalog mapped test", "Mapped test", UUID.randomUUID().toString(),
                OTHER_TEST_ID, "Analyzer catalog other test", "Other test", UUID.randomUUID().toString());
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, analyzer_type, is_active, last_updated)"
                + " VALUES (?, ?, ?, true, NOW())", ANALYZER_ID, "Catalog Guard Analyzer", "MOLECULAR");
        jdbc.update(
                "INSERT INTO clinlims.test_result_component"
                        + " (id, test_id, code, label, display_order, result_type, is_primary, is_active, lastupdated)"
                        + " VALUES (?, ?, 'PRIMARY', 'Mapped result', 0, 'D', true, 'Y', NOW()),"
                        + " (?, ?, 'PRIMARY', 'Other result', 0, 'D', true, 'Y', NOW())",
                MAPPED_COMPONENT_ID, MAPPED_TEST_ID, OTHER_COMPONENT_ID, OTHER_TEST_ID);
        jdbc.update("INSERT INTO clinlims.analyzer_test_map"
                + " (analyzer_id, analyzer_test_name, test_id, component_id, last_updated)"
                + " VALUES (?, 'MTB', ?, ?, NOW())", ANALYZER_ID, MAPPED_TEST_ID, MAPPED_COMPONENT_ID);
        jdbc.update(
                "INSERT INTO clinlims.test_result"
                        + " (id, test_id, tst_rslt_type, value, sort_order, is_active, is_normal, component_id,"
                        + " lastupdated)" + " VALUES (?, ?, 'D', 'Detected', '1', true, true, ?, NOW()),"
                        + " (?, ?, 'D', 'Inactive', '2', false, false, ?, NOW()),"
                        + " (?, ?, 'D', 'Other test option', '1', true, false, ?, NOW())",
                VALID_OPTION_ID, MAPPED_TEST_ID, MAPPED_COMPONENT_ID, INACTIVE_OPTION_ID, MAPPED_TEST_ID,
                MAPPED_COMPONENT_ID, OTHER_TEST_OPTION_ID, OTHER_TEST_ID, OTHER_COMPONENT_ID);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        if (jdbc == null) {
            return;
        }
        jdbc.update("DELETE FROM clinlims.analyzer_test_map WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.test_result WHERE id IN (?, ?, ?)", VALID_OPTION_ID, INACTIVE_OPTION_ID,
                OTHER_TEST_OPTION_ID);
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE id IN (?, ?)", MAPPED_COMPONENT_ID,
                OTHER_COMPONENT_ID);
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?)", MAPPED_TEST_ID, OTHER_TEST_ID);
    }

    @Test
    public void catalogBindingAllowsOnlyActiveOptionsFromMappedComponent() {
        List<AnalyzerResultValueOption> options = resultValueOptionService.getOptions(String.valueOf(ANALYZER_ID),
                "MTB");

        assertEquals(1, options.size());
        assertEquals(String.valueOf(VALID_OPTION_ID), options.get(0).getId());
        assertThrows(IllegalArgumentException.class, () -> resultValueOptionService
                .requireValidOption(String.valueOf(ANALYZER_ID), "MTB", String.valueOf(INACTIVE_OPTION_ID)));
        assertThrows(IllegalArgumentException.class, () -> resultValueOptionService
                .requireValidOption(String.valueOf(ANALYZER_ID), "MTB", String.valueOf(OTHER_TEST_OPTION_ID)));
    }
}
