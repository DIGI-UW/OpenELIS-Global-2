package org.openelisglobal.analyzer.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.form.OpenELISFieldForm;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.analyzer.valueholder.UnitMapping;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for AnalyzerFieldMapping operations
 * 
 * Provides business logic for managing field mappings with: - Type
 * compatibility validation - Required mapping validation - Draft/active
 * workflow
 * 
 * Manual Relationship Management: This service uses
 * AnalyzerFieldMappingHydrator to manually load and set related entities
 * (AnalyzerField, Analyzer) on mappings. This avoids Hibernate's relationship
 * management which has issues when XML-mapped entities reference
 * annotation-based entities.
 * 
 * The DAO layer returns mappings with ID fields only (analyzerFieldId,
 * analyzerId). The service layer uses the hydrator to populate transient
 * relationship fields when needed for business logic.
 * 
 * This approach is documented in research.md section 2.5.
 */
@Service
@Transactional
public class AnalyzerFieldMappingServiceImpl extends BaseObjectServiceImpl<AnalyzerFieldMapping, String>
        implements AnalyzerFieldMappingService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerFieldMappingServiceImpl.class);

    private final AnalyzerFieldMappingDAO analyzerFieldMappingDAO;
    private final AnalyzerFieldDAO analyzerFieldDAO;
    private final AnalyzerFieldMappingHydrator hydrator;

    @Autowired(required = false)
    private AnalyzerService analyzerService;

    @Autowired(required = false)
    private AnalyzerErrorService analyzerErrorService;

    @Autowired(required = false)
    private OpenELISFieldService openELISFieldService;

    @Autowired(required = false)
    private UnitMappingService unitMappingService;

    @Autowired(required = false)
    private ValidationRuleConfigurationService validationRuleConfigurationService;

    @Autowired(required = false)
    private ValidationRuleEngine validationRuleEngine;

    @Autowired(required = false)
    private CustomFieldTypeService customFieldTypeService;

    @Autowired
    public AnalyzerFieldMappingServiceImpl(AnalyzerFieldMappingDAO analyzerFieldMappingDAO,
            AnalyzerFieldDAO analyzerFieldDAO, AnalyzerFieldMappingHydrator hydrator) {
        super(AnalyzerFieldMapping.class);
        this.analyzerFieldMappingDAO = analyzerFieldMappingDAO;
        this.analyzerFieldDAO = analyzerFieldDAO;
        this.hydrator = hydrator;
    }

    @Override
    protected BaseDAO<AnalyzerFieldMapping, String> getBaseObjectDAO() {
        return analyzerFieldMappingDAO;
    }

    /**
     * Map OpenELISFieldType enum to OpenELISFieldForm.EntityType enum
     * 
     * @param fieldType The OpenELIS field type from mapping
     * @return Corresponding EntityType or null if no mapping exists
     */
    private OpenELISFieldForm.EntityType mapOpenelisFieldTypeToEntityType(
            AnalyzerFieldMapping.OpenELISFieldType fieldType) {
        if (fieldType == null) {
            return null;
        }
        switch (fieldType) {
        case TEST:
            return OpenELISFieldForm.EntityType.TEST;
        case PANEL:
            return OpenELISFieldForm.EntityType.PANEL;
        case RESULT:
            return OpenELISFieldForm.EntityType.RESULT;
        case ORDER:
            return OpenELISFieldForm.EntityType.ORDER;
        case SAMPLE:
            return OpenELISFieldForm.EntityType.SAMPLE;
        case QC:
            return OpenELISFieldForm.EntityType.QC;
        case METADATA:
            return OpenELISFieldForm.EntityType.METADATA;
        case UNIT:
            return OpenELISFieldForm.EntityType.UNIT;
        default:
            return null;
        }
    }

    @Override
    @Transactional
    public String createMapping(AnalyzerFieldMapping mapping) {
        if (mapping.getAnalyzerField() == null && mapping.getAnalyzerFieldId() != null) {
            hydrator.hydrateAnalyzerField(mapping);
        }
        validateTypeCompatibility(mapping);

        if (mapping.getAnalyzer() == null && mapping.getAnalyzerField() != null
                && mapping.getAnalyzerField().getAnalyzer() != null) {
            mapping.setAnalyzer(mapping.getAnalyzerField().getAnalyzer());
        }

        if (mapping.getLastupdated() == null) {
            mapping.setLastupdatedFields();
        }

        return analyzerFieldMappingDAO.insert(mapping);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateRequiredMappings(String analyzerId) {
        List<AnalyzerFieldMapping> mappings = analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId);

        boolean hasRequiredMappings = mappings.stream().anyMatch(m -> m.getIsRequired() != null && m.getIsRequired());

        if (!hasRequiredMappings) {
            throw new LIMSRuntimeException(
                    "Required mappings missing. At least one mapping with isRequired=true must exist for analyzer activation");
        }
    }

    @Override
    @Transactional
    public AnalyzerFieldMapping activateMapping(String mappingId, boolean confirmed) {
        return activateMapping(mappingId, confirmed, null);
    }

    public AnalyzerFieldMapping activateMapping(String mappingId, boolean confirmed, Timestamp lastKnownUpdateTime) {
        return activateMapping(mappingId, confirmed, lastKnownUpdateTime, null);
    }

    public AnalyzerFieldMapping activateMapping(String mappingId, boolean confirmed, Timestamp lastKnownUpdateTime,
            Long expectedVersion) {
        AnalyzerFieldMapping mapping = get(mappingId);
        if (mapping == null) {
            throw new LIMSRuntimeException("Mapping not found: " + mappingId);
        }

        if (expectedVersion != null && mapping.getVersion() != null) {
            if (!mapping.getVersion().equals(expectedVersion)) {
                throw new LIMSRuntimeException("Mapping was modified by another user. Please refresh and try again.");
            }
        }

        // Fallback to timestamp-based check if version not provided (backward
        // compatibility)
        if (expectedVersion == null && lastKnownUpdateTime != null && mapping.getLastupdated() != null) {
            if (mapping.getLastupdated().after(lastKnownUpdateTime)) {
                throw new LIMSRuntimeException("Mapping was modified by another user. Please refresh and try again.");
            }
        }

        String analyzerId = mapping.getAnalyzerId();
        if (analyzerId == null) {
            hydrator.hydrateAnalyzerField(mapping);
            AnalyzerField field = mapping.getAnalyzerField();
            if (field == null || field.getAnalyzer() == null) {
                throw new LIMSRuntimeException("Analyzer field or analyzer not found for mapping: " + mappingId);
            }
            analyzerId = field.getAnalyzer().getId();
        }

        // Required mappings validation is performed at analyzer activation time,
        // not individual mapping activation, so mappings can be activated
        // independently.

        boolean analyzerIsActive = isAnalyzerActive(mapping);
        if (analyzerIsActive && !confirmed) {
            throw new LIMSRuntimeException("Confirmation required to activate mapping for active analyzer");
        }

        mapping.setIsActive(true);
        mapping.setLastupdatedFields();

        return analyzerFieldMappingDAO.update(mapping);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerFieldMapping> getMappingsByAnalyzerFieldId(String analyzerFieldId) {
        return analyzerFieldMappingDAO.findByAnalyzerFieldId(analyzerFieldId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMappingsForAnalyzer(String analyzerId) {
        return getMappingsForAnalyzer(analyzerId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMappingsForAnalyzer(String analyzerId, boolean includeRetired) {
        List<AnalyzerFieldMapping> mappings = analyzerFieldMappingDAO.findByAnalyzerId(analyzerId);
        hydrator.hydrateAnalyzerFields(mappings);

        List<Map<String, Object>> result = new ArrayList<>();
        for (AnalyzerFieldMapping mapping : mappings) {
            if (!includeRetired && (mapping.getIsActive() == null || !mapping.getIsActive())) {
                continue;
            }

            AnalyzerField field = mapping.getAnalyzerField();
            if (field == null) {
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", mapping.getId());
            map.put("analyzerFieldId", field.getId());
            map.put("analyzerFieldName", field.getFieldName());
            map.put("analyzerFieldType", field.getFieldType().toString());

            if (field.getFieldType() == AnalyzerField.FieldType.CUSTOM && field.getCustomFieldType() != null) {
                Map<String, Object> customFieldTypeMap = new HashMap<>();
                customFieldTypeMap.put("id", field.getCustomFieldType().getId());
                customFieldTypeMap.put("typeName", field.getCustomFieldType().getTypeName());
                customFieldTypeMap.put("displayName", field.getCustomFieldType().getDisplayName());
                map.put("customFieldType", customFieldTypeMap);
            }

            map.put("openelisFieldId", mapping.getOpenelisFieldId());
            map.put("openelisFieldType", mapping.getOpenelisFieldType().toString());
            map.put("mappingType", mapping.getMappingType().toString());
            map.put("isRequired", mapping.getIsRequired());
            map.put("isActive", mapping.getIsActive());
            map.put("specimenTypeConstraint", mapping.getSpecimenTypeConstraint());
            map.put("panelConstraint", mapping.getPanelConstraint());

            // Resolve OpenELIS field name if service is available
            if (openELISFieldService != null && mapping.getOpenelisFieldId() != null
                    && mapping.getOpenelisFieldType() != null) {
                try {
                    OpenELISFieldForm.EntityType entityType = mapOpenelisFieldTypeToEntityType(
                            mapping.getOpenelisFieldType());
                    if (entityType != null) {
                        Map<String, Object> openelisFieldData = openELISFieldService
                                .getFieldById(mapping.getOpenelisFieldId(), entityType);
                        if (openelisFieldData != null && openelisFieldData.containsKey("name")) {
                            map.put("openelisFieldName", openelisFieldData.get("name"));
                        }
                    }
                } catch (Exception e) {
                    // Log but don't fail - field name resolution is optional
                    // If name can't be resolved, frontend will use ID
                }
            }

            if (unitMappingService != null && field.getUnit() != null && !field.getUnit().trim().isEmpty()) {
                try {
                    List<UnitMapping> unitMappings = unitMappingService.getMappingsByAnalyzerFieldId(field.getId());
                    if (unitMappings != null && !unitMappings.isEmpty()) {
                        for (UnitMapping unitMapping : unitMappings) {
                            if (unitMapping.getAnalyzerUnit() != null
                                    && field.getUnit().equals(unitMapping.getAnalyzerUnit())) {
                                Map<String, Object> unitMap = new HashMap<>();
                                unitMap.put("analyzerUnit", unitMapping.getAnalyzerUnit());
                                unitMap.put("openelisUnit", unitMapping.getOpenelisUnit());
                                if (unitMapping.getConversionFactor() != null) {
                                    unitMap.put("conversionFactor", unitMapping.getConversionFactor());
                                }
                                map.put("unitMapping", unitMap);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not resolve unit mapping for field {}: {}", field.getId(), e.getMessage());
                    // Log but don't fail - unit mapping is optional
                }
            }

            if (field.getFieldType() == AnalyzerField.FieldType.CUSTOM) {
                try {
                    if (validationRuleConfigurationService != null) {
                        String customFieldTypeId = field.getCustomFieldTypeId();
                        if (customFieldTypeId != null) {
                            List<org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration> rules = validationRuleConfigurationService
                                    .findActiveRulesByCustomFieldTypeId(customFieldTypeId);
                            if (rules != null && !rules.isEmpty()) {
                                List<Map<String, Object>> rulesList = new ArrayList<>();
                                for (org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration rule : rules) {
                                    Map<String, Object> ruleMap = new HashMap<>();
                                    ruleMap.put("id", rule.getId());
                                    ruleMap.put("ruleName", rule.getRuleName());
                                    ruleMap.put("ruleType", rule.getRuleType().toString());
                                    ruleMap.put("ruleExpression", rule.getRuleExpression());
                                    ruleMap.put("errorMessage", rule.getErrorMessage());
                                    rulesList.add(ruleMap);
                                }
                                map.put("validationRules", rulesList);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not fetch validation rules for CUSTOM field {}: {}", field.getId(),
                            e.getMessage());
                    // Log but don't fail - validation rules are optional
                }
            }

            if (!mapping.getIsActive() && mapping.getLastupdated() != null) {
                map.put("retirementDate", mapping.getLastupdated());
                // Note: retirement_reason would be stored in notes field or separate column
                // For now, we use lastupdated as retirement date
            }
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createMappingForAnalyzer(String analyzerId, String analyzerFieldId,
            String openelisFieldId, AnalyzerFieldMapping.OpenELISFieldType openelisFieldType,
            AnalyzerFieldMapping.MappingType mappingType, Boolean isRequired, Boolean isActive,
            String specimenTypeConstraint, String panelConstraint) {

        AnalyzerField field = analyzerFieldDAO.findByIdWithAnalyzer(analyzerFieldId)
                .orElseThrow(() -> new LIMSRuntimeException("AnalyzerField not found: " + analyzerFieldId));

        if (field.getAnalyzer() == null || !field.getAnalyzer().getId().equals(analyzerId)) {
            throw new LIMSRuntimeException("Analyzer field does not belong to analyzer: " + analyzerId);
        }

        AnalyzerFieldMapping mapping = new AnalyzerFieldMapping();
        mapping.setAnalyzerField(field);
        mapping.setAnalyzer(field.getAnalyzer());
        mapping.setOpenelisFieldId(openelisFieldId);
        mapping.setOpenelisFieldType(openelisFieldType);
        mapping.setMappingType(mappingType);
        mapping.setIsRequired(isRequired != null ? isRequired : false);
        mapping.setIsActive(isActive != null ? isActive : false);
        mapping.setSpecimenTypeConstraint(specimenTypeConstraint);
        mapping.setPanelConstraint(panelConstraint);
        mapping.setSysUserId("1"); // Default system user (should come from security context)

        String mappingId = createMapping(mapping);
        return getMappingWithCompleteData(mappingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMappingWithCompleteData(String mappingId) {
        AnalyzerFieldMapping mapping = analyzerFieldMappingDAO.get(mappingId)
                .orElseThrow(() -> new LIMSRuntimeException("Mapping not found: " + mappingId));
        hydrator.hydrateAnalyzerField(mapping);

        AnalyzerField field = mapping.getAnalyzerField();
        if (field == null) {
            throw new LIMSRuntimeException("AnalyzerField not found for mapping: " + mappingId);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", mapping.getId());
        map.put("analyzerFieldId", field.getId());
        map.put("analyzerFieldName", field.getFieldName());
        map.put("analyzerFieldType", field.getFieldType().toString());

        // Add custom field type information if field type is CUSTOM
        if (field.getFieldType() == AnalyzerField.FieldType.CUSTOM && field.getCustomFieldType() != null) {
            Map<String, Object> customFieldTypeMap = new HashMap<>();
            customFieldTypeMap.put("id", field.getCustomFieldType().getId());
            customFieldTypeMap.put("typeName", field.getCustomFieldType().getTypeName());
            customFieldTypeMap.put("displayName", field.getCustomFieldType().getDisplayName());
            map.put("customFieldType", customFieldTypeMap);
        }

        map.put("openelisFieldId", mapping.getOpenelisFieldId());
        map.put("openelisFieldType", mapping.getOpenelisFieldType().toString());
        map.put("mappingType", mapping.getMappingType().toString());
        map.put("isRequired", mapping.getIsRequired());
        map.put("isActive", mapping.getIsActive());
        map.put("specimenTypeConstraint", mapping.getSpecimenTypeConstraint());
        map.put("panelConstraint", mapping.getPanelConstraint());

        // Add validation rules for CUSTOM field types
        if (field.getFieldType() == AnalyzerField.FieldType.CUSTOM) {
            try {
                if (validationRuleConfigurationService != null) {
                    String customFieldTypeId = field.getCustomFieldTypeId();
                    if (customFieldTypeId != null) {
                        List<org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration> rules = validationRuleConfigurationService
                                .findActiveRulesByCustomFieldTypeId(customFieldTypeId);
                        if (rules != null && !rules.isEmpty()) {
                            List<Map<String, Object>> rulesList = new ArrayList<>();
                            for (org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration rule : rules) {
                                Map<String, Object> ruleMap = new HashMap<>();
                                ruleMap.put("id", rule.getId());
                                ruleMap.put("ruleName", rule.getRuleName());
                                ruleMap.put("ruleType", rule.getRuleType().toString());
                                ruleMap.put("ruleExpression", rule.getRuleExpression());
                                ruleMap.put("errorMessage", rule.getErrorMessage());
                                rulesList.add(ruleMap);
                            }
                            map.put("validationRules", rulesList);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not fetch validation rules for CUSTOM field {}: {}", field.getId(), e.getMessage());
                // Log but don't fail - validation rules are optional
            }
        }

        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyMappingBelongsToAnalyzer(String mappingId, String analyzerId) {
        AnalyzerFieldMapping mapping = analyzerFieldMappingDAO.get(mappingId).orElse(null);
        if (mapping == null) {
            return false;
        }

        // Check analyzerId directly (no need to hydrate if ID matches)
        if (mapping.getAnalyzerId() != null && mapping.getAnalyzerId().equals(analyzerId)) {
            return true;
        }

        // If analyzerId doesn't match or is null, hydrate and check via field
        hydrator.hydrateAnalyzerField(mapping);
        AnalyzerField field = mapping.getAnalyzerField();
        if (field == null || field.getAnalyzer() == null) {
            return false;
        }

        return field.getAnalyzer().getId().equals(analyzerId);
    }

    /**
     * Validate that analyzer field type is compatible with OpenELIS field type
     * 
     * Rules: - NUMERIC analyzer field → NUMERIC OpenELIS field (TEST, RESULT with
     * numeric type) - QUALITATIVE analyzer field → QUALITATIVE OpenELIS field
     * (RESULT with coded values) - TEXT analyzer field → TEXT OpenELIS field
     * (METADATA, ORDER)
     * 
     * @param mapping The mapping to validate (must have analyzerField hydrated)
     * @throws LIMSRuntimeException if types are incompatible
     */
    private void validateTypeCompatibility(AnalyzerFieldMapping mapping) {
        // Get analyzerField - should already be hydrated by caller
        AnalyzerField field = mapping.getAnalyzerField();
        if (field == null) {
            // Try to hydrate if not already done
            if (mapping.getAnalyzerFieldId() != null) {
                hydrator.hydrateAnalyzerField(mapping);
                field = mapping.getAnalyzerField();
            }
            if (field == null) {
                throw new LIMSRuntimeException("AnalyzerField must be set on mapping");
            }
        }

        AnalyzerField.FieldType analyzerFieldType = field.getFieldType();
        AnalyzerFieldMapping.OpenELISFieldType openelisFieldType = mapping.getOpenelisFieldType();

        if (analyzerFieldType == AnalyzerField.FieldType.NUMERIC) {
            // NUMERIC can map to TEST or RESULT (both can be numeric)
            if (openelisFieldType != AnalyzerFieldMapping.OpenELISFieldType.TEST
                    && openelisFieldType != AnalyzerFieldMapping.OpenELISFieldType.RESULT) {
                throw new LIMSRuntimeException("NUMERIC analyzer field can only map to TEST or RESULT OpenELIS fields. "
                        + "Attempted: " + openelisFieldType);
            }
        } else if (analyzerFieldType == AnalyzerField.FieldType.QUALITATIVE) {
            // QUALITATIVE can map to RESULT (coded values)
            if (openelisFieldType != AnalyzerFieldMapping.OpenELISFieldType.RESULT) {
                throw new LIMSRuntimeException("QUALITATIVE analyzer field can only map to RESULT OpenELIS fields. "
                        + "Attempted: " + openelisFieldType);
            }
        } else if (analyzerFieldType == AnalyzerField.FieldType.TEXT) {
            // TEXT can map to METADATA, ORDER, or SAMPLE (for Sample ID mappings)
            if (openelisFieldType != AnalyzerFieldMapping.OpenELISFieldType.METADATA
                    && openelisFieldType != AnalyzerFieldMapping.OpenELISFieldType.ORDER
                    && openelisFieldType != AnalyzerFieldMapping.OpenELISFieldType.SAMPLE) {
                throw new LIMSRuntimeException(
                        "TEXT analyzer field can only map to METADATA, ORDER, or SAMPLE OpenELIS fields. "
                                + "Attempted: " + openelisFieldType);
            }
        }
        // Other types (CONTROL_TEST, MELTING_POINT, DATE_TIME, CUSTOM) have flexible
        // mapping
    }

    @Override
    @Transactional
    public AnalyzerFieldMapping updateMapping(AnalyzerFieldMapping mapping, boolean confirmed) {
        AnalyzerFieldMapping existingMapping = get(mapping.getId());
        if (existingMapping == null) {
            throw new LIMSRuntimeException("Mapping not found: " + mapping.getId());
        }

        validateTypeCompatibility(mapping);

        boolean analyzerIsActive = isAnalyzerActive(existingMapping);
        boolean mappingIsActive = existingMapping.getIsActive() != null && existingMapping.getIsActive();

        if (analyzerIsActive && mappingIsActive && !confirmed) {
            throw new LIMSRuntimeException("Confirmation required to update active mapping for active analyzer");
        }

        existingMapping.setOpenelisFieldId(mapping.getOpenelisFieldId());
        existingMapping.setOpenelisFieldType(mapping.getOpenelisFieldType());
        existingMapping.setMappingType(mapping.getMappingType());
        existingMapping.setIsRequired(mapping.getIsRequired());
        existingMapping.setIsActive(mapping.getIsActive());
        existingMapping.setSpecimenTypeConstraint(mapping.getSpecimenTypeConstraint());
        existingMapping.setPanelConstraint(mapping.getPanelConstraint());

        if (mapping.getSysUserId() != null) {
            existingMapping.setSysUserId(mapping.getSysUserId());
        }
        existingMapping.setLastupdatedFields();

        // Note: Detailed audit trail (previous vs new values) can be added via
        // AuditTrailService
        // if needed. BaseObject audit fields (sys_user_id, last_updated) provide basic
        // audit trail.
        // To enable detailed audit trail, inject AuditTrailService and call:
        // auditTrailService.saveHistory(mapping, existingMapping,
        // mapping.getSysUserId(),
        // IActionConstants.AUDIT_TRAIL_UPDATE, getBaseObjectDAO().getTableName());

        return analyzerFieldMappingDAO.update(existingMapping);
    }

    @Override
    @Transactional
    public AnalyzerFieldMapping disableMapping(String mappingId, String retirementReason) {
        AnalyzerFieldMapping mapping = get(mappingId);
        if (mapping == null) {
            throw new LIMSRuntimeException("Mapping not found: " + mappingId);
        }

        if (mapping.getIsRequired() != null && mapping.getIsRequired()) {
            throw new LIMSRuntimeException(
                    "Cannot disable required mapping. Required mappings (Sample ID, Test Code, Result Value) must remain active.");
        }

        if (analyzerErrorService != null) {
            String analyzerId = mapping.getAnalyzerId();
            if (analyzerId == null) {
                hydrator.hydrateAnalyzerField(mapping);
                AnalyzerField field = mapping.getAnalyzerField();
                if (field != null && field.getAnalyzer() != null) {
                    analyzerId = field.getAnalyzer().getId();
                }
            }
            if (analyzerId != null) {
                List<AnalyzerError> pendingErrors = analyzerErrorService.getErrorsByFilters(analyzerId, null, null,
                        AnalyzerError.ErrorStatus.UNACKNOWLEDGED, null, null);
                if (pendingErrors != null && !pendingErrors.isEmpty()) {
                    // Check if any pending errors reference this mapping
                    // Note: This is a simplified check - in practice, we'd need to check if the
                    // error's raw message references fields mapped by this mapping
                    // For now, we check if analyzer has any pending messages
                    throw new LIMSRuntimeException("Cannot retire mapping: " + pendingErrors.size()
                            + " pending messages reference this mapping. Please resolve errors first.");
                }
            }
        }

        mapping.setIsActive(false);
        mapping.setLastupdatedFields();

        // Note: Retirement reason and detailed audit trail (previous vs new values) can
        // be added
        // via AuditTrailService if needed. BaseObject audit fields (sys_user_id,
        // last_updated)
        // provide basic audit trail for who and when. To enable detailed audit trail,
        // inject
        // AuditTrailService and call:
        // auditTrailService.saveHistory(mapping, existingMapping,
        // mapping.getSysUserId(),
        // IActionConstants.AUDIT_TRAIL_UPDATE, getBaseObjectDAO().getTableName());

        return analyzerFieldMappingDAO.update(mapping);
    }

    /**
     * Check if analyzer is active by checking Analyzer status
     *
     * @param mapping The mapping to check analyzer status for
     * @return true if analyzer is active, false otherwise
     */
    private boolean isAnalyzerActive(AnalyzerFieldMapping mapping) {
        if (analyzerService == null) {
            // If service not available, assume analyzer is not active (safe default)
            return false;
        }

        String analyzerId = mapping.getAnalyzerId();
        if (analyzerId == null) {
            hydrator.hydrateAnalyzerField(mapping);
            AnalyzerField field = mapping.getAnalyzerField();
            if (field == null || field.getAnalyzer() == null) {
                return false;
            }
            analyzerId = field.getAnalyzer().getId();
        }

        Analyzer analyzer = analyzerService.get(analyzerId);
        if (analyzer != null) {
            return analyzer.getStatus() == AnalyzerStatus.ACTIVE;
        }

        return false;
    }

    @Override
    @Transactional
    public int bulkActivateMappings(String analyzerId, List<String> mappingIds, boolean confirmed) {
        int activatedCount = 0;
        for (String mappingId : mappingIds) {
            // Ownership check — fail immediately instead of silently skipping
            if (!verifyMappingBelongsToAnalyzer(mappingId, analyzerId)) {
                throw new LIMSRuntimeException("Mapping " + mappingId + " does not belong to analyzer " + analyzerId);
            }
            activateMapping(mappingId, confirmed);
            activatedCount++;
        }
        return activatedCount;
    }

    @Override
    @Transactional(readOnly = true)
    public ActivationValidationResult validateActivation(String analyzerId) {
        ActivationValidationResult result = new ActivationValidationResult();

        List<AnalyzerFieldMapping> mappings = analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId);

        List<String> missingRequired = new ArrayList<>();
        boolean hasSampleIdMapping = mappings.stream().anyMatch(m -> m.getIsRequired() != null && m.getIsRequired()
                && m.getOpenelisFieldType() == AnalyzerFieldMapping.OpenELISFieldType.SAMPLE);
        boolean hasTestCodeMapping = mappings.stream().anyMatch(m -> m.getIsRequired() != null && m.getIsRequired()
                && m.getMappingType() == AnalyzerFieldMapping.MappingType.TEST_LEVEL);
        boolean hasResultValueMapping = mappings.stream().anyMatch(m -> m.getIsRequired() != null && m.getIsRequired()
                && m.getMappingType() == AnalyzerFieldMapping.MappingType.RESULT_LEVEL);

        if (!hasSampleIdMapping) {
            missingRequired.add("Sample ID");
        }
        if (!hasTestCodeMapping) {
            missingRequired.add("Test Code");
        }
        if (!hasResultValueMapping) {
            missingRequired.add("Result Value");
        }

        result.setMissingRequired(missingRequired);

        int pendingMessagesCount = 0;
        if (analyzerErrorService != null) {
            List<AnalyzerError> pendingErrors = analyzerErrorService.getErrorsByFilters(analyzerId, null, null,
                    AnalyzerError.ErrorStatus.UNACKNOWLEDGED, null, null);
            pendingMessagesCount = pendingErrors != null ? pendingErrors.size() : 0;
        }
        result.setPendingMessagesCount(pendingMessagesCount);

        List<String> warnings = new ArrayList<>();
        if (pendingMessagesCount > 0) {
            warnings.add("This analyzer has " + pendingMessagesCount
                    + " pending messages in the error queue. Activating mapping changes may affect how these messages are reprocessed.");
        }

        for (AnalyzerFieldMapping mapping : mappings) {
            try {
                validateTypeCompatibility(mapping);
            } catch (LIMSRuntimeException e) {
                warnings.add("Type incompatibility detected in mapping: " + e.getMessage());
            }
        }

        // Check for concurrent edits using version-based optimistic locking
        // Note: This is a pre-check. Actual optimistic locking happens during update
        // via Hibernate @Version
        // If any mapping version has changed since last load, concurrent edit is
        // detected
        // The actual version check happens in activateMapping method when
        // expectedVersion is provided

        result.setWarnings(warnings);

        boolean canActivate = missingRequired.isEmpty();
        result.setCanActivate(canActivate);

        return result;
    }
}
