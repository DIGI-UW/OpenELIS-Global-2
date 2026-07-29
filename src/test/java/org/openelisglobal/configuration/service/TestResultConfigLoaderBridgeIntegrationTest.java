package org.openelisglobal.configuration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The config loader writes tests/test_results in the legacy shape; this
 * verifies it now also bridges a loaded test into the new editor model — a
 * PRIMARY result component (Sample &amp; Results) and a LOINC terminology
 * mapping.
 */
public class TestResultConfigLoaderBridgeIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95601L;
    private static final String TEST_NAME = "ConfigBridgeIT";

    // The handler is a @Transactional JDK proxy, so inject it by its interface.
    @Autowired
    @Qualifier("testResultConfigurationHandler")
    private DomainConfigurationHandler handler;

    @Autowired
    private TestResultComponentService componentService;

    @Autowired
    private TestTerminologyMappingService terminologyService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, loinc, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, '1558-6', NOW())",
                TEST_ID, TEST_NAME, TEST_NAME, UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        try {
            jdbc.update("DELETE FROM clinlims.test_terminology_mapping WHERE test_id = ?", TEST_ID);
            // test_result.component_id references test_result_component, so delete the
            // results before the components.
            jdbc.update("DELETE FROM clinlims.test_result WHERE test_id = ?", TEST_ID);
            jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", TEST_ID);
        } catch (Exception ignored) {
            // tables may be absent in a degenerate schema
        }
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }

    @Test
    public void loadingTestResults_bridgesToPrimaryComponentAndTerminology() throws Exception {
        String csv = "testName,resultType,resultValue,sortOrder,isQuantifiable,isActive,isNormal,"
                + "significantDigits,flags\n" + TEST_NAME + ",N,,1,Y,Y,N,2,\n";
        handler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                "test-results.csv");

        // Sample & Results: a PRIMARY component was created from the legacy result.
        List<TestResultComponent> components = componentService.getActiveComponentsByTestId(String.valueOf(TEST_ID));
        assertFalse("a component must be created for the loaded test", components.isEmpty());
        TestResultComponent primary = components.get(0);
        assertTrue("the component is flagged primary", primary.getIsPrimary());
        assertEquals("N", primary.getResultType());

        // Terminology: the test's LOINC became a mapping.
        assertFalse("a LOINC terminology mapping must be created",
                terminologyService.getActiveByTestId(String.valueOf(TEST_ID)).isEmpty());
    }
}
