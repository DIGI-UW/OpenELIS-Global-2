package org.openelisglobal.qc.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCRuleViolation;

/**
 * DAO interface for QCRuleViolation entity operations.
 */
public interface QCRuleViolationDAO extends BaseDAO<QCRuleViolation, String> {

    /**
     * Get all violations for a specific instrument.
     */
    List<QCRuleViolation> findByInstrument(Integer instrumentId) throws LIMSRuntimeException;

    /**
     * Get unresolved violations.
     */
    List<QCRuleViolation> findUnresolved() throws LIMSRuntimeException;

    /**
     * Get violations by severity.
     */
    List<QCRuleViolation> findBySeverity(String severity) throws LIMSRuntimeException;

    /**
     * Get violations by instrument and date range.
     */
    List<QCRuleViolation> findByInstrumentAndDateRange(Integer instrumentId, Timestamp startDate, Timestamp endDate)
            throws LIMSRuntimeException;

    /**
     * Get unresolved violations for a specific instrument.
     */
    List<QCRuleViolation> findUnresolvedByInstrument(Integer instrumentId) throws LIMSRuntimeException;

    /**
     * Get violations for a specific triggering QC result.
     */
    List<QCRuleViolation> findByTriggeringResultId(String triggeringResultId) throws LIMSRuntimeException;
}
