package org.openelisglobal.qc.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCResult;

/**
 * DAO interface for QCResult entity operations.
 */
public interface QCResultDAO extends BaseDAO<QCResult, String> {

    /**
     * Get all results for a specific control lot.
     */
    List<QCResult> findByControlLot(String controlLotId) throws LIMSRuntimeException;

    /**
     * Get historical results for rule evaluation (ordered by run date).
     */
    List<QCResult> findHistoricalForRule(String controlLotId, int limit) throws LIMSRuntimeException;

    /**
     * Get results by instrument and date range.
     */
    List<QCResult> findByInstrumentAndDateRange(Integer instrumentId, Timestamp startDate, Timestamp endDate)
            throws LIMSRuntimeException;

    /**
     * Get latest N results for a control lot.
     */
    List<QCResult> findLatestByControlLot(String controlLotId, int limit) throws LIMSRuntimeException;

    /**
     * Get all results for a control lot ordered by run date ascending (oldest
     * first). Used for Westgard rule evaluation.
     */
    List<QCResult> findByControlLotIdOrderByRunDateTime(String controlLotId) throws LIMSRuntimeException;

    /**
     * Get results by control lot and date range for chart display.
     */
    List<QCResult> findByControlLotAndDateRange(String controlLotId, Timestamp startDate, Timestamp endDate)
            throws LIMSRuntimeException;
}
