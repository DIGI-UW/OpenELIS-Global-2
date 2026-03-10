package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.QualitativeResultMappingDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.QualitativeResultMapping;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Unit tests for QualitativeResultMappingService implementation
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class QualitativeResultMappingServiceTest {

    @Mock
    private QualitativeResultMappingDAO qualitativeResultMappingDAO;

    private QualitativeResultMappingServiceImpl qualitativeResultMappingService;

    private Analyzer testAnalyzer;
    private AnalyzerField qualitativeField;
    private QualitativeResultMapping testMapping;

    @Before
    public void setUp() {
        qualitativeResultMappingService = new QualitativeResultMappingServiceImpl(qualitativeResultMappingDAO);

        // Setup test analyzer
        testAnalyzer = new Analyzer();
        testAnalyzer.setId("1");
        testAnalyzer.setName("Test Analyzer");

        // Setup qualitative field
        qualitativeField = new AnalyzerField();
        qualitativeField.setId("FIELD-002");
        qualitativeField.setAnalyzer(testAnalyzer);
        qualitativeField.setFieldName("HIV_TEST");
        qualitativeField.setFieldType(AnalyzerField.FieldType.QUALITATIVE);

        // Setup test mapping
        testMapping = new QualitativeResultMapping();
        testMapping.setId("QUAL-MAPPING-001");
        testMapping.setAnalyzerField(qualitativeField);
        testMapping.setAnalyzerFieldId(qualitativeField.getId()); // Required for service validation
        testMapping.setAnalyzerValue("POSITIVE");
        testMapping.setOpenelisCode("POS");
        testMapping.setIsDefault(false);
    }

    /**
     * Test: Create mapping with many-to-one mapping persists multiple values
     * 
     * Many-to-one: Multiple analyzer values can map to the same OpenELIS code
     */
    @Test
    public void testCreateMapping_WithManyToOneMapping_PersistsMultipleValues() {
        // Arrange: Multiple analyzer values mapping to same OpenELIS code
        QualitativeResultMapping mapping1 = new QualitativeResultMapping();
        mapping1.setAnalyzerField(qualitativeField);
        mapping1.setAnalyzerFieldId(qualitativeField.getId());
        mapping1.setAnalyzerValue("POSITIVE");
        mapping1.setOpenelisCode("POS");
        mapping1.setIsDefault(false);

        QualitativeResultMapping mapping2 = new QualitativeResultMapping();
        mapping2.setAnalyzerField(qualitativeField);
        mapping2.setAnalyzerFieldId(qualitativeField.getId());
        mapping2.setAnalyzerValue("REACTIVE");
        mapping2.setOpenelisCode("POS"); // Same OpenELIS code
        mapping2.setIsDefault(false);

        when(qualitativeResultMappingDAO.insert(mapping1)).thenReturn("QUAL-MAPPING-001");
        when(qualitativeResultMappingDAO.insert(mapping2)).thenReturn("QUAL-MAPPING-002");

        // Act: Create both mappings
        String id1 = qualitativeResultMappingService.createMapping(mapping1);
        String id2 = qualitativeResultMappingService.createMapping(mapping2);

        // Assert: Both mappings should be created successfully
        assertNotNull("First mapping ID should not be null", id1);
        assertNotNull("Second mapping ID should not be null", id2);
        assertEquals("First mapping ID should match", "QUAL-MAPPING-001", id1);
        assertEquals("Second mapping ID should match", "QUAL-MAPPING-002", id2);
    }

    /**
     * Test: Create mapping with duplicate value throws exception
     * 
     * Validation: Unique constraint on (analyzer_field_id, analyzer_value)
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testCreateMapping_WithDuplicateValue_ThrowsException() {
        // Arrange: Check for existing mapping with same analyzer_field_id and
        // analyzer_value
        List<QualitativeResultMapping> existingMappings = new ArrayList<>();
        QualitativeResultMapping existing = new QualitativeResultMapping();
        existing.setId("EXISTING-001");
        existing.setAnalyzerField(qualitativeField);
        existing.setAnalyzerValue("POSITIVE");
        existing.setOpenelisCode("POS");
        existingMappings.add(existing);

        when(qualitativeResultMappingDAO.findByAnalyzerFieldId("FIELD-002")).thenReturn(existingMappings);

        // Act: Try to create duplicate mapping (same analyzer_field_id +
        // analyzer_value)
        QualitativeResultMapping duplicate = new QualitativeResultMapping();
        duplicate.setAnalyzerField(qualitativeField);
        duplicate.setAnalyzerFieldId(qualitativeField.getId());
        duplicate.setAnalyzerValue("POSITIVE"); // Duplicate value
        duplicate.setOpenelisCode("POSITIVE_CODE"); // Different code, but same analyzer_value

        // Should throw LIMSRuntimeException
        qualitativeResultMappingService.createMapping(duplicate);
    }

    /**
     * Test: Create mapping with valid data persists mapping
     */
    @Test
    public void testCreateMapping_WithValidData_PersistsMapping() {
        // Arrange
        when(qualitativeResultMappingDAO.findByAnalyzerFieldId("FIELD-002"))
                .thenReturn(new ArrayList<>()); // No existing mappings
        when(qualitativeResultMappingDAO.insert(testMapping)).thenReturn("QUAL-MAPPING-001");

        // Act
        String id = qualitativeResultMappingService.createMapping(testMapping);

        // Assert
        assertNotNull("ID should not be null", id);
        assertEquals("ID should match", "QUAL-MAPPING-001", id);
    }
}
