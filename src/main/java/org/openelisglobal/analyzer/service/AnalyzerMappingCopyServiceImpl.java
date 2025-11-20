package org.openelisglobal.analyzer.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for copying analyzer field mappings
 * 
 * Task Reference: T193
 * 
 * Provides business logic for copying mappings from source to target analyzer
 * with: - Conflict resolution (overwrite, merge) - Type compatibility
 * validation - Transaction rollback on failure
 */
@Service
@Transactional
public class AnalyzerMappingCopyServiceImpl implements AnalyzerMappingCopyService {

    private final AnalyzerFieldMappingDAO analyzerFieldMappingDAO;
    private final AnalyzerFieldDAO analyzerFieldDAO;

    @Autowired
    public AnalyzerMappingCopyServiceImpl(AnalyzerFieldMappingDAO analyzerFieldMappingDAO,
            AnalyzerFieldDAO analyzerFieldDAO) {
        this.analyzerFieldMappingDAO = analyzerFieldMappingDAO;
        this.analyzerFieldDAO = analyzerFieldDAO;
    }

    @Override
    @Transactional
    public CopyMappingsResult copyMappings(String sourceAnalyzerId, String targetAnalyzerId, CopyOptions options) {
        CopyMappingsResult result = new CopyMappingsResult();

        // Default options
        if (options == null) {
            options = new CopyOptions();
        }

        // Get source mappings
        List<AnalyzerFieldMapping> sourceMappings = analyzerFieldMappingDAO
                .findActiveMappingsByAnalyzerId(sourceAnalyzerId);
        if (sourceMappings == null || sourceMappings.isEmpty()) {
            throw new LIMSRuntimeException("Source analyzer has no active mappings to copy");
        }

        // Get target mappings (for conflict detection)
        List<AnalyzerFieldMapping> targetMappings = analyzerFieldMappingDAO
                .findActiveMappingsByAnalyzerId(targetAnalyzerId);

        // Build map of target mappings by field name for quick lookup
        Map<String, AnalyzerFieldMapping> targetMappingsByFieldName = new HashMap<>();
        for (AnalyzerFieldMapping targetMapping : targetMappings) {
            AnalyzerField targetField = targetMapping.getAnalyzerField();
            if (targetField != null && targetField.getFieldName() != null) {
                targetMappingsByFieldName.put(targetField.getFieldName(), targetMapping);
            }
        }

        // Copy each source mapping
        for (AnalyzerFieldMapping sourceMapping : sourceMappings) {
            try {
                AnalyzerField sourceField = sourceMapping.getAnalyzerField();
                if (sourceField == null || sourceField.getFieldName() == null) {
                    result.setSkippedCount(result.getSkippedCount() + 1);
                    result.getWarnings().add("Skipped mapping with null field: " + sourceMapping.getId());
                    continue;
                }

                // Find corresponding field in target analyzer
                Optional<AnalyzerField> targetFieldOpt = analyzerFieldDAO.findByAnalyzerIdAndFieldName(targetAnalyzerId,
                        sourceField.getFieldName());

                if (!targetFieldOpt.isPresent()) {
                    result.setSkippedCount(result.getSkippedCount() + 1);
                    result.getWarnings().add("Skipped mapping for field '" + sourceField.getFieldName()
                            + "': Field not found in target analyzer");
                    continue;
                }

                AnalyzerField targetField = targetFieldOpt.get();

                // Check type compatibility
                if (!sourceField.getFieldType().equals(targetField.getFieldType())) {
                    if (options.isSkipIncompatible()) {
                        result.setSkippedCount(result.getSkippedCount() + 1);
                        result.getWarnings()
                                .add("Skipped mapping for field '" + sourceField.getFieldName()
                                        + "': Type incompatibility (source: " + sourceField.getFieldType()
                                        + ", target: " + targetField.getFieldType() + ")");
                        continue;
                    } else {
                        // Generate warning but continue
                        result.getWarnings()
                                .add("Type incompatibility for field '" + sourceField.getFieldName() + "': source type "
                                        + sourceField.getFieldType() + " vs target type " + targetField.getFieldType());
                    }
                }

                // Check if target already has mapping for this field
                AnalyzerFieldMapping existingTargetMapping = targetMappingsByFieldName.get(sourceField.getFieldName());

                if (existingTargetMapping != null) {
                    if (options.isOverwriteExisting()) {
                        // Overwrite existing mapping
                        existingTargetMapping.setOpenelisFieldId(sourceMapping.getOpenelisFieldId());
                        existingTargetMapping.setOpenelisFieldType(sourceMapping.getOpenelisFieldType());
                        existingTargetMapping.setMappingType(sourceMapping.getMappingType());
                        existingTargetMapping.setIsRequired(sourceMapping.getIsRequired());
                        existingTargetMapping.setIsActive(sourceMapping.getIsActive());
                        existingTargetMapping.setSpecimenTypeConstraint(sourceMapping.getSpecimenTypeConstraint());
                        existingTargetMapping.setPanelConstraint(sourceMapping.getPanelConstraint());
                        existingTargetMapping.setLastupdatedFields();

                        analyzerFieldMappingDAO.update(existingTargetMapping);
                        result.setCopiedCount(result.getCopiedCount() + 1);
                    } else {
                        result.setSkippedCount(result.getSkippedCount() + 1);
                        result.getWarnings().add("Skipped mapping for field '" + sourceField.getFieldName()
                                + "': Existing mapping found and overwrite disabled");
                    }
                } else {
                    // Create new mapping
                    AnalyzerFieldMapping newMapping = new AnalyzerFieldMapping();
                    newMapping.setAnalyzerField(targetField);
                    newMapping.setOpenelisFieldId(sourceMapping.getOpenelisFieldId());
                    newMapping.setOpenelisFieldType(sourceMapping.getOpenelisFieldType());
                    newMapping.setMappingType(sourceMapping.getMappingType());
                    newMapping.setIsRequired(sourceMapping.getIsRequired());
                    newMapping.setIsActive(sourceMapping.getIsActive());
                    newMapping.setSpecimenTypeConstraint(sourceMapping.getSpecimenTypeConstraint());
                    newMapping.setPanelConstraint(sourceMapping.getPanelConstraint());
                    newMapping.setSysUserId("1"); // Default system user

                    analyzerFieldMappingDAO.insert(newMapping);
                    result.setCopiedCount(result.getCopiedCount() + 1);
                }
            } catch (Exception e) {
                // Rollback on any failure
                throw new LIMSRuntimeException("Error copying mapping: " + e.getMessage(), e);
            }
        }

        return result;
    }
}
