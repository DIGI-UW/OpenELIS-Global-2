package org.openelisglobal.qc.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCCorrectiveAction;

/**
 * DAO interface for QCCorrectiveAction entity operations.
 */
public interface QCCorrectiveActionDAO extends BaseDAO<QCCorrectiveAction, String> {

    /**
     * Get all corrective actions for a specific violation.
     */
    List<QCCorrectiveAction> findByViolation(String violationId) throws LIMSRuntimeException;

    /**
     * Get corrective actions assigned to a specific user.
     */
    List<QCCorrectiveAction> findByAssignedUser(Integer userId) throws LIMSRuntimeException;

    /**
     * Get corrective actions by status.
     */
    List<QCCorrectiveAction> findByStatus(String status) throws LIMSRuntimeException;

    /**
     * Get pending corrective actions assigned to a user.
     */
    List<QCCorrectiveAction> findPendingByAssignedUser(Integer userId) throws LIMSRuntimeException;
}
