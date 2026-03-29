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
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Unit tests for AnalyzerFieldService implementation
 * 
 * References: - Testing Roadmap: .specify/guides/testing-roadmap.md - Template:
 * JUnit 4 Service Test
 * 
 * TDD Workflow (MANDATORY for complex logic): - RED: Write failing test first
 * (defines expected behavior) - GREEN: Write minimal code to make test pass -
 * REFACTOR: Improve code quality while keeping tests green
 * 
 * 
 * Test Naming: test{MethodName}_{Scenario}_{ExpectedResult}
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerFieldServiceTest {

    @Mock
    private AnalyzerFieldDAO analyzerFieldDAO;

    private AnalyzerFieldServiceImpl analyzerFieldService;

    private Analyzer testAnalyzer;
    private AnalyzerField testField;

    @Before
    public void setUp() {
        analyzerFieldService = new AnalyzerFieldServiceImpl(analyzerFieldDAO);

        // Setup test analyzer
        testAnalyzer = new Analyzer();
        testAnalyzer.setId("1");
        testAnalyzer.setName("Test Analyzer");

        // Setup test field
        testField = new AnalyzerField();
        testField.setId("FIELD-001");
        testField.setAnalyzer(testAnalyzer);
        testField.setFieldName("GLUCOSE");
        testField.setFieldType(AnalyzerField.FieldType.NUMERIC);
        testField.setUnit("mg/dL");
        testField.setIsActive(true);
    }

    /**
     * Test: Get fields by analyzer ID returns list of fields
     */
    @Test
    public void testGetFieldsByAnalyzerId_ReturnsFields() {
        // Arrange
        List<AnalyzerField> expectedFields = new ArrayList<>();
        expectedFields.add(testField);

        AnalyzerField field2 = new AnalyzerField();
        field2.setId("FIELD-002");
        field2.setAnalyzer(testAnalyzer);
        field2.setFieldName("CHOLESTEROL");
        field2.setFieldType(AnalyzerField.FieldType.NUMERIC);
        field2.setUnit("mg/dL");
        field2.setIsActive(true);
        expectedFields.add(field2);

        when(analyzerFieldDAO.findByAnalyzerId("1")).thenReturn(expectedFields);

        // Act
        List<AnalyzerField> result = analyzerFieldService.getFieldsByAnalyzerId("1");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return 2 fields", 2, result.size());
        assertEquals("First field name should match", "GLUCOSE", result.get(0).getFieldName());
        assertEquals("Second field name should match", "CHOLESTEROL", result.get(1).getFieldName());
    }

    /**
     * Test: Create field with valid data persists field
     */
    @Test
    public void testCreateField_WithValidData_PersistsField() {
        // Arrange
        AnalyzerField newField = new AnalyzerField();
        newField.setAnalyzer(testAnalyzer);
        newField.setFieldName("HEMOGLOBIN");
        newField.setFieldType(AnalyzerField.FieldType.NUMERIC);
        newField.setUnit("g/dL");
        newField.setIsActive(true);

        when(analyzerFieldDAO.insert(newField)).thenReturn("FIELD-003");

        // Act
        String id = analyzerFieldService.createField(newField);

        // Assert
        assertNotNull("ID should not be null", id);
        assertEquals("ID should match", "FIELD-003", id);
    }

    /**
     * Test: Create field with invalid field type throws exception
     * 
     * Validation: NUMERIC fields must have unit, non-NUMERIC fields must not have
     * unit
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testCreateField_WithInvalidFieldType_ThrowsException() {
        // Arrange: NUMERIC field without unit (invalid)
        AnalyzerField invalidField = new AnalyzerField();
        invalidField.setAnalyzer(testAnalyzer);
        invalidField.setFieldName("INVALID");
        invalidField.setFieldType(AnalyzerField.FieldType.NUMERIC);
        // Missing unit - should throw exception
        invalidField.setUnit(null);

        // Act: Should throw LIMSRuntimeException
        analyzerFieldService.createField(invalidField);
    }

    /**
     * Test: Create field with QUALITATIVE type and unit throws exception
     * 
     * Validation: QUALITATIVE fields must not have unit
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testCreateField_WithQualitativeTypeAndUnit_ThrowsException() {
        // Arrange: QUALITATIVE field with unit (invalid)
        AnalyzerField invalidField = new AnalyzerField();
        invalidField.setAnalyzer(testAnalyzer);
        invalidField.setFieldName("HIV_TEST");
        invalidField.setFieldType(AnalyzerField.FieldType.QUALITATIVE);
        invalidField.setUnit("mg/dL"); // Should not have unit for QUALITATIVE

        // Act: Should throw LIMSRuntimeException
        analyzerFieldService.createField(invalidField);
    }

    /**
     * Test: Get fields by analyzer ID with empty result returns empty list
     */
    @Test
    public void testGetFieldsByAnalyzerId_WithNoFields_ReturnsEmptyList() {
        // Arrange
        when(analyzerFieldDAO.findByAnalyzerId("999")).thenReturn(new ArrayList<>());

        // Act
        List<AnalyzerField> result = analyzerFieldService.getFieldsByAnalyzerId("999");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return empty list", 0, result.size());
    }
}
