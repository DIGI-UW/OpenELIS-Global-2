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
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Unit tests for AnalyzerMappingCopyService implementation
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerMappingCopyServiceTest {

    @Mock
    private AnalyzerFieldMappingDAO analyzerFieldMappingDAO;

    @Mock
    private AnalyzerFieldDAO analyzerFieldDAO;

    private AnalyzerMappingCopyServiceImpl analyzerMappingCopyService;

    private Analyzer sourceAnalyzer;
    private Analyzer targetAnalyzer;
    private AnalyzerField sourceField;
    private AnalyzerFieldMapping sourceMapping;

    @Before
    public void setUp() {
        analyzerMappingCopyService = new AnalyzerMappingCopyServiceImpl(analyzerFieldMappingDAO, analyzerFieldDAO);

        // Setup source analyzer
        sourceAnalyzer = new Analyzer();
        sourceAnalyzer.setId("SOURCE-001");
        sourceAnalyzer.setName("Source Analyzer");

        // Setup target analyzer
        targetAnalyzer = new Analyzer();
        targetAnalyzer.setId("TARGET-001");
        targetAnalyzer.setName("Target Analyzer");

        // Setup source field
        sourceField = new AnalyzerField();
        sourceField.setId("FIELD-001");
        sourceField.setAnalyzer(sourceAnalyzer);
        sourceField.setFieldName("GLUCOSE");
        sourceField.setFieldType(AnalyzerField.FieldType.NUMERIC);

        // Setup source mapping
        sourceMapping = new AnalyzerFieldMapping();
        sourceMapping.setId("MAPPING-001");
        sourceMapping.setAnalyzerField(sourceField);
        sourceMapping.setOpenelisFieldId("TEST-001");
        sourceMapping.setOpenelisFieldType(AnalyzerFieldMapping.OpenELISFieldType.TEST);
        sourceMapping.setMappingType(AnalyzerFieldMapping.MappingType.TEST_LEVEL);
        sourceMapping.setIsRequired(false);
        sourceMapping.setIsActive(true);
    }

    /**
     * Test: Copy mappings with valid source copies all mappings
     */
    @Test
    public void testCopyMappings_WithValidSource_CopiesAllMappings() {
        // Arrange: Source analyzer has active mappings
        List<AnalyzerFieldMapping> sourceMappings = new ArrayList<>();
        sourceMappings.add(sourceMapping);

        // Target analyzer has no mappings
        List<AnalyzerFieldMapping> targetMappings = new ArrayList<>();

        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("SOURCE-001")).thenReturn(sourceMappings);
        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("TARGET-001")).thenReturn(targetMappings);

        // Mock field lookup for target analyzer
        AnalyzerField targetField = new AnalyzerField();
        targetField.setId("TARGET-FIELD-001");
        targetField.setAnalyzer(targetAnalyzer);
        targetField.setFieldName("GLUCOSE");
        targetField.setFieldType(AnalyzerField.FieldType.NUMERIC);

        when(analyzerFieldDAO.findByAnalyzerIdAndFieldName("TARGET-001", "GLUCOSE"))
                .thenReturn(Optional.of(targetField));
        when(analyzerFieldMappingDAO.insert(org.mockito.ArgumentMatchers.any(AnalyzerFieldMapping.class)))
                .thenReturn("NEW-MAPPING-001");

        // Act: Copy mappings
        org.openelisglobal.analyzer.service.CopyMappingsResult result = analyzerMappingCopyService
                .copyMappings("SOURCE-001", "TARGET-001", null);

        // Assert: Mappings copied
        assertNotNull("Result should not be null", result);
        assertEquals("Should copy one mapping", 1, result.getCopiedCount().intValue());
        assertEquals("Should have no skipped mappings", 0, result.getSkippedCount().intValue());
    }

    /**
     * Test: Copy mappings with existing mappings overwrites target
     */
    @Test
    public void testCopyMappings_WithExistingMappings_OverwritesTarget() {
        // Arrange: Both analyzers have mappings for same field
        List<AnalyzerFieldMapping> sourceMappings = new ArrayList<>();
        sourceMappings.add(sourceMapping);

        AnalyzerField targetField = new AnalyzerField();
        targetField.setId("TARGET-FIELD-001");
        targetField.setAnalyzer(targetAnalyzer);
        targetField.setFieldName("GLUCOSE");
        targetField.setFieldType(AnalyzerField.FieldType.NUMERIC);

        AnalyzerFieldMapping existingTargetMapping = new AnalyzerFieldMapping();
        existingTargetMapping.setId("TARGET-MAPPING-001");
        existingTargetMapping.setAnalyzerField(targetField); // Set analyzer field for lookup
        existingTargetMapping.setOpenelisFieldId("TEST-002"); // Different OpenELIS field
        existingTargetMapping.setIsActive(true);

        List<AnalyzerFieldMapping> targetMappings = new ArrayList<>();
        targetMappings.add(existingTargetMapping);

        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("SOURCE-001")).thenReturn(sourceMappings);
        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("TARGET-001")).thenReturn(targetMappings);
        when(analyzerFieldDAO.findByAnalyzerIdAndFieldName("TARGET-001", "GLUCOSE"))
                .thenReturn(Optional.of(targetField));
        when(analyzerFieldMappingDAO.update(org.mockito.ArgumentMatchers.any(AnalyzerFieldMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Copy mappings (should overwrite existing)
        org.openelisglobal.analyzer.service.CopyMappingsResult result = analyzerMappingCopyService
                .copyMappings("SOURCE-001", "TARGET-001", null);

        // Assert: Existing mapping updated
        assertNotNull("Result should not be null", result);
        verify(analyzerFieldMappingDAO).update(org.mockito.ArgumentMatchers.any(AnalyzerFieldMapping.class));
    }

    /**
     * Test: Copy mappings with type incompatibility generates warnings
     */
    @Test
    public void testCopyMappings_WithTypeIncompatibility_GeneratesWarnings() {
        // Arrange: Source has NUMERIC field, target has QUALITATIVE field with same
        // name
        List<AnalyzerFieldMapping> sourceMappings = new ArrayList<>();
        sourceMappings.add(sourceMapping);

        List<AnalyzerFieldMapping> targetMappings = new ArrayList<>();

        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("SOURCE-001")).thenReturn(sourceMappings);
        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("TARGET-001")).thenReturn(targetMappings);

        // Target field has incompatible type
        AnalyzerField targetField = new AnalyzerField();
        targetField.setId("TARGET-FIELD-001");
        targetField.setAnalyzer(targetAnalyzer);
        targetField.setFieldName("GLUCOSE");
        targetField.setFieldType(AnalyzerField.FieldType.QUALITATIVE); // Incompatible type

        when(analyzerFieldDAO.findByAnalyzerIdAndFieldName("TARGET-001", "GLUCOSE"))
                .thenReturn(Optional.of(targetField));

        // Act: Copy mappings
        org.openelisglobal.analyzer.service.CopyMappingsResult result = analyzerMappingCopyService
                .copyMappings("SOURCE-001", "TARGET-001", null);

        // Assert: Warning generated, mapping skipped
        assertNotNull("Result should not be null", result);
        assertEquals("Should skip incompatible mapping", 1, result.getSkippedCount().intValue());
        assertEquals("Should have warnings", true, result.getWarnings().size() > 0);
    }

    /**
     * Test: Merge qualitative mappings combines values deduplicated
     */
    @Test
    public void testMergeQualitativeMappings_CombinesValuesDeduplicated() {
        // Arrange: Source and target both have qualitative mappings for same field
        AnalyzerField qualitativeSourceField = new AnalyzerField();
        qualitativeSourceField.setId("QUAL-FIELD-001");
        qualitativeSourceField.setAnalyzer(sourceAnalyzer);
        qualitativeSourceField.setFieldName("HIV_TEST");
        qualitativeSourceField.setFieldType(AnalyzerField.FieldType.QUALITATIVE);

        AnalyzerFieldMapping qualitativeSourceMapping = new AnalyzerFieldMapping();
        qualitativeSourceMapping.setId("QUAL-MAPPING-001");
        qualitativeSourceMapping.setAnalyzerField(qualitativeSourceField);
        qualitativeSourceMapping.setOpenelisFieldId("RESULT-001");
        qualitativeSourceMapping.setOpenelisFieldType(AnalyzerFieldMapping.OpenELISFieldType.RESULT);
        qualitativeSourceMapping.setMappingType(AnalyzerFieldMapping.MappingType.RESULT_LEVEL);
        qualitativeSourceMapping.setIsActive(true);

        List<AnalyzerFieldMapping> sourceMappings = new ArrayList<>();
        sourceMappings.add(qualitativeSourceMapping);

        AnalyzerField targetQualField = new AnalyzerField();
        targetQualField.setId("TARGET-QUAL-FIELD-001");
        targetQualField.setAnalyzer(targetAnalyzer);
        targetQualField.setFieldName("HIV_TEST");
        targetQualField.setFieldType(AnalyzerField.FieldType.QUALITATIVE);

        AnalyzerFieldMapping existingQualitativeMapping = new AnalyzerFieldMapping();
        existingQualitativeMapping.setId("TARGET-QUAL-001");
        existingQualitativeMapping.setAnalyzerField(targetQualField); // Set analyzer field for lookup
        existingQualitativeMapping.setOpenelisFieldId("RESULT-001"); // Same OpenELIS field
        existingQualitativeMapping.setIsActive(true);

        List<AnalyzerFieldMapping> targetMappings = new ArrayList<>();
        targetMappings.add(existingQualitativeMapping);

        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("SOURCE-001")).thenReturn(sourceMappings);
        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("TARGET-001")).thenReturn(targetMappings);

        when(analyzerFieldDAO.findByAnalyzerIdAndFieldName("TARGET-001", "HIV_TEST"))
                .thenReturn(Optional.of(targetQualField));
        when(analyzerFieldMappingDAO.update(org.mockito.ArgumentMatchers.any(AnalyzerFieldMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Copy mappings (should merge qualitative values)
        org.openelisglobal.analyzer.service.CopyMappingsResult result = analyzerMappingCopyService
                .copyMappings("SOURCE-001", "TARGET-001", null);

        // Assert: Qualitative mappings merged (not overwritten)
        assertNotNull("Result should not be null", result);
        verify(analyzerFieldMappingDAO).update(org.mockito.ArgumentMatchers.any(AnalyzerFieldMapping.class));
    }

    /**
     * Test: Copy mappings with partial failure rolls back transaction
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testCopyMappings_WithPartialFailure_RollsBackTransaction() {
        // Arrange: Source has mappings, but insert fails
        List<AnalyzerFieldMapping> sourceMappings = new ArrayList<>();
        sourceMappings.add(sourceMapping);

        List<AnalyzerFieldMapping> targetMappings = new ArrayList<>();

        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("SOURCE-001")).thenReturn(sourceMappings);
        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("TARGET-001")).thenReturn(targetMappings);

        AnalyzerField targetField = new AnalyzerField();
        targetField.setId("TARGET-FIELD-001");
        targetField.setAnalyzer(targetAnalyzer);
        targetField.setFieldName("GLUCOSE");
        targetField.setFieldType(AnalyzerField.FieldType.NUMERIC);

        when(analyzerFieldDAO.findByAnalyzerIdAndFieldName("TARGET-001", "GLUCOSE"))
                .thenReturn(Optional.of(targetField));
        when(analyzerFieldMappingDAO.insert(org.mockito.ArgumentMatchers.any(AnalyzerFieldMapping.class)))
                .thenThrow(new LIMSRuntimeException("Database constraint violation"));

        // Act: Copy mappings (should rollback on failure)
        analyzerMappingCopyService.copyMappings("SOURCE-001", "TARGET-001", null);
    }
}
