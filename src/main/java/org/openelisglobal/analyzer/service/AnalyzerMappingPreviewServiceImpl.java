package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.analyzer.valueholder.AstmFieldExtractionConfig;
import org.openelisglobal.analyzer.valueholder.AstmFlagMapping;
import org.openelisglobal.analyzer.valueholder.AstmTestMappingConfig;
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

    @Autowired(required = false)
    private AstmConfigService astmConfigService;

    @Autowired(required = false)
    private AstmQcRuleService astmQcRuleService;

    @Autowired(required = false)
    private AstmTestMappingConfigService astmTestMappingConfigService;

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
            enrichWithAstmV12Outputs(analyzerId, astmMessage, result);

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

    private void enrichWithAstmV12Outputs(String analyzerId, String astmMessage, MappingPreviewResult result) {
        if (astmConfigService == null || astmTestMappingConfigService == null || astmQcRuleService == null) {
            return;
        }

        List<String[]> segments = parseSegments(astmMessage);
        List<AstmFieldExtractionConfig> extractionConfigs = astmConfigService.getExtractionConfigs(analyzerId);
        Map<String, String> extracted = extractConfiguredFields(segments, extractionConfigs);

        List<Map<String, Object>> extractionApplied = new ArrayList<>();
        for (AstmFieldExtractionConfig config : extractionConfigs) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("key", config.getKey());
            entry.put("fieldIndex", config.getFieldIndex());
            entry.put("componentIndex", config.getComponentIndex());
            entry.put("value", extracted.get(config.getKey()));
            extractionApplied.add(entry);
        }
        result.setExtractionApplied(extractionApplied);

        boolean isQc = astmQcRuleService.evaluateAsQc(analyzerId, extracted);
        Map<String, Object> qcEvaluation = new LinkedHashMap<>();
        qcEvaluation.put("isQc", isQc);
        qcEvaluation.put("evaluatedFields", extracted);
        result.setQcRuleEvaluation(qcEvaluation);

        List<AstmTestMappingConfig> transforms = astmTestMappingConfigService.findByAnalyzerId(analyzerId);
        List<Map<String, Object>> transformResults = new ArrayList<>();
        List<Map<String, Object>> flagResults = new ArrayList<>();
        List<AstmFlagMapping> configuredFlagMappings = astmConfigService.getFlagMappings(analyzerId);

        for (String[] segment : segments) {
            if (segment.length == 0 || !"R".equals(segment[0])) {
                continue;
            }
            String testCode = getValueWithExtractionFallback(segment, "R", "TEST_ID_COMPONENT", extractionConfigs,
                    extracted);
            if (testCode == null || testCode.trim().isEmpty()) {
                testCode = getValueWithExtractionFallback(segment, "R", "TEST_ID_FIELD", extractionConfigs, extracted);
            }
            String rawValue = getValueWithExtractionFallback(segment, "R", "RESULT_VALUE_FIELD", extractionConfigs,
                    extracted);
            String abnormalFlag = getValueWithExtractionFallback(segment, "R", "ABNORMAL_FLAG_FIELD", extractionConfigs,
                    extracted);

            if (testCode == null || testCode.trim().isEmpty()) {
                continue;
            }

            String transformed = astmTestMappingConfigService.applyTransform(testCode, rawValue, transforms);
            Map<String, Object> transformResult = new LinkedHashMap<>();
            transformResult.put("analyzerTestName", testCode);
            transformResult.put("rawValue", rawValue);
            transformResult.put("transformedValue", transformed);
            transformResult.put("transformType", resolveTransformType(transforms, testCode));
            transformResults.add(transformResult);

            if (resolveTransformType(transforms, testCode) == null) {
                result.getWarnings().add("Unmapped analyzer test code detected: " + testCode);
            }

            if (abnormalFlag != null && !abnormalFlag.trim().isEmpty()) {
                String mappedFlag = resolveOpenelisFlag(configuredFlagMappings, abnormalFlag);
                Map<String, Object> flagResult = new LinkedHashMap<>();
                flagResult.put("analyzerFlag", abnormalFlag);
                flagResult.put("openelisFlag", mappedFlag);
                flagResult.put("analyzerTestName", testCode);
                flagResults.add(flagResult);
            }
        }

        result.setTransformResults(transformResults);
        result.setFlagMappings(flagResults);
    }

    private List<String[]> parseSegments(String astmMessage) {
        List<String[]> segments = new ArrayList<>();
        if (astmMessage == null || astmMessage.trim().isEmpty()) {
            return segments;
        }
        String[] lines = astmMessage.split("[\r\n]+");
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            segments.add(line.split("\\|", -1));
        }
        return segments;
    }

    private Map<String, String> extractConfiguredFields(List<String[]> segments,
            List<AstmFieldExtractionConfig> configs) {
        Map<String, String> extracted = new LinkedHashMap<>();
        Map<String, AstmFieldExtractionConfig> byKey = configs.stream()
                .collect(Collectors.toMap(AstmFieldExtractionConfig::getKey, c -> c, (a, b) -> a));
        for (Map.Entry<String, AstmFieldExtractionConfig> entry : byKey.entrySet()) {
            String key = entry.getKey();
            AstmFieldExtractionConfig cfg = entry.getValue();
            String recordType = key.startsWith("SENDER") ? "H" : key.startsWith("SPECIMEN") ? "O" : "R";
            String value = extractFromRecord(segments, recordType, cfg.getFieldIndex(), cfg.getComponentIndex());
            extracted.put(key, value);
        }
        return extracted;
    }

    private String extractFromRecord(List<String[]> segments, String recordType, Integer fieldIndex,
            Integer componentIndex) {
        if (fieldIndex == null || fieldIndex < 1) {
            return null;
        }
        for (String[] segment : segments) {
            if (segment.length == 0 || !recordType.equals(segment[0])) {
                continue;
            }
            if (fieldIndex >= segment.length) {
                return null;
            }
            String field = segment[fieldIndex];
            if (componentIndex == null) {
                return field;
            }
            if (componentIndex < 1) {
                return null;
            }
            String[] components = field.split("\\^", -1);
            if (componentIndex - 1 >= components.length) {
                return null;
            }
            return components[componentIndex - 1];
        }
        return null;
    }

    private String getValueWithExtractionFallback(String[] segment, String recordType, String key,
            List<AstmFieldExtractionConfig> configs, Map<String, String> extracted) {
        String configured = extracted.get(key);
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        AstmFieldExtractionConfig cfg = configs.stream().filter(c -> key.equals(c.getKey())).findFirst().orElse(null);
        if (cfg == null || cfg.getFieldIndex() == null || !recordType.equals(segment[0])) {
            return null;
        }
        if (cfg.getFieldIndex() >= segment.length) {
            return null;
        }
        String field = segment[cfg.getFieldIndex()];
        if (cfg.getComponentIndex() == null) {
            return field;
        }
        String[] components = field.split("\\^", -1);
        int componentPos = cfg.getComponentIndex() - 1;
        if (componentPos < 0 || componentPos >= components.length) {
            return null;
        }
        return components[componentPos];
    }

    private String resolveTransformType(List<AstmTestMappingConfig> transforms, String testCode) {
        for (AstmTestMappingConfig transform : transforms) {
            if (Boolean.TRUE.equals(transform.getIsActive()) && testCode.equals(transform.getAnalyzerTestName())) {
                return transform.getTransformType();
            }
        }
        return null;
    }

    private String resolveOpenelisFlag(List<AstmFlagMapping> mappings, String analyzerFlag) {
        for (AstmFlagMapping mapping : mappings) {
            if (analyzerFlag.equals(mapping.getAnalyzerFlag())) {
                return mapping.getOpenelisFlag();
            }
        }
        return analyzerFlag;
    }
}
