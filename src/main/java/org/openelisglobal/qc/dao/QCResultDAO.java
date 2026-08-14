package org.openelisglobal.qc.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCSource;

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
    List<QCResult> findByInstrumentAndDateRange(String instrumentId, Timestamp startDate, Timestamp endDate)
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

    /**
     * Get latest N results for a specific instrument and test, ordered by run date
     * descending.
     */
    List<QCResult> findLatestByInstrumentAndTest(String instrumentId, String testId, int limit)
            throws LIMSRuntimeException;

    /**
     * Get all distinct instrument IDs that have QC results.
     */
    List<String> findDistinctInstrumentIds() throws LIMSRuntimeException;

    /**
     * Get the most recent accepted (in-control) result for an instrument and test
     * strictly before the given time. Bounds the affected-samples window when a
     * violation auto-creates an NCE. At most one result is returned.
     */
    List<QCResult> findLatestAcceptedBefore(String instrumentId, String testId, Timestamp before)
            throws LIMSRuntimeException;

    /**
     * The bench counterpart of {@link #findLatestAcceptedBefore}: the most recent
     * in-control MANUAL or RDT result for a test in a lab unit, strictly before the
     * given time. Bounds the affected-analysis window for a failed bench control,
     * which has no analyzer to key on (OGC-1147 FR-C1). At most one result.
     */
    List<QCResult> findLatestAcceptedBenchResultBefore(String testSectionId, String testId, Timestamp before)
            throws LIMSRuntimeException;

    /**
     * Bench QC activity for a window, grouped by lab unit and test (OGC-1147
     * FR-D1). Returns {testSectionId, testId, source, totalRuns, failedRuns,
     * lastRun}. A null {@code source} covers both MANUAL and RDT.
     */
    List<Object[]> summariseBenchQc(Timestamp startDate, Timestamp endDate, QCSource source)
            throws LIMSRuntimeException;

    /**
     * Flat list of bench control runs in a window, newest first, capped at
     * {@code maxRows} (OGC-1147 FR-D5). A null {@code source} covers MANUAL and
     * RDT.
     */
    List<QCResult> findBenchResults(Timestamp startDate, Timestamp endDate, QCSource source, int maxRows)
            throws LIMSRuntimeException;
}
