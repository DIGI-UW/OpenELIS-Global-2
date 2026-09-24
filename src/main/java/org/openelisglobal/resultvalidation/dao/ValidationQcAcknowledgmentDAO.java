package org.openelisglobal.resultvalidation.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.resultvalidation.valueholder.ValidationQcAcknowledgment;

/**
 * DAO interface for ValidationQcAcknowledgment entity operations.
 */
public interface ValidationQcAcknowledgmentDAO extends BaseDAO<ValidationQcAcknowledgment, Integer> {

    /**
     * Find all acknowledgments for a specific analysis.
     */
    List<ValidationQcAcknowledgment> findByAnalysisId(Integer analysisId) throws LIMSRuntimeException;
}
