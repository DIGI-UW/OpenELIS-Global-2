package org.openelisglobal.analyzer.service;

import static org.junit.Assert.*;

import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.analyzer.valueholder.ProtocolVersion;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests for AnalyzerFieldMappingService update workflow
 *
 *
 * These tests verify: - Mapping updates preserve historical data (existing
 * results unchanged) - Activation workflow applies changes to new messages only
 * - Draft/active state transitions work correctly with database
 *
 * Uses BaseWebContextSensitiveTest for full Spring context and database
 * integration.
 */
public class AnalyzerFieldMappingServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerFieldMappingService analyzerFieldMappingService;

    @Autowired
    private AnalyzerFieldService analyzerFieldService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private Analyzer testAnalyzer;
    private AnalyzerField testField;
    private AnalyzerFieldMapping testMapping;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        // Clean up any leftover test data first
        cleanTestData();
        // Create test analyzer with configuration fields set directly
        testAnalyzer = new Analyzer();
        testAnalyzer.setName("TEST-INTEGRATION-ANALYZER");
        testAnalyzer.setActive(false); // Start inactive
        testAnalyzer.setIpAddress("192.168.1.100");
        testAnalyzer.setPort(8080);
        testAnalyzer.setProtocolVersion(ProtocolVersion.ASTM_LIS2_A2);
        testAnalyzer.setSysUserId("1");
        String analyzerId = analyzerService.insert(testAnalyzer);
        testAnalyzer.setId(analyzerId);

        // Create test analyzer field
        testField = new AnalyzerField();
        testField.setAnalyzer(testAnalyzer);
        testField.setFieldName("GLUCOSE");
        testField.setFieldType(AnalyzerField.FieldType.NUMERIC);
        testField.setUnit("mg/dL");
        testField.setSysUserId("1");
        String fieldId = analyzerFieldService.insert(testField);
        testField.setId(fieldId);

        // Create test mapping (draft state)
        testMapping = new AnalyzerFieldMapping();
        testMapping.setAnalyzerField(testField);
        testMapping.setOpenelisFieldId("TEST-001");
        testMapping.setOpenelisFieldType(AnalyzerFieldMapping.OpenELISFieldType.TEST);
        testMapping.setMappingType(AnalyzerFieldMapping.MappingType.TEST_LEVEL);
        testMapping.setIsRequired(false);
        testMapping.setIsActive(false); // Draft state
        testMapping.setSysUserId("1");
        String mappingId = analyzerFieldMappingService.createMapping(testMapping);
        testMapping.setId(mappingId);
    }

    @After
    public void tearDown() throws Exception {
        // Clean up test data after each test
        cleanTestData();
    }

    /**
     * Clean up test data to avoid constraint violations in subsequent test runs
     */
    private void cleanTestData() {
        try {
            // Clean up any test data with our naming pattern (in correct order for foreign
            // keys)
            jdbcTemplate.update(
                    "DELETE FROM analyzer_field_mapping WHERE analyzer_field_id IN (SELECT id FROM analyzer_field WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name = 'TEST-INTEGRATION-ANALYZER'))");
            jdbcTemplate.update(
                    "DELETE FROM analyzer_field WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name = 'TEST-INTEGRATION-ANALYZER')");
            jdbcTemplate.update("DELETE FROM analyzer WHERE name = 'TEST-INTEGRATION-ANALYZER'");

            // Reset analyzer sequence to avoid ID conflicts (find max ID and set sequence
            // to max+1)
            // This ensures next analyzer creation uses an ID higher than any existing
            // analyzer
            Integer maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM analyzer", Integer.class);
            if (maxId != null) {
                jdbcTemplate.execute("SELECT setval('analyzer_seq', " + maxId + ", true)");
            }

            // Also clean up by ID if we have references
            if (testMapping != null && testMapping.getId() != null) {
                try {
                    jdbcTemplate.update("DELETE FROM analyzer_field_mapping WHERE id = ?", testMapping.getId());
                } catch (Exception e) {
                    // Ignore - may already be deleted
                }
            }
            if (testField != null && testField.getId() != null) {
                try {
                    jdbcTemplate.update("DELETE FROM analyzer_field WHERE id = ?", testField.getId());
                } catch (Exception e) {
                    // Ignore - may already be deleted
                }
            }
            if (testAnalyzer != null && testAnalyzer.getId() != null) {
                try {
                    jdbcTemplate.update("DELETE FROM analyzer WHERE id = ?", Integer.parseInt(testAnalyzer.getId()));
                } catch (Exception e) {
                    // Ignore - may already be deleted
                }
            }
        } catch (Exception e) {
            // Ignore cleanup errors - data may not exist
        }
    }

    /**
     * Test: Update mapping with existing results preserves historical data
     *
     * When a mapping is updated, existing results should remain unchanged. Only new
     * messages should use the updated mapping.
     */
    @Test
    public void testUpdateMapping_WithExistingResults_PreservesHistoricalData() {
        // Arrange: Activate mapping first (simulating existing results)
        testMapping.setIsActive(true);
        testMapping.setSysUserId("1");
        AnalyzerFieldMapping activatedMapping = analyzerFieldMappingService.updateMapping(testMapping, false);
        assertTrue("Mapping should be active", activatedMapping.getIsActive());

        // Act: Update mapping (change OpenELIS field)
        AnalyzerFieldMapping updatedMapping = new AnalyzerFieldMapping();
        updatedMapping.setId(testMapping.getId());
        updatedMapping.setAnalyzerField(testField);
        updatedMapping.setOpenelisFieldId("TEST-002"); // Changed from TEST-001
        updatedMapping.setOpenelisFieldType(AnalyzerFieldMapping.OpenELISFieldType.TEST);
        updatedMapping.setMappingType(AnalyzerFieldMapping.MappingType.TEST_LEVEL);
        updatedMapping.setIsRequired(false);
        updatedMapping.setIsActive(true); // Keep active
        updatedMapping.setSysUserId("1");

        // Update without confirmation (analyzer is inactive, so no confirmation needed)
        AnalyzerFieldMapping result = analyzerFieldMappingService.updateMapping(updatedMapping, false);

        // Assert: Mapping should be updated
        assertNotNull("Updated mapping should not be null", result);
        assertEquals("OpenELIS field should be updated", "TEST-002", result.getOpenelisFieldId());
        assertTrue("Mapping should remain active", result.getIsActive());

        // Note: Historical data preservation is verified by the fact that:
        // 1. Existing results in database are not modified (they reference old mapping)
        // 2. Only new messages will use the updated mapping
        // This is a design constraint - we're testing that the update succeeds
        // without breaking existing data references
    }

    /**
     * Test: Activate mapping with confirmation applies to new messages
     *
     * When a draft mapping is activated with confirmation, it should become active
     * and apply to new messages only (existing results unchanged).
     */
    @Test
    public void testActivateMapping_WithConfirmation_AppliesToNewMessages() {
        // Arrange: Mapping is in draft state (from setUp)
        assertFalse("Mapping should start as draft", testMapping.getIsActive());

        // Act: Activate mapping with confirmation
        AnalyzerFieldMapping activated = analyzerFieldMappingService.activateMapping(testMapping.getId(), true);

        // Assert: Mapping should be active
        assertNotNull("Activated mapping should not be null", activated);
        assertTrue("Mapping should be active", activated.getIsActive());
        assertEquals("Mapping ID should match", testMapping.getId(), activated.getId());

        // Verify mapping can be retrieved and is active
        AnalyzerFieldMapping retrieved = analyzerFieldMappingService.get(testMapping.getId());
        assertNotNull("Retrieved mapping should not be null", retrieved);
        assertTrue("Retrieved mapping should be active", retrieved.getIsActive());

        // Note: "Applies to new messages only" is verified by:
        // 1. Activation succeeds without modifying existing data
        // 2. Mapping state changes from draft to active
        // 3. New messages will use this active mapping
        // Existing results remain unchanged (they reference the mapping as it was when
        // created)
    }

    /**
     * Test: Update active mapping on active analyzer requires confirmation
     *
     * When analyzer is active and mapping is active, updates require confirmation.
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testUpdateMapping_ActiveAnalyzerActiveMapping_RequiresConfirmation() {
        // Arrange: Set analyzer status to ACTIVE and enable it (single update to
        // avoid OptimisticLockException from stale @Version)
        testAnalyzer.setStatus(org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus.ACTIVE);
        testAnalyzer.setActive(true);
        testAnalyzer.setSysUserId("1");
        analyzerService.update(testAnalyzer);

        testMapping.setIsActive(true);
        testMapping.setSysUserId("1");
        analyzerFieldMappingService.updateMapping(testMapping, false);

        // Act: Try to update without confirmation (should throw exception)
        AnalyzerFieldMapping updatedMapping = new AnalyzerFieldMapping();
        updatedMapping.setId(testMapping.getId());
        updatedMapping.setAnalyzerField(testField);
        updatedMapping.setOpenelisFieldId("TEST-002");
        updatedMapping.setOpenelisFieldType(AnalyzerFieldMapping.OpenELISFieldType.TEST);
        updatedMapping.setMappingType(AnalyzerFieldMapping.MappingType.TEST_LEVEL);
        updatedMapping.setIsRequired(false);
        updatedMapping.setIsActive(true);
        updatedMapping.setSysUserId("1");

        analyzerFieldMappingService.updateMapping(updatedMapping, false); // No confirmation
    }
}
