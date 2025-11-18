package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/**
 * Service implementation for AnalyzerFieldMapping operations
 * 
 * Provides business logic for managing field mappings with:
 * - Type compatibility validation
 * - Required mapping validation
 * - Draft/active workflow
 */
@Service
@Transactional
public class AnalyzerFieldMappingServiceImpl extends BaseObjectServiceImpl<AnalyzerFieldMapping, String>
        implements AnalyzerFieldMappingService {

    private final AnalyzerFieldMappingDAO analyzerFieldMappingDAO;
    private final AnalyzerFieldDAO analyzerFieldDAO;
    
    @Autowired(required = false)
    private AnalyzerConfigurationService analyzerConfigurationService;

    @Autowired
    public AnalyzerFieldMappingServiceImpl(AnalyzerFieldMappingDAO analyzerFieldMappingDAO,
            AnalyzerFieldDAO analyzerFieldDAO) {
        super(AnalyzerFieldMapping.class);
        this.analyzerFieldMappingDAO = analyzerFieldMappingDAO;
        this.analyzerFieldDAO = analyzerFieldDAO;
    }

    @Override
    protected BaseDAO<AnalyzerFieldMapping, String> getBaseObjectDAO() {
        return analyzerFieldMappingDAO;
    }

    @Override
    @Transactional
    public String createMapping(AnalyzerFieldMapping mapping) {
        // Validate type compatibility
        validateTypeCompatibility(mapping);
        
        return analyzerFieldMappingDAO.insert(mapping);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateRequiredMappings(String analyzerId) {
        List<AnalyzerFieldMapping> mappings = analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId);
        
        // Check if there are any required mappings
        boolean hasRequiredMappings = mappings.stream()
                .anyMatch(m -> m.getIsRequired() != null && m.getIsRequired());

        if (!hasRequiredMappings) {
            throw new LIMSRuntimeException(
                    "Required mappings missing. At least one mapping with isRequired=true must exist for analyzer activation");
        }
    }

    @Override
    @Transactional
    public AnalyzerFieldMapping activateMapping(String mappingId, boolean confirmed) {
        AnalyzerFieldMapping mapping = get(mappingId);
        if (mapping == null) {
            throw new LIMSRuntimeException("Mapping not found: " + mappingId);
        }
        
        // Get analyzer ID from mapping
        AnalyzerField field = analyzerFieldDAO.findByIdWithAnalyzer(mapping.getAnalyzerField().getId())
                .orElse(null);
        if (field == null || field.getAnalyzer() == null) {
            throw new LIMSRuntimeException("Analyzer field or analyzer not found for mapping: " + mappingId);
        }
        String analyzerId = field.getAnalyzer().getId();
        
        // Note: Required mappings validation (T074 requirement) should be performed at analyzer
        // activation time, not individual mapping activation time. This allows mappings to be
        // activated independently while still ensuring analyzer configuration completeness before
        // the analyzer is activated for production use.
        // The validateRequiredMappings() method can be called from analyzer activation workflow.
        
        // Check if analyzer is active - requires confirmation for active analyzers
        boolean analyzerIsActive = isAnalyzerActive(mapping);
        if (analyzerIsActive && !confirmed) {
            throw new LIMSRuntimeException(
                    "Confirmation required to activate mapping for active analyzer");
        }
        
        // Activate mapping
        mapping.setIsActive(true);
        // Set audit fields (T075: who, when)
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
        // Get mappings with analyzerField eagerly loaded (uses native SQL for analyzer_id foreign key)
        List<AnalyzerFieldMapping> mappings = analyzerFieldMappingDAO.findByAnalyzerIdWithFields(analyzerId);
        
        // Compile complete data - eagerly fetch analyzer relationship for each field
        List<Map<String, Object>> result = new ArrayList<>();
        for (AnalyzerFieldMapping mapping : mappings) {
            // analyzerField is already eagerly loaded, but analyzer relationship is lazy
            // Eagerly fetch analyzer relationship using findByIdWithAnalyzer
            AnalyzerField field = analyzerFieldDAO.findByIdWithAnalyzer(mapping.getAnalyzerField().getId())
                    .orElse(null);
            if (field == null) {
                continue; // Skip if field not found
            }
            
            // All relationships are eagerly loaded, safe to access within transaction
            Map<String, Object> map = new HashMap<>();
            map.put("id", mapping.getId());
            map.put("analyzerFieldId", field.getId());
            map.put("analyzerFieldName", field.getFieldName());
            map.put("analyzerFieldType", field.getFieldType().toString());
            map.put("openelisFieldId", mapping.getOpenelisFieldId());
            map.put("openelisFieldType", mapping.getOpenelisFieldType().toString());
            map.put("mappingType", mapping.getMappingType().toString());
            map.put("isRequired", mapping.getIsRequired());
            map.put("isActive", mapping.getIsActive());
            map.put("specimenTypeConstraint", mapping.getSpecimenTypeConstraint());
            map.put("panelConstraint", mapping.getPanelConstraint());
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
        
        // Get analyzer field with analyzer eagerly fetched (within transaction)
        // Use JOIN FETCH to ensure Analyzer relationship is loaded
        AnalyzerField field = analyzerFieldDAO.findByIdWithAnalyzer(analyzerFieldId)
                .orElseThrow(() -> new LIMSRuntimeException("AnalyzerField not found: " + analyzerFieldId));
        
        // Verify field belongs to analyzer (relationship eagerly loaded, safe to access)
        if (!field.getAnalyzer().getId().equals(analyzerId)) {
            throw new LIMSRuntimeException("Analyzer field does not belong to analyzer: " + analyzerId);
        }
        
        // Create mapping entity
        AnalyzerFieldMapping mapping = new AnalyzerFieldMapping();
        mapping.setAnalyzerField(field);
        mapping.setOpenelisFieldId(openelisFieldId);
        mapping.setOpenelisFieldType(openelisFieldType);
        mapping.setMappingType(mappingType);
        mapping.setIsRequired(isRequired != null ? isRequired : false);
        mapping.setIsActive(isActive != null ? isActive : false);
        mapping.setSpecimenTypeConstraint(specimenTypeConstraint);
        mapping.setPanelConstraint(panelConstraint);
        mapping.setSysUserId("1"); // Default system user (should come from security context)
        
        // Validate and create
        String mappingId = createMapping(mapping);
        
        // Return complete data
        return getMappingWithCompleteData(mappingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMappingWithCompleteData(String mappingId) {
        // Eagerly fetch mapping with field within transaction
        AnalyzerFieldMapping mapping = analyzerFieldMappingDAO.findByIdWithField(mappingId);
        if (mapping == null) {
            throw new LIMSRuntimeException("Mapping not found: " + mappingId);
        }
        
        // Eagerly fetch analyzerField with analyzer relationship
        AnalyzerField field = analyzerFieldDAO.findByIdWithAnalyzer(mapping.getAnalyzerField().getId())
                .orElseThrow(() -> new LIMSRuntimeException("AnalyzerField not found: " + mapping.getAnalyzerField().getId()));
        
        // All relationships are eagerly loaded, safe to access within transaction
        Map<String, Object> map = new HashMap<>();
        map.put("id", mapping.getId());
        map.put("analyzerFieldId", field.getId());
        map.put("analyzerFieldName", field.getFieldName());
        map.put("analyzerFieldType", field.getFieldType().toString());
        map.put("openelisFieldId", mapping.getOpenelisFieldId());
        map.put("openelisFieldType", mapping.getOpenelisFieldType().toString());
        map.put("mappingType", mapping.getMappingType().toString());
        map.put("isRequired", mapping.getIsRequired());
        map.put("isActive", mapping.getIsActive());
        map.put("specimenTypeConstraint", mapping.getSpecimenTypeConstraint());
        map.put("panelConstraint", mapping.getPanelConstraint());
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyMappingBelongsToAnalyzer(String mappingId, String analyzerId) {
        // Eagerly fetch mapping with field within transaction
        AnalyzerFieldMapping mapping = analyzerFieldMappingDAO.findByIdWithField(mappingId);
        if (mapping == null) {
            return false;
        }
        
        // Eagerly fetch analyzerField with analyzer relationship
        AnalyzerField field = analyzerFieldDAO.findByIdWithAnalyzer(mapping.getAnalyzerField().getId())
                .orElse(null);
        if (field == null) {
            return false;
        }
        
        // All relationships are eagerly loaded, safe to access within transaction
        return field.getAnalyzer().getId().equals(analyzerId);
    }

    /**
     * Validate that analyzer field type is compatible with OpenELIS field type
     * 
     * Rules:
     * - NUMERIC analyzer field → NUMERIC OpenELIS field (TEST, RESULT with numeric type)
     * - QUALITATIVE analyzer field → QUALITATIVE OpenELIS field (RESULT with coded values)
     * - TEXT analyzer field → TEXT OpenELIS field (METADATA, ORDER)
     * 
     * @param mapping The mapping to validate
     * @throws LIMSRuntimeException if types are incompatible
     */
    private void validateTypeCompatibility(AnalyzerFieldMapping mapping) {
        AnalyzerField analyzerField = mapping.getAnalyzerField();
        if (analyzerField == null || analyzerField.getId() == null) {
            throw new LIMSRuntimeException("AnalyzerField must be set on mapping");
        }
        
        // Fetch analyzer field to get field type
        AnalyzerField field = analyzerFieldDAO.get(analyzerField.getId())
                .orElseThrow(() -> new LIMSRuntimeException("AnalyzerField not found: " + analyzerField.getId()));
        
        AnalyzerField.FieldType analyzerFieldType = field.getFieldType();
        AnalyzerFieldMapping.OpenELISFieldType openelisFieldType = mapping.getOpenelisFieldType();
        
        // Type compatibility rules
        if (analyzerFieldType == AnalyzerField.FieldType.NUMERIC) {
            // NUMERIC can map to TEST or RESULT (both can be numeric)
            if (openelisFieldType != AnalyzerFieldMapping.OpenELISFieldType.TEST
                    && openelisFieldType != AnalyzerFieldMapping.OpenELISFieldType.RESULT) {
                throw new LIMSRuntimeException(
                        "NUMERIC analyzer field can only map to TEST or RESULT OpenELIS fields. "
                                + "Attempted: " + openelisFieldType);
            }
        } else if (analyzerFieldType == AnalyzerField.FieldType.QUALITATIVE) {
            // QUALITATIVE can map to RESULT (coded values)
            if (openelisFieldType != AnalyzerFieldMapping.OpenELISFieldType.RESULT) {
                throw new LIMSRuntimeException(
                        "QUALITATIVE analyzer field can only map to RESULT OpenELIS fields. "
                                + "Attempted: " + openelisFieldType);
            }
        } else if (analyzerFieldType == AnalyzerField.FieldType.TEXT) {
            // TEXT can map to METADATA or ORDER
            if (openelisFieldType != AnalyzerFieldMapping.OpenELISFieldType.METADATA
                    && openelisFieldType != AnalyzerFieldMapping.OpenELISFieldType.ORDER) {
                throw new LIMSRuntimeException(
                        "TEXT analyzer field can only map to METADATA or ORDER OpenELIS fields. "
                                + "Attempted: " + openelisFieldType);
            }
        }
        // Other types (CONTROL_TEST, MELTING_POINT, DATE_TIME, CUSTOM) have flexible mapping
    }

    @Override
    @Transactional
    public AnalyzerFieldMapping updateMapping(AnalyzerFieldMapping mapping, boolean confirmed) {
        // Get existing mapping
        AnalyzerFieldMapping existingMapping = get(mapping.getId());
        if (existingMapping == null) {
            throw new LIMSRuntimeException("Mapping not found: " + mapping.getId());
        }
        
        // Validate type compatibility
        validateTypeCompatibility(mapping);
        
        // Check if analyzer is active and mapping is active - requires confirmation
        boolean analyzerIsActive = isAnalyzerActive(existingMapping);
        boolean mappingIsActive = existingMapping.getIsActive() != null && existingMapping.getIsActive();
        
        if (analyzerIsActive && mappingIsActive && !confirmed) {
            throw new LIMSRuntimeException(
                    "Confirmation required to update active mapping for active analyzer");
        }
        
        // Update mapping fields
        existingMapping.setOpenelisFieldId(mapping.getOpenelisFieldId());
        existingMapping.setOpenelisFieldType(mapping.getOpenelisFieldType());
        existingMapping.setMappingType(mapping.getMappingType());
        existingMapping.setIsRequired(mapping.getIsRequired());
        existingMapping.setIsActive(mapping.getIsActive());
        existingMapping.setSpecimenTypeConstraint(mapping.getSpecimenTypeConstraint());
        existingMapping.setPanelConstraint(mapping.getPanelConstraint());
        
        // Set audit fields (T075: who, when)
        if (mapping.getSysUserId() != null) {
            existingMapping.setSysUserId(mapping.getSysUserId());
        }
        existingMapping.setLastupdatedFields();
        
        // Note: Detailed audit trail (previous vs new values) can be added via AuditTrailService
        // if needed. BaseObject audit fields (sys_user_id, last_updated) provide basic audit trail.
        // To enable detailed audit trail, inject AuditTrailService and call:
        // auditTrailService.saveHistory(mapping, existingMapping, mapping.getSysUserId(),
        //     IActionConstants.AUDIT_TRAIL_UPDATE, getBaseObjectDAO().getTableName());
        
        return analyzerFieldMappingDAO.update(existingMapping);
    }

    @Override
    @Transactional
    public AnalyzerFieldMapping disableMapping(String mappingId, String retirementReason) {
        // Get existing mapping
        AnalyzerFieldMapping mapping = get(mappingId);
        if (mapping == null) {
            throw new LIMSRuntimeException("Mapping not found: " + mappingId);
        }
        
        // Cannot disable required mappings
        if (mapping.getIsRequired() != null && mapping.getIsRequired()) {
            throw new LIMSRuntimeException(
                    "Cannot disable required mapping. Required mappings (Sample ID, Test Code, Result Value) must remain active.");
        }
        
        // Set inactive
        mapping.setIsActive(false);
        // Set audit fields (T075: who, when)
        mapping.setLastupdatedFields();
        
        // Note: Retirement reason and detailed audit trail (previous vs new values) can be added
        // via AuditTrailService if needed. BaseObject audit fields (sys_user_id, last_updated)
        // provide basic audit trail for who and when. To enable detailed audit trail, inject
        // AuditTrailService and call:
        // auditTrailService.saveHistory(mapping, existingMapping, mapping.getSysUserId(),
        //     IActionConstants.AUDIT_TRAIL_UPDATE, getBaseObjectDAO().getTableName());
        
        return analyzerFieldMappingDAO.update(mapping);
    }

    /**
     * Check if analyzer is active by checking AnalyzerConfiguration
     * 
     * @param mapping The mapping to check analyzer status for
     * @return true if analyzer is active, false otherwise
     */
    private boolean isAnalyzerActive(AnalyzerFieldMapping mapping) {
        if (analyzerConfigurationService == null) {
            // If service not available, assume analyzer is not active (safe default)
            return false;
        }
        
        // Get analyzer field with analyzer relationship
        AnalyzerField field = analyzerFieldDAO.findByIdWithAnalyzer(mapping.getAnalyzerField().getId())
                .orElse(null);
        if (field == null || field.getAnalyzer() == null) {
            return false;
        }
        
        String analyzerId = field.getAnalyzer().getId();
        Optional<AnalyzerConfiguration> configOpt = analyzerConfigurationService.getByAnalyzerId(analyzerId);
        
        if (configOpt.isPresent()) {
            AnalyzerConfiguration config = configOpt.get();
            // Check if analyzer is active (via Analyzer.active field)
            // Note: Analyzer.active is a boolean field in the legacy Analyzer entity
            return config.getAnalyzer() != null && config.getAnalyzer().isActive();
        }
        
        return false;
    }
}

