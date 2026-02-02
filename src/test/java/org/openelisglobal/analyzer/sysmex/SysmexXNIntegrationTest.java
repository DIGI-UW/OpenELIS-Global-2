/**
 * Integration test for Sysmex XN Series HL7 (hematology/CBC).
 *
 * <p>Task Reference: T217 [M14] Sysmex XN HL7 integration - P2 analyzer validation.
 * Tests the SysmexXN-L plugin with HL7AnalyzerReader for XN Series analyzers.
 */
package org.openelisglobal.analyzer.sysmex;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import javax.sql.DataSource;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerConfigurationService;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.analyzerreaders.HL7AnalyzerReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

public class SysmexXNIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String SYSMEX_XN_ANALYZER_NAME = "SYSMEX";
    private static final String SYSMEX_XN_IP = "192.168.1.110";
    private static final String FIXTURE_PATH = "testdata/hl7/sysmex-xn-result.hl7";

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerConfigurationService analyzerConfigurationService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private HL7AnalyzerReader reader;
    private Analyzer sysmexAnalyzer;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        reader = new HL7AnalyzerReader();
        cleanTestData();
        createSysmexAnalyzerAndConfig();
    }

    @After
    public void tearDown() throws Exception {
        cleanTestData();
    }

    private void createSysmexAnalyzerAndConfig() {
        sysmexAnalyzer = new Analyzer();
        sysmexAnalyzer.setName(SYSMEX_XN_ANALYZER_NAME);
        sysmexAnalyzer.setActive(true);
        sysmexAnalyzer.setSysUserId("1");
        String analyzerId = analyzerService.insert(sysmexAnalyzer);
        sysmexAnalyzer.setId(analyzerId);

        analyzerConfigurationService.createConfiguration(sysmexAnalyzer, SYSMEX_XN_IP, 2575, Collections.emptyList());
    }

    private void cleanTestData() {
        try {
            jdbcTemplate.update(
                    "DELETE FROM clinlims.analyzer_results WHERE analyzer_id IN (SELECT id FROM clinlims.analyzer WHERE name = ?)",
                    SYSMEX_XN_ANALYZER_NAME);
            jdbcTemplate.update(
                    "DELETE FROM clinlims.analyzer_configuration WHERE analyzer_id IN (SELECT id FROM clinlims.analyzer WHERE name = ?)",
                    SYSMEX_XN_ANALYZER_NAME);
            jdbcTemplate.update("DELETE FROM clinlims.analyzer WHERE name = ?", SYSMEX_XN_ANALYZER_NAME);
        } catch (Exception e) {
            // best-effort cleanup
        }
    }

    @Test
    public void sysmexXnHl7Message_resultsStored() throws Exception {
        String raw = loadFixture(FIXTURE_PATH);
        InputStream in = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));

        boolean readOk = reader.readStream(in);
        assertTrue("readStream should succeed", readOk);
        assertNotNull("analyzer should be identifiable from MSH", sysmexAnalyzer.getId());

        boolean insertOk = reader.insertAnalyzerData("1");
        assertTrue("insertAnalyzerData should succeed: " + reader.getError(), insertOk);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clinlims.analyzer_results WHERE analyzer_id = ?", Integer.class,
                Integer.parseInt(sysmexAnalyzer.getId()));
        assertTrue("Expected at least one analyzer result", count != null && count >= 1);
    }

    @Test
    public void sysmexXnHl7Message_parsesCorrectValues() throws Exception {
        // Given: HL7 fixture with specific CBC values
        String raw = loadFixture(FIXTURE_PATH);
        InputStream in = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));

        // When: parse and insert
        boolean readOk = reader.readStream(in);
        assertTrue("readStream should succeed", readOk);
        boolean insertOk = reader.insertAnalyzerData("1");
        assertTrue("insertAnalyzerData should succeed", insertOk);

        // Then: verify specific values from fixture are stored correctly
        // Fixture has: WBC=9.2, RBC=5.1, HGB=15.5, PLT=275 (13 total OBX segments)
        Integer totalResults = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clinlims.analyzer_results WHERE analyzer_id = ?", Integer.class,
                Integer.parseInt(sysmexAnalyzer.getId()));
        assertTrue("Expected 13 results (one per OBX segment)", totalResults != null && totalResults == 13);

        // Verify WBC value (first OBX segment)
        String wbcValue = jdbcTemplate.queryForObject(
                "SELECT result FROM clinlims.analyzer_results WHERE analyzer_id = ? AND test_name LIKE '%WBC%' LIMIT 1",
                String.class, Integer.parseInt(sysmexAnalyzer.getId()));
        assertNotNull("WBC result should be stored", wbcValue);
        assertTrue("WBC value should be 9.2", wbcValue.contains("9.2"));

        // Verify PLT value (8th OBX segment)
        String pltValue = jdbcTemplate.queryForObject(
                "SELECT result FROM clinlims.analyzer_results WHERE analyzer_id = ? AND test_name LIKE '%PLT%' LIMIT 1",
                String.class, Integer.parseInt(sysmexAnalyzer.getId()));
        assertNotNull("PLT result should be stored", pltValue);
        assertTrue("PLT value should be 275", pltValue.contains("275"));
    }

    // TODO: When adding more Sysmex analyzer tests (e.g., XT-2000i), extract common
    // setup/cleanup/verification logic into AbstractSysmexIntegrationTest similar
    // to
    // AbstractMindrayIntegrationTest pattern. See PR #2674 review.

    private static String loadFixture(String path) throws Exception {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }
}
