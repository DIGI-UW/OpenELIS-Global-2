package org.openelisglobal.analyzerimport.analyzerreaders;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests for ASTMAnalyzerReader's tiered analyzer identification.
 * Verifies that bridge headers (X-Source-Id, X-Source-Port) enable
 * deterministic Strategy 0 lookup, and that absent/non-matching headers fall
 * through gracefully.
 */
public class ASTMAnalyzerReaderIdentificationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private DataSource dataSource;

    private ASTMAnalyzerReader reader;
    private Analyzer testAnalyzer;

    private static final String ASTM_MESSAGE = "H|\\^&|||TestManufacturer^TestModel^1.0\n" + "P|1||PATIENT001\n"
            + "L|1|N\n";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        reader = new ASTMAnalyzerReader();

        // Resync analyzer_seq — other tests may leave rows via NOT_SUPPORTED
        // propagation
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute(
                "SELECT setval('clinlims.analyzer_seq', CAST(COALESCE((SELECT MAX(id) FROM clinlims.analyzer), 0) + 1 AS BIGINT), false)");

        // Create and save an analyzer with IP+port (no type required).
        // Use 192.0.2.x (RFC 5737 TEST-NET) to avoid fixture collisions in CI.
        testAnalyzer = new Analyzer();
        testAnalyzer.setName("Test ASTM Analyzer");
        testAnalyzer.setIpAddress("192.0.2.42");
        testAnalyzer.setPort(9000);
        testAnalyzer = analyzerService.save(testAnalyzer);
    }

    @After
    public void tearDown() throws Exception {
        if (testAnalyzer != null && testAnalyzer.getId() != null) {
            analyzerService.delete(testAnalyzer);
        }
    }

    /**
     * Verify the test analyzer was saved and can be found by IP+port (Strategy 0
     * prerequisite).
     */
    @Test
    public void ipPortLookup_withMatchingAnalyzer_returnsAnalyzer() {
        Optional<Analyzer> found = analyzerService.getByIpAddressAndPort("192.0.2.42", 9000);
        assertTrue("Analyzer should be findable by IP+port", found.isPresent());
        assertEquals(testAnalyzer.getId(), found.get().getId());
    }

    /**
     * Verify IP-only lookup works (Strategy 2 in ASTM reader).
     */
    @Test
    public void ipLookup_withMatchingAnalyzer_returnsAnalyzer() {
        Optional<Analyzer> found = analyzerService.getByIpAddress("192.0.2.42");
        assertTrue("Analyzer should be findable by IP", found.isPresent());
        assertEquals(testAnalyzer.getId(), found.get().getId());
    }

    /**
     * Non-matching IP+port should return empty.
     */
    @Test
    public void ipPortLookup_withNonMatchingValues_returnsEmpty() {
        Optional<Analyzer> found = analyzerService.getByIpAddressAndPort("10.0.0.99", 12345);
        assertFalse("Non-matching IP+port should return empty", found.isPresent());
    }

    /**
     * When no bridge headers are set, processData should fall through all
     * strategies gracefully and fail with a clear error (no plugin matched).
     */
    @Test
    public void processData_withoutBridgeHeaders_fallsThroughGracefully() throws Exception {
        boolean read = reader.readStream(new ByteArrayInputStream(ASTM_MESSAGE.getBytes(StandardCharsets.UTF_8)));
        assertTrue("readStream should succeed", read);

        boolean result = reader.processData("1");
        assertFalse("processData should fail (no matching plugin)", result);
        assertNotNull("error should be set", reader.getError());
        assertTrue("error should mention no plugin matched", reader.getError().contains("No ASTM plugin matched"));
    }

    /**
     * When non-matching bridge headers are set, processData should fall through
     * without throwing exceptions.
     */
    @Test
    public void processData_withNonMatchingHeaders_fallsThroughGracefully() throws Exception {
        reader.setClientIpAddress("10.0.0.99");
        reader.setClientPort(12345);

        boolean read = reader.readStream(new ByteArrayInputStream(ASTM_MESSAGE.getBytes(StandardCharsets.UTF_8)));
        assertTrue("readStream should succeed", read);

        boolean result = reader.processData("1");
        assertFalse("processData should fail (no matching plugin)", result);
        assertNotNull("error should be set", reader.getError());
    }
}
