package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for AnalyzerField operations
 * 
 * Provides business logic for managing analyzer fields with validation: -
 * NUMERIC fields must have unit - QUALITATIVE/TEXT fields must not have unit
 */
@Service
@Transactional
public class AnalyzerFieldServiceImpl extends BaseObjectServiceImpl<AnalyzerField, String>
        implements AnalyzerFieldService {

    private final AnalyzerFieldDAO analyzerFieldDAO;

    @Autowired
    public AnalyzerFieldServiceImpl(AnalyzerFieldDAO analyzerFieldDAO) {
        super(AnalyzerField.class);
        this.analyzerFieldDAO = analyzerFieldDAO;
    }

    @Override
    protected BaseDAO<AnalyzerField, String> getBaseObjectDAO() {
        return analyzerFieldDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerField> getFieldsByAnalyzerId(String analyzerId) {
        return analyzerFieldDAO.findByAnalyzerId(analyzerId);
    }

    @Override
    @Transactional
    public String createField(AnalyzerField field) {
        // Validate field type and unit compatibility
        validateFieldTypeAndUnit(field);

        return analyzerFieldDAO.insert(field);
    }

    /**
     * Validate that field type and unit are compatible
     * 
     * Rules: - NUMERIC fields MUST have unit - QUALITATIVE, TEXT, DATE_TIME,
     * CONTROL_TEST, MELTING_POINT, CUSTOM fields MUST NOT have unit
     * 
     * @param field The field to validate
     * @throws LIMSRuntimeException if validation fails
     */
    private void validateFieldTypeAndUnit(AnalyzerField field) {
        AnalyzerField.FieldType fieldType = field.getFieldType();
        String unit = field.getUnit();

        if (fieldType == AnalyzerField.FieldType.NUMERIC) {
            if (unit == null || unit.trim().isEmpty()) {
                throw new LIMSRuntimeException("NUMERIC field type requires a unit. Field: " + field.getFieldName());
            }
        } else {
            // QUALITATIVE, TEXT, DATE_TIME, CONTROL_TEST, MELTING_POINT, CUSTOM
            if (unit != null && !unit.trim().isEmpty()) {
                throw new LIMSRuntimeException("Non-NUMERIC field type (" + fieldType
                        + ") must not have a unit. Field: " + field.getFieldName());
            }
        }
    }
}
