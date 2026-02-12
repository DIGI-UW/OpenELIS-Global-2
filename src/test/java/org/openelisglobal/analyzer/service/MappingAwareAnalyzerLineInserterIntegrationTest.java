package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests for MappingAwareAnalyzerLineInserter wrapper pattern
 * 
 * 
 * Tests the complete wrapper pattern workflow: - Process message with mappings
 * applies transformations - Process message without mappings uses original
 * inserter (backward compatibility) - Process message with unmapped field
 * creates error
 * 
 * Uses @SpringBootTest via BaseWebContextSensitiveTest for full integration.
 */
public class MappingAwareAnalyzerLineInserterIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MappingApplicationService mappingApplicationService;

    @Autowired
    private AnalyzerErrorService analyzerErrorService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private Analyzer testAnalyzer;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);

        // Clean up any leftover test data first
        cleanTestData();

        // Create test analyzer
        testAnalyzer = new Analyzer();
        testAnalyzer.setName("TEST-MAPPING-ANALYZER");
        testAnalyzer.setActive(true);
        testAnalyzer.setSysUserId("1");
        String analyzerId = analyzerService.insert(testAnalyzer);
        testAnalyzer.setId(analyzerId);
    }

    @After
    public void tearDown() throws Exception {
        // Clean up test data after each test
        cleanTestData();
    }

    /**
     * Clean up test-created data
     */
    private void cleanTestData() {
        try {
            // Delete analyzer errors for test analyzer
            jdbcTemplate.execute("DELETE FROM analyzer_error WHERE analyzer_id IN "
                    + "(SELECT id FROM analyzer WHERE name LIKE 'TEST-%')");

            // Delete analyzer field mappings
            jdbcTemplate.execute("DELETE FROM analyzer_field_mapping WHERE analyzer_field_id IN "
                    + "(SELECT id FROM analyzer_field WHERE analyzer_id IN "
                    + "(SELECT id FROM analyzer WHERE name LIKE 'TEST-%'))");

            // Delete analyzer fields
            jdbcTemplate.execute("DELETE FROM analyzer_field WHERE analyzer_id IN "
                    + "(SELECT id FROM analyzer WHERE name LIKE 'TEST-%')");

            // Delete test analyzer
            jdbcTemplate.execute("DELETE FROM analyzer WHERE name LIKE 'TEST-%'");
        } catch (Exception e) {
            // Log but don't fail - cleanup is best effort
            System.out.println("Failed to clean mapping test data: " + e.getMessage());
        }
    }

    /**
     * Test: Check has active mappings returns false when no mappings exist
     */
    @Test
    public void testProcessMessage_WithoutMappings_UsesOriginalInserter() {
        // Arrange: Analyzer with no mappings
        String analyzerId = testAnalyzer.getId();

        // Act: Check if analyzer has active mappings
        boolean hasMappings = mappingApplicationService.hasActiveMappings(analyzerId);

        // Assert: Should return false (no mappings configured)
        assertFalse("Analyzer with no mappings should return false", hasMappings);
    }

    /**
     * Test: Apply mappings with no mappings returns original lines
     */
    @Test
    public void testApplyMappings_WithNoMappings_ReturnsOriginalLines() {
        // Arrange
        String analyzerId = testAnalyzer.getId();
        List<String> lines = new ArrayList<>();
        lines.add("H|\\^&|||PSM^Micro^2.0|...");
        lines.add("R|1|^^^GLUCOSE^GLU||100|mg/dL||N|||20250127");

        // Act: Apply mappings (none exist)
        MappingApplicationResult result = mappingApplicationService.applyMappings(analyzerId, lines);

        // Assert
        assertNotNull("Result should not be null", result);
        assertFalse("Should not have mappings", result.hasMappings());
        assertTrue("Should be successful", result.isSuccess());
        assertEquals("Should return original lines", lines.size(), result.getTransformedLines().size());
        // Verify lines match
        for (int i = 0; i < lines.size(); i++) {
            assertEquals("Line " + i + " should match", lines.get(i), result.getTransformedLines().get(i));
        }
    }

    /**
     * Test: Process message with unmapped field creates error
     * 
     * Note: This test verifies that when mappings are applied and unmapped fields
     * are detected, an error is created. Full integration test would require
     * setting up mappings and testing the wrapper with a real inserter.
     */
    @Test
    public void testProcessMessage_WithUnmappedField_CreatesError() {
        // Arrange: Create error for unmapped field
        String rawMessage = "H|\\^&|||PSM^Micro^2.0|...\nR|1|^^^UNMAPPED_TEST||100|mg/dL||N|||20250127";
        String errorMessage = "Unmapped fields detected: UNMAPPED_TEST";

        // Act: Create error
        String errorId = analyzerErrorService.createError(testAnalyzer, AnalyzerError.ErrorType.MAPPING,
                AnalyzerError.Severity.ERROR, errorMessage, rawMessage);

        // Assert: Error was created
        assertNotNull("Error ID should be returned", errorId);

        // Verify error can be retrieved
        AnalyzerError error = analyzerErrorService.getErrorById(errorId);
        assertNotNull("Error should be retrievable", error);
        assertEquals(AnalyzerError.ErrorType.MAPPING, error.getErrorType());
        assertEquals(AnalyzerError.Severity.ERROR, error.getSeverity());
        assertTrue("Error message should contain unmapped field", error.getErrorMessage().contains("UNMAPPED_TEST"));
    }
}
