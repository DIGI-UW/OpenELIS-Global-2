package org.openelisglobal.qc.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qc.valueholder.QCResult;

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
}
