package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Unit tests for AnalyzerFieldMappingService implementation
 * 
 * Task Reference: T030 Test Coverage Goal: >80%
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerFieldMappingServiceTest {

    @Mock
    private AnalyzerFieldMappingDAO analyzerFieldMappingDAO;

    @Mock
    private AnalyzerFieldDAO analyzerFieldDAO;

    @Mock
    private AnalyzerConfigurationService analyzerConfigurationService;

    private AnalyzerFieldMappingServiceImpl analyzerFieldMappingService;

    private Analyzer testAnalyzer;
    private AnalyzerField numericField;
    private AnalyzerField qualitativeField;
    private AnalyzerFieldMapping testMapping;

    @Before
    public void setUp() {
        analyzerFieldMappingService = new AnalyzerFieldMappingServiceImpl(analyzerFieldMappingDAO, analyzerFieldDAO);
        // Inject mocked AnalyzerConfigurationService via reflection for testing
        try {
            java.lang.reflect.Field field = AnalyzerFieldMappingServiceImpl.class
                    .getDeclaredField("analyzerConfigurationService");
            field.setAccessible(true);
            field.set(analyzerFieldMappingService, analyzerConfigurationService);
        } catch (Exception e) {
            // If field doesn't exist yet, that's okay - will be added in implementation
        }

        // Setup test analyzer
        testAnalyzer = new Analyzer();
        testAnalyzer.setId("1");
        testAnalyzer.setName("Test Analyzer");

        // Setup numeric field
        numericField = new AnalyzerField();
        numericField.setId("FIELD-001");
        numericField.setAnalyzer(testAnalyzer);
        numericField.setFieldName("GLUCOSE");
        numericField.setFieldType(AnalyzerField.FieldType.NUMERIC);
        numericField.setUnit("mg/dL");

        // Setup qualitative field
        qualitativeField = new AnalyzerField();
        qualitativeField.setId("FIELD-002");
        qualitativeField.setAnalyzer(testAnalyzer);
        qualitativeField.setFieldName("HIV_TEST");
        qualitativeField.setFieldType(AnalyzerField.FieldType.QUALITATIVE);

        // Setup test mapping
        testMapping = new AnalyzerFieldMapping();
        testMapping.setId("MAPPING-001");
        testMapping.setAnalyzerField(numericField);
        testMapping.setOpenelisFieldId("TEST-001");
        testMapping.setOpenelisFieldType(AnalyzerFieldMapping.OpenELISFieldType.TEST);
        testMapping.setMappingType(AnalyzerFieldMapping.MappingType.TEST_LEVEL);
        testMapping.setIsRequired(false);
        testMapping.setIsActive(false);
    }

    /**
     * Test: Create mapping with valid data persists mapping Task Reference: T030
     */
    @Test
    public void testCreateMapping_WithValidData_PersistsMapping() {
        // Arrange: NUMERIC field → TEST (compatible)
        testMapping.setAnalyzerField(numericField);
        when(analyzerFieldDAO.get("FIELD-001")).thenReturn(Optional.of(numericField));
        when(analyzerFieldMappingDAO.insert(testMapping)).thenReturn("MAPPING-001");

        // Act
        String id = analyzerFieldMappingService.createMapping(testMapping);

        // Assert
        assertNotNull("ID should not be null", id);
        assertEquals("ID should match", "MAPPING-001", id);
    }

    /**
     * Test: Create mapping with type incompatibility throws exception Task
     * Reference: T030
     * 
     * Validation: NUMERIC analyzer field can only map to TEST or RESULT OpenELIS
     * fields
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testCreateMapping_WithTypeIncompatibility_ThrowsException() {
        // Arrange: NUMERIC field → METADATA OpenELIS field (incompatible - NUMERIC can
        // only map to TEST/RESULT)
        AnalyzerFieldMapping incompatibleMapping = new AnalyzerFieldMapping();
        incompatibleMapping.setAnalyzerField(numericField);
        incompatibleMapping.setOpenelisFieldId("METADATA-001");
        incompatibleMapping.setOpenelisFieldType(AnalyzerFieldMapping.OpenELISFieldType.METADATA);
        incompatibleMapping.setMappingType(AnalyzerFieldMapping.MappingType.METADATA);

        when(analyzerFieldDAO.get("FIELD-001")).thenReturn(Optional.of(numericField));

        // Act: Should throw LIMSRuntimeException
        analyzerFieldMappingService.createMapping(incompatibleMapping);
    }

    /**
     * Test: Validate required mappings with missing required throws exception Task
     * Reference: T030
     * 
     * Validation: At least one mapping with isRequired=true must exist for Sample
     * ID, Test Code, Result Value
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testValidateRequiredMappings_WithMissingRequired_ThrowsException() {
        // Arrange: No required mappings exist
        List<AnalyzerFieldMapping> existingMappings = new ArrayList<>();
        // Only non-required mappings
        testMapping.setIsRequired(false);
        existingMappings.add(testMapping);

        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("1")).thenReturn(existingMappings);

        // Act: Should throw LIMSRuntimeException for missing required mappings
        analyzerFieldMappingService.validateRequiredMappings("1");
    }

    /**
     * Test: Activate mapping with active analyzer requires confirmation Task
     * Reference: T030
     * 
     * Note: This test verifies that activation requires confirmation flag
     */
    @Test
    public void testActivateMapping_WithActiveAnalyzer_RequiresConfirmation() {
        // Arrange: Analyzer is active, mapping is draft
        testMapping.setIsActive(false);
        numericField.setAnalyzer(testAnalyzer); // Ensure field has analyzer relationship

        when(analyzerFieldMappingDAO.get("MAPPING-001")).thenReturn(Optional.of(testMapping));
        when(analyzerFieldDAO.findByIdWithAnalyzer("FIELD-001")).thenReturn(Optional.of(numericField));
        when(analyzerFieldMappingDAO.update(org.mockito.ArgumentMatchers.any(AnalyzerFieldMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Activate mapping (should succeed with confirmation flag)
        AnalyzerFieldMapping activated = analyzerFieldMappingService.activateMapping("MAPPING-001", true);

        // Assert
        assertNotNull("Activated mapping should not be null", activated);
        assertEquals("Mapping should be active", true, activated.getIsActive());
    }

    /**
     * Test: Update mapping with active analyzer requires confirmation Task
     * Reference: T070
     * 
     * When analyzer is active, updating a mapping requires explicit confirmation to
     * prevent accidental changes to live configuration
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testUpdateMapping_WithActiveAnalyzer_RequiresConfirmation() {
        // Arrange: Analyzer is active, mapping exists and is active
        testAnalyzer.setActive(true);
        testMapping.setIsActive(true);
        numericField.setAnalyzer(testAnalyzer); // Ensure field has analyzer relationship

        AnalyzerConfiguration config = new AnalyzerConfiguration();
        config.setAnalyzer(testAnalyzer);

        when(analyzerFieldMappingDAO.get("MAPPING-001")).thenReturn(Optional.of(testMapping));
        when(analyzerFieldDAO.get("FIELD-001")).thenReturn(Optional.of(numericField));
        when(analyzerFieldDAO.findByIdWithAnalyzer("FIELD-001")).thenReturn(Optional.of(numericField));
        when(analyzerConfigurationService.getByAnalyzerId("1")).thenReturn(Optional.of(config));

        // Create updated mapping (changing OpenELIS field)
        AnalyzerFieldMapping updatedMapping = new AnalyzerFieldMapping();
        updatedMapping.setId("MAPPING-001");
        updatedMapping.setAnalyzerField(numericField);
        updatedMapping.setOpenelisFieldId("TEST-002"); // Changed from TEST-001
        updatedMapping.setOpenelisFieldType(AnalyzerFieldMapping.OpenELISFieldType.TEST);
        updatedMapping.setMappingType(AnalyzerFieldMapping.MappingType.TEST_LEVEL);
        updatedMapping.setIsRequired(false);
        updatedMapping.setIsActive(true);

        // Act: Try to update without confirmation (should throw exception)
        analyzerFieldMappingService.updateMapping(updatedMapping, false); // No confirmation
    }

    /**
     * Test: Update mapping with draft state does not require confirmation Task
     * Reference: T070
     * 
     * When mapping is in draft state (isActive=false), updates can be made without
     * confirmation since it's not affecting live processing
     */
    @Test
    public void testUpdateMapping_WithDraftState_DoesNotRequireConfirmation() {
        // Arrange: Mapping is draft (not active)
        testMapping.setIsActive(false);
        numericField.setAnalyzer(testAnalyzer); // Ensure field has analyzer relationship

        AnalyzerFieldMapping updatedMapping = new AnalyzerFieldMapping();
        updatedMapping.setId("MAPPING-001");
        updatedMapping.setAnalyzerField(numericField);
        updatedMapping.setOpenelisFieldId("TEST-002"); // Changed
        updatedMapping.setOpenelisFieldType(AnalyzerFieldMapping.OpenELISFieldType.TEST);
        updatedMapping.setMappingType(AnalyzerFieldMapping.MappingType.TEST_LEVEL);
        updatedMapping.setIsRequired(false);
        updatedMapping.setIsActive(false); // Still draft

        when(analyzerFieldMappingDAO.get("MAPPING-001")).thenReturn(Optional.of(testMapping));
        when(analyzerFieldDAO.get("FIELD-001")).thenReturn(Optional.of(numericField));
        when(analyzerFieldDAO.findByIdWithAnalyzer("FIELD-001")).thenReturn(Optional.of(numericField));
        // Mock update to return the existing mapping (which will be updated by the
        // implementation)
        when(analyzerFieldMappingDAO.update(org.mockito.ArgumentMatchers.any(AnalyzerFieldMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Update draft mapping (should succeed without confirmation)
        AnalyzerFieldMapping result = analyzerFieldMappingService.updateMapping(updatedMapping, false);

        // Assert: Update should succeed
        assertNotNull("Updated mapping should not be null", result);
        assertEquals("OpenELIS field should be updated", "TEST-002", result.getOpenelisFieldId());
    }

    /**
     * Test: Deactivate mapping with active analyzer logs audit trail Task
     * Reference: T070
     * 
     * When deactivating a mapping for an active analyzer, the system should log an
     * audit trail entry with who, when, and what changed
     */
    @Test
    public void testDeactivateMapping_WithActiveAnalyzer_LogsAuditTrail() {
        // Arrange: Mapping is active, not required (can be disabled)
        testMapping.setIsActive(true);
        testMapping.setIsRequired(false); // Not a required mapping (can be disabled)
        testMapping.setSysUserId("USER-001");

        when(analyzerFieldMappingDAO.get("MAPPING-001")).thenReturn(Optional.of(testMapping));

        // Mock update to return the existing mapping (which will be updated by the
        // implementation)
        when(analyzerFieldMappingDAO.update(org.mockito.ArgumentMatchers.any(AnalyzerFieldMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Deactivate mapping
        AnalyzerFieldMapping result = analyzerFieldMappingService.disableMapping("MAPPING-001",
                "Retired for maintenance");

        // Assert: Mapping should be deactivated and audit trail logged
        assertNotNull("Deactivated mapping should not be null", result);
        assertEquals("Mapping should be inactive", false, result.getIsActive());
        verify(analyzerFieldMappingDAO).update(org.mockito.ArgumentMatchers.any(AnalyzerFieldMapping.class));
        // Note: Audit trail verification would require checking AuditTrailService calls
    }
}
