package org.openelisglobal.qc.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCAlert;

/**
 * DAO interface for QCAlert entity operations.
 */
public interface QCAlertDAO extends BaseDAO<QCAlert, String> {

    /**
     * Get all alerts for a specific violation.
     */
    List<QCAlert> findByViolation(String violationId) throws LIMSRuntimeException;

    /**
     * Get alerts for a specific user (recipient).
     */
    List<QCAlert> findByRecipient(Integer userId) throws LIMSRuntimeException;

    /**
     * Get unread alerts for a specific user.
     */
    List<QCAlert> findUnreadByRecipient(Integer userId) throws LIMSRuntimeException;

    /**
     * Get alerts by read status.
     */
    List<QCAlert> findByReadStatus(Boolean readStatus) throws LIMSRuntimeException;
}
