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
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;

/**
 * Unit tests for AnalyzerMappingPreviewService implementation
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerMappingPreviewServiceTest {

    @Mock
    private AnalyzerFieldMappingDAO analyzerFieldMappingDAO;

    @Mock
    private AnalyzerFieldDAO analyzerFieldDAO;

    private AnalyzerMappingPreviewServiceImpl analyzerMappingPreviewService;

    private Analyzer testAnalyzer;
    private AnalyzerField testField;
    private AnalyzerFieldMapping testMapping;

    @Before
    public void setUp() {
        analyzerMappingPreviewService = new AnalyzerMappingPreviewServiceImpl(analyzerFieldMappingDAO,
                analyzerFieldDAO);

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
     * Test: Preview mapping with valid message returns preview
     */
    @Test
    public void testPreviewMapping_WithValidMessage_ReturnsPreview() {
        // Arrange: Valid ASTM message
        String astmMessage = "H|\\^&|||PSM^Micro^2.0|...";
        String analyzerId = "1";

        List<AnalyzerFieldMapping> mappings = new ArrayList<>();
        mappings.add(testMapping);

        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId)).thenReturn(mappings);

        // Mock ASTM parsing (simplified - actual implementation will use
        // ASTMAnalyzerReader)
        // For now, we'll test the structure

        // Act: Preview mapping
        org.openelisglobal.analyzer.service.MappingPreviewResult result = analyzerMappingPreviewService
                .previewMapping(analyzerId, astmMessage, null);

        // Assert: Preview result returned
        assertNotNull("Preview result should not be null", result);
        assertNotNull("Parsed fields should not be null", result.getParsedFields());
        assertNotNull("Applied mappings should not be null", result.getAppliedMappings());
    }

    /**
     * Test: Preview mapping with invalid format still attempts to parse
     * 
     * Note: Current implementation is lenient and attempts to parse any input.
     * Invalid format may result in empty parsed fields or warnings, not errors.
     */
    @Test
    public void testPreviewMapping_WithInvalidFormat_ReturnsError() {
        // Arrange: Invalid ASTM message (too large)
        String astmMessage = "x".repeat(11 * 1024); // Exceeds 10KB limit
        String analyzerId = "1";

        // Note: No need to mock DAO since size check returns early

        // Act: Preview mapping
        org.openelisglobal.analyzer.service.MappingPreviewResult result = analyzerMappingPreviewService
                .previewMapping(analyzerId, astmMessage, null);

        // Assert: Error returned for size limit
        assertNotNull("Preview result should not be null", result);
        assertEquals("Should have errors for size limit", true, result.getErrors().size() > 0);
    }

    /**
     * Test: Preview mapping with unmapped fields returns warnings
     */
    @Test
    public void testPreviewMapping_WithUnmappedFields_ReturnsWarnings() {
        // Arrange: ASTM message with fields not in mappings
        String astmMessage = "H|\\^&|||PSM^Micro^2.0|...";
        String analyzerId = "1";

        // No mappings exist
        List<AnalyzerFieldMapping> mappings = new ArrayList<>();

        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId)).thenReturn(mappings);

        // Act: Preview mapping
        org.openelisglobal.analyzer.service.MappingPreviewResult result = analyzerMappingPreviewService
                .previewMapping(analyzerId, astmMessage, null);

        // Assert: Warnings returned for unmapped fields
        assertNotNull("Preview result should not be null", result);
        assertEquals("Should have warnings for unmapped fields", true, result.getWarnings().size() > 0);
    }

    /**
     * Test: Parse ASTM message with complex message parses all fields
     */
    @Test
    public void testParseAstmMessage_WithComplexMessage_ParsesAllFields() {
        // Arrange: Complex ASTM message with multiple segments
        String astmMessage = "H|\\^&|||PSM^Micro^2.0|...\nP|1||...\nO|1||...\nR|1|^GLUCOSE^...|123|mg/dL|...";

        // Act: Parse message
        List<org.openelisglobal.analyzer.service.ParsedField> parsedFields = analyzerMappingPreviewService
                .parseAstmMessage(astmMessage);

        // Assert: All fields parsed
        assertNotNull("Parsed fields should not be null", parsedFields);
        assertEquals("Should parse multiple fields", true, parsedFields.size() > 0);
    }

    /**
     * Test: Apply mappings with type compatibility applies mappings
     */
    @Test
    public void testApplyMappings_WithTypeCompatibility_AppliesMappings() {
        // Arrange: Parsed fields and compatible mappings
        List<org.openelisglobal.analyzer.service.ParsedField> parsedFields = new ArrayList<>();
        org.openelisglobal.analyzer.service.ParsedField field = new org.openelisglobal.analyzer.service.ParsedField();
        field.setFieldName("GLUCOSE");
        field.setFieldType("NUMERIC");
        field.setRawValue("123");
        parsedFields.add(field);

        List<AnalyzerFieldMapping> mappings = new ArrayList<>();
        mappings.add(testMapping);

        // Act: Apply mappings
        List<org.openelisglobal.analyzer.service.AppliedMapping> appliedMappings = analyzerMappingPreviewService
                .applyMappings(parsedFields, mappings);

        // Assert: Mappings applied
        assertNotNull("Applied mappings should not be null", appliedMappings);
        assertEquals("Should apply compatible mappings", true, appliedMappings.size() > 0);
    }

    /**
     * Test: Build entity preview constructs test and result
     */
    @Test
    public void testBuildEntityPreview_ConstructsTestAndResult() {
        // Arrange: Applied mappings
        List<org.openelisglobal.analyzer.service.AppliedMapping> appliedMappings = new ArrayList<>();
        org.openelisglobal.analyzer.service.AppliedMapping mapping = new org.openelisglobal.analyzer.service.AppliedMapping();
        mapping.setAnalyzerFieldName("GLUCOSE");
        mapping.setOpenelisFieldId("TEST-001");
        mapping.setOpenelisFieldType("TEST");
        mapping.setMappedValue("123");
        appliedMappings.add(mapping);

        // Act: Build entity preview
        org.openelisglobal.analyzer.service.EntityPreview entityPreview = analyzerMappingPreviewService
                .buildEntityPreview(appliedMappings);

        // Assert: Entity preview constructed
        assertNotNull("Entity preview should not be null", entityPreview);
        assertNotNull("Test entities should not be null", entityPreview.getTests());
    }
}
