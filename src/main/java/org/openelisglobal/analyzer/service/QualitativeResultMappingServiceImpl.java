package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.dao.QualitativeResultMappingDAO;
import org.openelisglobal.analyzer.valueholder.QualitativeResultMapping;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for QualitativeResultMapping operations
 * 
 * Provides business logic for managing qualitative result mappings with: -
 * Many-to-one mapping support - Duplicate value validation
 */
@Service
@Transactional
public class QualitativeResultMappingServiceImpl extends BaseObjectServiceImpl<QualitativeResultMapping, String>
        implements QualitativeResultMappingService {

    private final QualitativeResultMappingDAO qualitativeResultMappingDAO;

    @Autowired
    public QualitativeResultMappingServiceImpl(QualitativeResultMappingDAO qualitativeResultMappingDAO) {
        super(QualitativeResultMapping.class);
        this.qualitativeResultMappingDAO = qualitativeResultMappingDAO;
    }

    @Override
    protected BaseDAO<QualitativeResultMapping, String> getBaseObjectDAO() {
        return qualitativeResultMappingDAO;
    }

    @Override
    @Transactional
    public String createMapping(QualitativeResultMapping mapping) {
        // Validate no duplicate analyzer_value for same analyzer_field_id
        validateNoDuplicateValue(mapping);

        return qualitativeResultMappingDAO.insert(mapping);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualitativeResultMapping> getMappingsByAnalyzerFieldId(String analyzerFieldId) {
        return qualitativeResultMappingDAO.findByAnalyzerFieldId(analyzerFieldId);
    }

    /**
     * Validate that no duplicate analyzer_value exists for the same
     * analyzer_field_id
     * 
     * Unique constraint: (analyzer_field_id, analyzer_value)
     * 
     * @param mapping The mapping to validate
     * @throws LIMSRuntimeException if duplicate value exists
     */
    private void validateNoDuplicateValue(QualitativeResultMapping mapping) {
        if (mapping.getAnalyzerFieldId() == null || mapping.getAnalyzerFieldId().trim().isEmpty()) {
            throw new LIMSRuntimeException("analyzerFieldId must be set on mapping");
        }

        if (mapping.getAnalyzerValue() == null || mapping.getAnalyzerValue().trim().isEmpty()) {
            throw new LIMSRuntimeException("AnalyzerValue is required");
        }

        String analyzerFieldId = mapping.getAnalyzerFieldId().trim();
        String analyzerValue = mapping.getAnalyzerValue().trim();

        // Check for existing mappings with same analyzer_field_id and analyzer_value
        List<QualitativeResultMapping> existingMappings = qualitativeResultMappingDAO
                .findByAnalyzerFieldId(analyzerFieldId);

        boolean duplicateExists = existingMappings.stream()
                .anyMatch(m -> analyzerValue.equalsIgnoreCase(m.getAnalyzerValue())
                        && (mapping.getId() == null || !mapping.getId().equals(m.getId())));

        if (duplicateExists) {
            throw new LIMSRuntimeException("Duplicate analyzer value '" + analyzerValue
                    + "' already exists for analyzer field: " + analyzerFieldId);
        }
    }
}
