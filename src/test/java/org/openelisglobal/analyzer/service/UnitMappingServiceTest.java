package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.UnitMappingDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.UnitMapping;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Unit tests for UnitMappingService implementation
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class UnitMappingServiceTest {

    @Mock
    private UnitMappingDAO unitMappingDAO;

    private UnitMappingServiceImpl unitMappingService;

    private Analyzer testAnalyzer;
    private AnalyzerField numericField;
    private UnitMapping testMapping;

    @Before
    public void setUp() {
        unitMappingService = new UnitMappingServiceImpl(unitMappingDAO);

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

        // Setup test mapping
        testMapping = new UnitMapping();
        testMapping.setId("UNIT-MAPPING-001");
        testMapping.setAnalyzerField(numericField);
        testMapping.setAnalyzerUnit("mg/dL");
        testMapping.setOpenelisUnit("mg/dL");
        testMapping.setConversionFactor(null);
        testMapping.setRejectIfMismatch(false);
    }

    /**
     * Test: Create mapping with conversion factor applies conversion
     * 
     * Conversion factor: Used when analyzer unit differs from OpenELIS unit
     */
    @Test
    public void testCreateMapping_WithConversionFactor_AppliesConversion() {
        // Arrange: Different units with conversion factor
        UnitMapping mapping = new UnitMapping();
        mapping.setAnalyzerField(numericField);
        mapping.setAnalyzerUnit("mmol/L");
        mapping.setOpenelisUnit("mg/dL");
        mapping.setConversionFactor(new BigDecimal("18.0182")); // mmol/L to mg/dL conversion
        mapping.setRejectIfMismatch(false);

        when(unitMappingDAO.insert(mapping)).thenReturn("UNIT-MAPPING-001");

        // Act
        String id = unitMappingService.createMapping(mapping);

        // Assert
        assertNotNull("ID should not be null", id);
        assertEquals("ID should match", "UNIT-MAPPING-001", id);
    }

    /**
     * Test: Create mapping with unit mismatch requires conversion factor
     * 
     * Validation: If units don't match, conversion factor is required
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testCreateMapping_WithUnitMismatch_RequiresConversionFactor() {
        // Arrange: Different units but no conversion factor (invalid)
        UnitMapping mapping = new UnitMapping();
        mapping.setAnalyzerField(numericField);
        mapping.setAnalyzerUnit("mmol/L");
        mapping.setOpenelisUnit("mg/dL"); // Different unit
        mapping.setConversionFactor(null); // Missing conversion factor - should throw exception
        mapping.setRejectIfMismatch(false);

        // Act: Should throw LIMSRuntimeException
        unitMappingService.createMapping(mapping);
    }

    /**
     * Test: Create mapping with matching units doesn't require conversion factor
     */
    @Test
    public void testCreateMapping_WithMatchingUnits_DoesNotRequireConversionFactor() {
        // Arrange: Same units, no conversion factor needed
        UnitMapping mapping = new UnitMapping();
        mapping.setAnalyzerField(numericField);
        mapping.setAnalyzerUnit("mg/dL");
        mapping.setOpenelisUnit("mg/dL"); // Same unit
        mapping.setConversionFactor(null); // No conversion factor needed
        mapping.setRejectIfMismatch(false);

        when(unitMappingDAO.insert(mapping)).thenReturn("UNIT-MAPPING-002");

        // Act
        String id = unitMappingService.createMapping(mapping);

        // Assert
        assertNotNull("ID should not be null", id);
        assertEquals("ID should match", "UNIT-MAPPING-002", id);
    }
}
