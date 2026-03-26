package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;

/**
 * Unit tests for MappingApplicationService implementation
 * 
 * 
 * Test Coverage Goal: >80%
 */
@RunWith(MockitoJUnitRunner.class)
public class MappingApplicationServiceTest {

    @Mock
    private AnalyzerFieldMappingDAO analyzerFieldMappingDAO;

    @Mock
    private AnalyzerFieldDAO analyzerFieldDAO;

    private MappingApplicationServiceImpl mappingApplicationService;

    private Analyzer testAnalyzer;
    private AnalyzerField testField;
    private AnalyzerFieldMapping testMapping;

    @Before
    public void setUp() {
        mappingApplicationService = new MappingApplicationServiceImpl(analyzerFieldMappingDAO, analyzerFieldDAO);

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

        // Setup test mapping
        testMapping = new AnalyzerFieldMapping();
        testMapping.setId("MAPPING-001");
        testMapping.setAnalyzerField(testField);
        testMapping.setOpenelisFieldId("TEST-001");
        testMapping.setOpenelisFieldType(AnalyzerFieldMapping.OpenELISFieldType.TEST);
        testMapping.setMappingType(AnalyzerFieldMapping.MappingType.TEST_LEVEL);
        testMapping.setIsActive(true);
    }

    /**
     * Test: Apply mappings with valid mappings returns transformed lines
     */
    @Test
    public void testApplyMappings_WithValidMappings_ReturnsTransformedLines() {
        // Arrange
        String analyzerId = "1";
        List<String> lines = new ArrayList<>();
        lines.add("H|\\^&|||PSM^Micro^2.0|...");
        lines.add("P|1||PATIENT-001||DOE^JOHN||19800101|M");
        lines.add("O|1|SAMPLE-001|||R");
        lines.add("R|1|^^^GLUCOSE^GLU||100|mg/dL||N|||20250127");

        List<AnalyzerFieldMapping> mappings = new ArrayList<>();
        mappings.add(testMapping);

        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId)).thenReturn(mappings);

        // Act
        MappingApplicationResult result = mappingApplicationService.applyMappings(analyzerId, lines);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Should have mappings", result.hasMappings());
        assertNotNull("Transformed lines should not be null", result.getTransformedLines());
    }

    /**
     * Test: Apply mappings with no mappings returns empty result
     */
    @Test
    public void testApplyMappings_WithNoMappings_ReturnsNoMappings() {
        // Arrange
        String analyzerId = "1";
        List<String> lines = new ArrayList<>();
        lines.add("H|\\^&|||PSM^Micro^2.0|...");

        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId)).thenReturn(new ArrayList<>());

        // Act
        MappingApplicationResult result = mappingApplicationService.applyMappings(analyzerId, lines);

        // Assert
        assertNotNull("Result should not be null", result);
        assertFalse("Should not have mappings", result.hasMappings());
    }

    /**
     * Test: Check has active mappings returns true when mappings exist
     */
    @Test
    public void testHasActiveMappings_WithMappings_ReturnsTrue() {
        // Arrange
        String analyzerId = "1";
        List<AnalyzerFieldMapping> mappings = new ArrayList<>();
        mappings.add(testMapping);

        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId)).thenReturn(mappings);

        // Act
        boolean hasMappings = mappingApplicationService.hasActiveMappings(analyzerId);

        // Assert
        assertTrue("Should have active mappings", hasMappings);
    }

    /**
     * Test: Check has active mappings returns false when no mappings exist
     */
    @Test
    public void testHasActiveMappings_WithNoMappings_ReturnsFalse() {
        // Arrange
        String analyzerId = "1";

        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId)).thenReturn(new ArrayList<>());

        // Act
        boolean hasMappings = mappingApplicationService.hasActiveMappings(analyzerId);

        // Assert
        assertFalse("Should not have active mappings", hasMappings);
    }

    /**
     * Test: Apply mappings with unmapped fields adds to unmappedFields list
     */
    @Test
    public void testApplyMappings_WithUnmappedFields_AddsToUnmappedFields() {
        // Arrange
        String analyzerId = "1";
        List<String> lines = new ArrayList<>();
        lines.add("H|\\^&|||PSM^Micro^2.0|...");
        lines.add("R|1|^^^UNMAPPED_TEST||100|mg/dL||N|||20250127");

        List<AnalyzerFieldMapping> mappings = new ArrayList<>();
        mappings.add(testMapping); // Only GLUCOSE mapped, UNMAPPED_TEST not mapped

        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId)).thenReturn(mappings);

        // Act
        MappingApplicationResult result = mappingApplicationService.applyMappings(analyzerId, lines);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Should have unmapped fields", result.getUnmappedFields().size() > 0);
    }
}
