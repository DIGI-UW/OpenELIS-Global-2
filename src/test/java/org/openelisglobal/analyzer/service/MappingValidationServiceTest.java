package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerField.FieldType;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping.MappingType;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping.OpenELISFieldType;

/**
 * Unit tests for MappingValidationService
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class MappingValidationServiceTest {

    @Mock
    private AnalyzerFieldMappingDAO analyzerFieldMappingDAO;

    @Mock
    private AnalyzerFieldDAO analyzerFieldDAO;

    @InjectMocks
    private MappingValidationServiceImpl mappingValidationService;

    private AnalyzerField numericField;
    private AnalyzerField textField;
    private AnalyzerFieldMapping activeMapping;

    @Before
    public void setUp() {
        // Create test analyzer fields
        numericField = new AnalyzerField();
        numericField.setId("FIELD-001");
        numericField.setFieldName("Test Result");
        numericField.setFieldType(FieldType.NUMERIC);

        textField = new AnalyzerField();
        textField.setId("FIELD-002");
        textField.setFieldName("Test Name");
        textField.setFieldType(FieldType.TEXT);

        // Create test mapping
        activeMapping = new AnalyzerFieldMapping();
        activeMapping.setId("MAPPING-001");
        activeMapping.setAnalyzerField(numericField);
        activeMapping.setOpenelisFieldType(OpenELISFieldType.RESULT);
        activeMapping.setMappingType(MappingType.RESULT_LEVEL);
        activeMapping.setIsActive(true);
    }

    @Test
    public void testCalculateMappingAccuracy_WithAllFieldsMapped_ReturnsOne() {
        // Arrange
        List<AnalyzerField> allFields = Arrays.asList(numericField, textField);
        List<AnalyzerFieldMapping> activeMappings = Arrays.asList(activeMapping);

        when(analyzerFieldDAO.findByAnalyzerId("ANALYZER-001")).thenReturn(allFields);
        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("ANALYZER-001")).thenReturn(activeMappings);

        // Act
        double accuracy = mappingValidationService.calculateMappingAccuracy("ANALYZER-001");

        // Assert
        assertEquals("Accuracy should be 0.5 (1 of 2 fields mapped)", 0.5, accuracy, 0.01);
    }

    @Test
    public void testCalculateMappingAccuracy_WithNoFields_ReturnsZero() {
        // Arrange
        when(analyzerFieldDAO.findByAnalyzerId("ANALYZER-001")).thenReturn(Collections.emptyList());
        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("ANALYZER-001"))
                .thenReturn(Collections.emptyList());

        // Act
        double accuracy = mappingValidationService.calculateMappingAccuracy("ANALYZER-001");

        // Assert
        assertEquals("Accuracy should be 0.0 with no fields", 0.0, accuracy, 0.01);
    }

    @Test
    public void testIdentifyUnmappedFields_WithUnmappedFields_ReturnsFieldNames() {
        // Arrange
        List<AnalyzerField> allFields = Arrays.asList(numericField, textField);
        List<AnalyzerFieldMapping> activeMappings = Arrays.asList(activeMapping);

        when(analyzerFieldDAO.findByAnalyzerId("ANALYZER-001")).thenReturn(allFields);
        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("ANALYZER-001")).thenReturn(activeMappings);

        // Act
        List<String> unmappedFields = mappingValidationService.identifyUnmappedFields("ANALYZER-001");

        // Assert
        assertNotNull("Unmapped fields list should not be null", unmappedFields);
        assertEquals("Should have 1 unmapped field", 1, unmappedFields.size());
        assertTrue("Should contain unmapped field name", unmappedFields.contains("Test Name"));
    }

    @Test
    public void testValidateTypeCompatibility_WithCompatibleTypes_ReturnsNoWarnings() {
        // Arrange
        List<AnalyzerFieldMapping> mappings = Arrays.asList(activeMapping);

        // Act
        List<String> warnings = mappingValidationService.validateTypeCompatibility(mappings);

        // Assert
        assertNotNull("Warnings list should not be null", warnings);
        assertEquals("Should have no warnings for compatible types", 0, warnings.size());
    }

    @Test
    public void testGetValidationMetrics_ReturnsCompleteMetrics() {
        // Arrange
        List<AnalyzerField> allFields = Arrays.asList(numericField, textField);
        List<AnalyzerFieldMapping> activeMappings = Arrays.asList(activeMapping);

        when(analyzerFieldDAO.findByAnalyzerId("ANALYZER-001")).thenReturn(allFields);
        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("ANALYZER-001")).thenReturn(activeMappings);

        // Act
        MappingValidationService.ValidationMetrics metrics = mappingValidationService
                .getValidationMetrics("ANALYZER-001");

        // Assert
        assertNotNull("Metrics should not be null", metrics);
        assertEquals("Accuracy should be 0.5", 0.5, metrics.getAccuracy(), 0.01);
        assertEquals("Unmapped count should be 1", 1, metrics.getUnmappedCount());
        assertNotNull("Unmapped fields should not be null", metrics.getUnmappedFields());
        assertNotNull("Warnings should not be null", metrics.getWarnings());
        assertNotNull("Coverage by test unit should not be null", metrics.getCoverageByTestUnit());
    }
}
