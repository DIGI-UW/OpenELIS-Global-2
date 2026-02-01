/**
 * Base class for Mindray HL7 integration tests.
 * Provides common setup, cleanup, and verification logic for BC-5380 and BS-360E tests.
 */
package org.openelisglobal.analyzer.mindray;

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
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerConfigurationService;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.analyzerreaders.HL7AnalyzerReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class AbstractMindrayIntegrationTest extends BaseWebContextSensitiveTest {

    protected static final String MINDRAY_ANALYZER_NAME = "MINDRAY";

    @Autowired
    protected AnalyzerService analyzerService;

    @Autowired
    protected AnalyzerConfigurationService analyzerConfigurationService;

    @Autowired
    protected DataSource dataSource;

    protected JdbcTemplate jdbcTemplate;
    protected HL7AnalyzerReader reader;
    protected Analyzer mindrayAnalyzer;

    protected abstract String getAnalyzerIp();

    protected abstract String getFixturePath();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        reader = new HL7AnalyzerReader();
        cleanTestData();
        createMindrayAnalyzerAndConfig();
    }

    @After
    public void tearDown() throws Exception {
        cleanTestData();
    }

    private void createMindrayAnalyzerAndConfig() {
        mindrayAnalyzer = new Analyzer();
        mindrayAnalyzer.setName(MINDRAY_ANALYZER_NAME);
        mindrayAnalyzer.setActive(true);
        mindrayAnalyzer.setSysUserId("1");
        String analyzerId = analyzerService.insert(mindrayAnalyzer);
        mindrayAnalyzer.setId(analyzerId);

        analyzerConfigurationService.createConfiguration(mindrayAnalyzer, getAnalyzerIp(), 2575,
                Collections.emptyList());
    }

    private void cleanTestData() {
        try {
            jdbcTemplate.update(
                    "DELETE FROM analyzer_results WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name = ?)",
                    MINDRAY_ANALYZER_NAME);
            jdbcTemplate.update(
                    "DELETE FROM analyzer_configuration WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name = ?)",
                    MINDRAY_ANALYZER_NAME);
            jdbcTemplate.update("DELETE FROM analyzer WHERE name = ?", MINDRAY_ANALYZER_NAME);
        } catch (Exception e) {
            // best-effort cleanup
        }
    }

    protected void verifyHl7MessageResultsStored() throws Exception {
        String raw = loadFixture(getFixturePath());
        InputStream in = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));

        boolean readOk = reader.readStream(in);
        assertTrue("readStream should succeed", readOk);
        assertNotNull("analyzer should be identifiable from MSH", mindrayAnalyzer.getId());

        boolean insertOk = reader.insertAnalyzerData("1");
        assertTrue("insertAnalyzerData should succeed: " + reader.getError(), insertOk);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM analyzer_results WHERE analyzer_id = ?",
                Integer.class, mindrayAnalyzer.getId());
        assertTrue("Expected at least one analyzer result", count != null && count >= 1);
    }

    protected static String loadFixture(String path) throws Exception {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }
}
