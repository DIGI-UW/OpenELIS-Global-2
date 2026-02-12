package org.openelisglobal.analyzer.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hydrator for AnalyzerFieldMapping entities.
 * 
 * Manually loads and sets related entities (AnalyzerField, Analyzer) on
 * AnalyzerFieldMapping objects. This avoids Hibernate's relationship management
 * which has issues when XML-mapped entities reference annotation-based
 * entities.
 * 
 * Usage: After loading mappings from DAO, call hydrator methods to populate
 * transient relationship fields.
 */
@Component
public class AnalyzerFieldMappingHydrator {

    @Autowired
    private AnalyzerFieldDAO analyzerFieldDAO;

    /**
     * Hydrate a single mapping with its AnalyzerField (and Analyzer via field).
     * 
     * @param mapping The mapping to hydrate
     */
    @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
    public void hydrateAnalyzerField(AnalyzerFieldMapping mapping) {
        if (mapping == null || mapping.getAnalyzerFieldId() == null) {
            return;
        }

        analyzerFieldDAO.findByIdWithAnalyzer(mapping.getAnalyzerFieldId()).ifPresent(field -> {
            mapping.setAnalyzerField(field);
            // Also set analyzer from field if available
            if (field.getAnalyzer() != null) {
                mapping.setAnalyzer(field.getAnalyzer());
            }
        });
    }

    /**
     * Hydrate a list of mappings with their AnalyzerFields (and Analyzers via
     * fields). Efficiently batches field loading to minimize database queries.
     * 
     * @param mappings The list of mappings to hydrate
     */
    @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
    public void hydrateAnalyzerFields(List<AnalyzerFieldMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return;
        }

        // Collect unique analyzer field IDs
        Set<String> fieldIds = mappings.stream().map(AnalyzerFieldMapping::getAnalyzerFieldId).filter(id -> id != null)
                .collect(Collectors.toSet());

        if (fieldIds.isEmpty()) {
            return;
        }

        // Load all fields in batch
        Map<String, AnalyzerField> fieldMap = new HashMap<>();
        for (String fieldId : fieldIds) {
            analyzerFieldDAO.findByIdWithAnalyzer(fieldId).ifPresent(field -> fieldMap.put(fieldId, field));
        }

        // Set fields on mappings
        for (AnalyzerFieldMapping mapping : mappings) {
            if (mapping.getAnalyzerFieldId() != null) {
                AnalyzerField field = fieldMap.get(mapping.getAnalyzerFieldId());
                if (field != null) {
                    mapping.setAnalyzerField(field);
                    // Also set analyzer from field if available
                    if (field.getAnalyzer() != null) {
                        mapping.setAnalyzer(field.getAnalyzer());
                    }
                }
            }
        }
    }

    /**
     * Load AnalyzerFields for a set of field IDs and return as a map. Useful for
     * service layer when it needs to access fields by ID.
     * 
     * @param fieldIds The set of analyzer field IDs to load
     * @return Map of field ID to AnalyzerField
     */
    @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
    public Map<String, AnalyzerField> loadAnalyzerFields(Set<String> fieldIds) {
        Map<String, AnalyzerField> fieldMap = new HashMap<>();
        if (fieldIds == null || fieldIds.isEmpty()) {
            return fieldMap;
        }

        for (String fieldId : fieldIds) {
            analyzerFieldDAO.findByIdWithAnalyzer(fieldId).ifPresent(field -> fieldMap.put(fieldId, field));
        }

        return fieldMap;
    }
}
