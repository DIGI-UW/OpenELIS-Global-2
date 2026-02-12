package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service implementation for analyzer mapping preview operations
 * 
 * 
 * Provides stateless preview operations for testing field mappings with sample
 * ASTM messages. NO @Transactional - read-only stateless operations.
 */
@Service
public class AnalyzerMappingPreviewServiceImpl implements AnalyzerMappingPreviewService {

    private static final int MAX_MESSAGE_SIZE = 10 * 1024; // 10KB

    private final AnalyzerFieldMappingDAO analyzerFieldMappingDAO;
    private final AnalyzerFieldDAO analyzerFieldDAO;

    @Autowired
    public AnalyzerMappingPreviewServiceImpl(AnalyzerFieldMappingDAO analyzerFieldMappingDAO,
            AnalyzerFieldDAO analyzerFieldDAO) {
        this.analyzerFieldMappingDAO = analyzerFieldMappingDAO;
        this.analyzerFieldDAO = analyzerFieldDAO;
    }

    @Override
    public MappingPreviewResult previewMapping(String analyzerId, String astmMessage, PreviewOptions options) {
        MappingPreviewResult result = new MappingPreviewResult();

        if (astmMessage == null || astmMessage.length() > MAX_MESSAGE_SIZE) {
            result.getErrors().add("ASTM message exceeds maximum size of 10KB");
            return result;
        }

        if (options == null) {
            options = new PreviewOptions();
        }

        try {
            List<ParsedField> parsedFields = parseAstmMessage(astmMessage);
            result.setParsedFields(parsedFields);

            List<AnalyzerFieldMapping> mappings = analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId);

            List<AppliedMapping> appliedMappings = applyMappings(parsedFields, mappings);
            result.setAppliedMappings(appliedMappings);

            EntityPreview entityPreview = buildEntityPreview(appliedMappings);
            result.setEntityPreview(entityPreview);

            validateMappings(parsedFields, mappings, result);

        } catch (Exception e) {
            result.getErrors().add("Error processing ASTM message: " + e.getMessage());
        }

