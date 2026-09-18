package org.openelisglobal.qc.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCSource;

/**
 * Service interface for QC Result management. Supports User Story 8:
 * Integration with Instrument Interfaces
 *
 * Primary use case: Feature 004 calls createQCResult() after parsing ASTM
 * Q-segments
 */
public interface QCResultService extends BaseObjectService<QCResult, String> {

    /**
     * Create a QC result from analyzer data (called by Feature 004 ASTM interface
     * or manual entry).
     *
     * This method: 1. Retrieves control lot and statistics 2. Calculates z-score
     * using formula: (value - mean) / standard deviation 3. Persists QCResult
     * entity 4. Publishes QCResultCreatedEvent for async rule evaluation 5. Returns
     * created entity
     *
     * @param analyzerId   The instrument/analyzer ID
     * @param testId       The test ID (e.g., "Glucose")
     * @param controlLotId The control lot ID
     * @param controlLevel The control level (LOW, NORMAL, HIGH)
     * @param resultValue  The measured QC value
     * @param unit         The unit of measure
     * @param timestamp    The run date/time
     * @return The created QCResult with calculated z-score
     * @throws IllegalArgumentException if control lot not found or not ACTIVE, or
     *                                  if result value is null/invalid
     */
    QCResult createQCResult(String analyzerId, String testId, String controlLotId, String controlLevel,
            BigDecimal resultValue, String unit, LocalDateTime timestamp) throws IllegalArgumentException;

    /**
     * Most recent accepted (in-control) result for an instrument and test strictly
     * before the given time; empty if none. Bounds the affected-samples window for
     * Westgard auto-created NCEs (OGC-728). At most one element.
     */
    List<QCResult> findLatestAcceptedBefore(String instrumentId, String testId, Timestamp before);

    /**
     * Bench counterpart of {@link #findLatestAcceptedBefore}, keyed by lab unit
     * because a manual or RDT control has no analyzer (OGC-1147). At most one
     * element.
     */
    List<QCResult> findLatestAcceptedBenchResultBefore(String testSectionId, String testId, Timestamp before);

    /**
     * Record a bench control run — an RDT control line or a manual quantitative
     * control (OGC-1147). The non-analyzer counterpart to
     * {@link #createQCResult(String, String, String, String, BigDecimal, String, LocalDateTime)}:
     * no instrument, the real session user rather than the automation user, and a
     * qualitative outcome alongside (or instead of) a number.
     *
     * <p>
     * A manual quantitative run against a lot with statistics gets a z-score
     * exactly as an analyzer result does, so it plots on Levey-Jennings and is
     * evaluated by the Westgard engine with no further wiring. An RDT run has no
     * number, therefore no z-score, therefore no rule evaluation — exactly the
     * intended split, enforced by arithmetic rather than a branch.
     *
     * @param capture   the control run as entered at the bench
     * @param sysUserId the acting technician's system user id — never the
     *                  automation user
     * @return the persisted result, with z-score when one could be computed
     * @throws IllegalArgumentException if the source/value/outcome combination is
     *                                  illegal, or the referenced control lot is
     *                                  missing or not usable
     */
    QCResult createBenchQCResult(BenchQCCaptureForm capture, int sysUserId) throws IllegalArgumentException;

    /**
     * Flat list of bench control runs in a window for the accreditation export
     * (OGC-1147). A null {@code source} covers MANUAL and RDT.
     */
    List<QCResult> findBenchResults(Timestamp startDate, Timestamp endDate, QCSource source, int maxRows);
}
