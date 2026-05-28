package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerField.FieldType;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping.OpenELISFieldType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for mapping validation operations
 * 
 * 
 * Provides validation metrics and analysis for analyzer field mappings. Target
 * response time: <1 second
 */
@Service
@Transactional(readOnly = true)
public class MappingValidationServiceImpl implements MappingValidationService {

    private final AnalyzerFieldMappingDAO analyzerFieldMappingDAO;
    private final AnalyzerFieldDAO analyzerFieldDAO;

    @Autowired
    public MappingValidationServiceImpl(AnalyzerFieldMappingDAO analyzerFieldMappingDAO,
            AnalyzerFieldDAO analyzerFieldDAO) {
        this.analyzerFieldMappingDAO = analyzerFieldMappingDAO;
        this.analyzerFieldDAO = analyzerFieldDAO;
    }

    @Override
    @Cacheable(value = "validationMetrics", key = "#analyzerId", unless = "#result == null")
    public double calculateMappingAccuracy(String analyzerId) {
        // For now, calculate based on active mappings vs total fields
        List<AnalyzerField> allFields = analyzerFieldDAO.findByAnalyzerId(analyzerId);
        List<AnalyzerFieldMapping> activeMappings = analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId);

        if (allFields.isEmpty()) {
            return 0.0;
        }

        long mappedFields = activeMappings.stream().map(AnalyzerFieldMapping::getAnalyzerField)
                .map(AnalyzerField::getId).distinct().count();

        return (double) mappedFields / allFields.size();
    }

    @Override
    public List<String> identifyUnmappedFields(String analyzerId) {
        List<AnalyzerField> allFields = analyzerFieldDAO.findByAnalyzerId(analyzerId);
        List<AnalyzerFieldMapping> activeMappings = analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId);

        List<String> mappedFieldIds = activeMappings.stream().map(AnalyzerFieldMapping::getAnalyzerField)
                .map(AnalyzerField::getId).collect(Collectors.toList());

        return allFields.stream().filter(field -> !mappedFieldIds.contains(field.getId()))
                .map(AnalyzerField::getFieldName).collect(Collectors.toList());
    }

    @Override
    public List<String> validateTypeCompatibility(List<AnalyzerFieldMapping> mappings) {
        List<String> warnings = new ArrayList<>();

        for (AnalyzerFieldMapping mapping : mappings) {
            AnalyzerField field = mapping.getAnalyzerField();
            if (field == null) {
                continue;
            }

            FieldType analyzerFieldType = field.getFieldType();
            OpenELISFieldType openelisFieldType = mapping.getOpenelisFieldType();

            if (!isTypeCompatible(analyzerFieldType, openelisFieldType)) {
                warnings.add(String.format(
                        "Type mismatch for field '%s': Analyzer field type '%s' is not compatible with OpenELIS field type '%s'",
                        field.getFieldName(), analyzerFieldType, openelisFieldType));
            }
        }

        return warnings;
    }

    @Override
    public Map<String, Double> generateCoverageReport(String analyzerId) {
        List<AnalyzerField> allFields = analyzerFieldDAO.findByAnalyzerId(analyzerId);
        List<AnalyzerFieldMapping> activeMappings = analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId);

        Map<String, Double> coverageByTestUnit = new HashMap<>();

        // Group fields by test unit (using field name prefix or analyzer ref)
        Map<String, List<AnalyzerField>> fieldsByTestUnit = allFields.stream().collect(Collectors.groupingBy(field -> {
            // Extract test unit from field name or ASTM ref
            String testUnit = field.getAstmRef();
            if (testUnit == null || testUnit.isEmpty()) {
                // Fallback to field name prefix
                String fieldName = field.getFieldName();
                int underscoreIndex = fieldName.indexOf('_');
                if (underscoreIndex > 0) {
                    testUnit = fieldName.substring(0, underscoreIndex);
                } else {
                    testUnit = "UNKNOWN";
                }
            }
            return testUnit;
        }));

        for (Map.Entry<String, List<AnalyzerField>> entry : fieldsByTestUnit.entrySet()) {
            String testUnit = entry.getKey();
            List<AnalyzerField> fields = entry.getValue();

            long mappedCount = fields.stream().filter(field -> activeMappings.stream()
                    .anyMatch(mapping -> mapping.getAnalyzerField().getId().equals(field.getId()))).count();

            double coverage = fields.isEmpty() ? 0.0 : (double) mappedCount / fields.size();
            coverageByTestUnit.put(testUnit, coverage);
        }

        return coverageByTestUnit;
    }

    @Override
    public ValidationMetrics getValidationMetrics(String analyzerId) {
        ValidationMetrics metrics = new ValidationMetrics();

        metrics.setAccuracy(calculateMappingAccuracy(analyzerId));

        List<String> unmappedFields = identifyUnmappedFields(analyzerId);
        metrics.setUnmappedFields(unmappedFields);
        metrics.setUnmappedCount(unmappedFields.size());

        List<AnalyzerFieldMapping> activeMappings = analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId);
        List<String> warnings = validateTypeCompatibility(activeMappings);
        metrics.setWarnings(warnings);

        Map<String, Double> coverage = generateCoverageReport(analyzerId);
        metrics.setCoverageByTestUnit(coverage);

        return metrics;
    }

    /**
     * Check if analyzer field type is compatible with OpenELIS field type
     */
    private boolean isTypeCompatible(FieldType analyzerType, OpenELISFieldType openelisType) {
        if (analyzerType == null || openelisType == null) {
            return false;
        }

        // Numeric types are compatible with RESULT
        if (analyzerType == FieldType.NUMERIC && openelisType == OpenELISFieldType.RESULT) {
            return true;
        }

        // Qualitative types are compatible with RESULT
        if (analyzerType == FieldType.QUALITATIVE && openelisType == OpenELISFieldType.RESULT) {
            return true;
        }

        // Text types are compatible with TEST, SAMPLE, METADATA
        if (analyzerType == FieldType.TEXT && (openelisType == OpenELISFieldType.TEST
                || openelisType == OpenELISFieldType.SAMPLE || openelisType == OpenELISFieldType.METADATA)) {
            return true;
        }

        // Date/time types are compatible with METADATA
        if (analyzerType == FieldType.DATE_TIME && openelisType == OpenELISFieldType.METADATA) {
            return true;
        }

        // Unit types are compatible with UNIT
        if (analyzerType == FieldType.NUMERIC && openelisType == OpenELISFieldType.UNIT) {
            return true;
        }

        return false;
    }

    @CacheEvict(value = "validationMetrics", key = "#analyzerId")
    public void invalidateCache(String analyzerId) {
    }
}