        return result;
    }

    @Override
    public List<ParsedField> parseAstmMessage(String astmMessage) {
        List<ParsedField> parsedFields = new ArrayList<>();

        if (astmMessage == null || astmMessage.trim().isEmpty()) {
            return parsedFields;
        }

        String[] lines = astmMessage.split("[\r\n]+");

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            // ASTM record format: Segment|Field1^Field2^Field3|Field4|...
            String[] segments = line.split("\\|");
            if (segments.length < 2) {
                continue;
            }

            String segmentType = segments[0].trim();
            if (segmentType.length() == 0) {
                continue;
            }

            // Simplified parser - actual implementation would use
            // ASTMAnalyzerReader or plugin-based parsing
            for (int i = 1; i < segments.length; i++) {
                String fieldValue = segments[i];
                if (fieldValue != null && !fieldValue.trim().isEmpty()) {
                    ParsedField field = new ParsedField();
                    field.setFieldName(segmentType + "_" + i);
                    field.setAstmRef(segmentType + "|" + i);
                    field.setRawValue(fieldValue);
                    if (segmentType.equals("R")) {
                        field.setFieldType("NUMERIC");
                    } else if (segmentType.equals("P")) {
                        field.setFieldType("TEXT");
                    } else {
                        field.setFieldType("TEXT");
                    }
                    parsedFields.add(field);
                }
            }
        }

        return parsedFields;
    }

    @Override
    public List<AppliedMapping> applyMappings(List<ParsedField> parsedFields, List<AnalyzerFieldMapping> mappings) {
        List<AppliedMapping> appliedMappings = new ArrayList<>();

        Map<String, AnalyzerFieldMapping> mappingsByFieldName = new HashMap<>();
        for (AnalyzerFieldMapping mapping : mappings) {
            AnalyzerField field = mapping.getAnalyzerField();
            if (field != null && field.getFieldName() != null) {
                mappingsByFieldName.put(field.getFieldName(), mapping);
            }
        }

        for (ParsedField parsedField : parsedFields) {
            AnalyzerFieldMapping mapping = mappingsByFieldName.get(parsedField.getFieldName());
            if (mapping != null) {
                AppliedMapping applied = new AppliedMapping();
                applied.setAnalyzerFieldName(parsedField.getFieldName());
                applied.setOpenelisFieldId(mapping.getOpenelisFieldId());
                applied.setOpenelisFieldType(mapping.getOpenelisFieldType().toString());
                applied.setMappedValue(parsedField.getRawValue());
                applied.setMappingId(mapping.getId());
                appliedMappings.add(applied);
            }
        }

        return appliedMappings;
    }

    @Override
    public EntityPreview buildEntityPreview(List<AppliedMapping> appliedMappings) {
        EntityPreview preview = new EntityPreview();

        Map<String, List<AppliedMapping>> mappingsByType = appliedMappings.stream()
                .collect(Collectors.groupingBy(AppliedMapping::getOpenelisFieldType));

        // Build Test entities
        List<AppliedMapping> testMappings = mappingsByType.getOrDefault("TEST", new ArrayList<>());
        for (AppliedMapping mapping : testMappings) {
            Map<String, Object> test = new HashMap<>();
            test.put("id", mapping.getOpenelisFieldId());
            test.put("name", mapping.getAnalyzerFieldName());
            preview.getTests().add(test);
        }

        // Build Result entities
        List<AppliedMapping> resultMappings = mappingsByType.getOrDefault("RESULT", new ArrayList<>());
        for (AppliedMapping mapping : resultMappings) {
            Map<String, Object> result = new HashMap<>();
            result.put("testId", mapping.getOpenelisFieldId());
            result.put("value", mapping.getMappedValue());
            result.put("fieldName", mapping.getAnalyzerFieldName());
            preview.getResults().add(result);
        }

        // Build Sample entity
        List<AppliedMapping> sampleMappings = appliedMappings.stream()
                .filter(m -> m.getOpenelisFieldType().equals("SAMPLE") || m.getOpenelisFieldType().equals("ORDER"))
                .collect(Collectors.toList());
        for (AppliedMapping mapping : sampleMappings) {
            preview.getSample().put(mapping.getAnalyzerFieldName(), mapping.getMappedValue());
        }

        return preview;
    }

    /**
     * Validate mappings and generate warnings
     */
    private void validateMappings(List<ParsedField> parsedFields, List<AnalyzerFieldMapping> mappings,
            MappingPreviewResult result) {
        // Check for unmapped fields
        Map<String, AnalyzerFieldMapping> mappingsByFieldName = new HashMap<>();
        for (AnalyzerFieldMapping mapping : mappings) {
            AnalyzerField field = mapping.getAnalyzerField();
            if (field != null && field.getFieldName() != null) {
                mappingsByFieldName.put(field.getFieldName(), mapping);
            }
        }

        for (ParsedField parsedField : parsedFields) {
            if (!mappingsByFieldName.containsKey(parsedField.getFieldName())) {
                result.getWarnings()
                        .add("Field '" + parsedField.getFieldName() + "' is not mapped to any OpenELIS field");
            }
        }

        // Check for required mappings
        boolean hasSampleIdMapping = mappings.stream().anyMatch(m -> m.getIsRequired() != null && m.getIsRequired()
                && m.getOpenelisFieldType() == AnalyzerFieldMapping.OpenELISFieldType.SAMPLE);
        boolean hasTestCodeMapping = mappings.stream().anyMatch(m -> m.getIsRequired() != null && m.getIsRequired()
                && m.getMappingType() == AnalyzerFieldMapping.MappingType.TEST_LEVEL);
        boolean hasResultValueMapping = mappings.stream().anyMatch(m -> m.getIsRequired() != null && m.getIsRequired()
                && m.getMappingType() == AnalyzerFieldMapping.MappingType.RESULT_LEVEL);

        if (!hasSampleIdMapping) {
            result.getWarnings().add("Required mapping missing: Sample ID");
        }
        if (!hasTestCodeMapping) {
            result.getWarnings().add("Required mapping missing: Test Code");
        }
        if (!hasResultValueMapping) {
            result.getWarnings().add("Required mapping missing: Result Value");
        }
    }
}
