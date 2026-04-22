package org.openelisglobal.analyzer.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qc.dao.QCControlLotDAO;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes QC results received via the FHIR import pipeline.
 *
 * <p>
 * Resolves the {@link QCControlLot} for a QC observation using a two-tier
 * strategy: (1) strict match — specimen accession equals a lot's lotNumber, (2)
 * fallback — if no strict match, use the single ACTIVE lot for the (test,
 * instrument) pair. This follows the design spec's
 * {@code getActiveControlLot(testId, instrumentId, level)} pattern while
 * remaining compatible with labs that encode lot numbers in specimen IDs.
 *
 * <p>
 * If a matching lot is found, delegates to
 * {@link QCResultService#createQCResult} which persists the result, calculates
 * the z-score, and publishes a {@code QCResultCreatedEvent} for async Westgard
 * rule evaluation.
 *
 * <p>
 * If no matching lot is found (zero active lots or multiple ambiguous lots),
 * logs an ERROR so the failure is visible in monitoring.
 */
@Service
@Transactional
public class QCResultProcessingServiceImpl implements QCResultProcessingService {

    private static final String CLASS_NAME = "QCResultProcessingServiceImpl";

    @Autowired
    private QCResultService qcResultService;

    @Autowired
    private QCControlLotDAO controlLotDAO;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void processQCResult(String analyzerId, String testId, String accessionNumber, BigDecimal resultValue,
            String unit, LocalDateTime timestamp) {

        if (testId == null || analyzerId == null || accessionNumber == null) {
            LogEvent.logWarn(CLASS_NAME, "processQCResult", "Skipping QC processing — missing required field: testId="
                    + testId + " analyzerId=" + analyzerId + " accession=" + accessionNumber);
            return;
        }

        Integer instrumentId;
        Integer testIdInt;
        try {
            instrumentId = Integer.valueOf(analyzerId);
            testIdInt = Integer.valueOf(testId);
        } catch (NumberFormatException e) {
            LogEvent.logWarn(CLASS_NAME, "processQCResult",
                    "Cannot parse analyzerId or testId as integer: analyzerId=" + analyzerId + " testId=" + testId);
            return;
        }

        QCControlLot lot = findMatchingControlLot(accessionNumber, testIdInt, instrumentId);
        if (lot == null) {
            LogEvent.logError(CLASS_NAME, "processQCResult",
                    "No matching QC control lot for accession=" + accessionNumber + " testId=" + testId
                            + " instrumentId=" + analyzerId
                            + " — create an ACTIVE control lot for this test+instrument via the QC dashboard");
            return;
        }

        try {
            qcResultService.createQCResult(analyzerId, testId, lot.getId(), lot.getControlLevel(), resultValue, unit,
                    timestamp);

            LogEvent.logInfo(CLASS_NAME, "processQCResult", "QC result created for lot=" + lot.getLotNumber() + " test="
                    + testId + " instrument=" + analyzerId);
        } catch (Exception e) {
            LogEvent.logError(CLASS_NAME, "processQCResult", "Failed to create QC result for lot=" + lot.getLotNumber()
                    + " test=" + testId + ": " + e.getMessage());
            // Don't rethrow — the staging AnalyzerResult is still persisted,
            // and QC processing failure should not block analyzer import.
        }
    }

    /**
     * Find a control lot for the given QC observation.
     *
     * <p>
     * Two-tier resolution strategy per the design spec
     * ({@code getActiveControlLot(testId, instrumentId, level)}):
     *
     * <ol>
     * <li><b>Strict match</b>: specimen accession equals a lot's {@code lotNumber}.
     * Supports labs that encode the lot number in the specimen ID (e.g.,
     * "BioRad-Lot-12345").</li>
     * <li><b>Fallback</b>: if no strict match, and exactly one ACTIVE lot exists
     * for {@code (testId, instrumentId)}, use it. This covers FILE analyzers where
     * the specimen ID is a per-run identifier (CNEG001, NTC001) unrelated to the
     * lot number.</li>
     * </ol>
     *
     * <p>
     * Returns {@code null} if zero or multiple ACTIVE lots exist without a strict
     * match (ambiguous — requires manual lot selection or level-based
     * disambiguation in a future milestone).
     */
    private QCControlLot findMatchingControlLot(String accessionNumber, Integer testId, Integer instrumentId) {
        List<QCControlLot> lots = controlLotDAO.getByTestAndInstrument(testId, instrumentId);

        // Tier 1: strict match — accession number equals lot number
        for (QCControlLot lot : lots) {
            String status = lot.getStatus();
            if (("ACTIVE".equals(status) || "ESTABLISHMENT".equals(status))
                    && accessionNumber.equals(lot.getLotNumber())) {
                return lot;
            }
        }

        // Tier 2: fallback — single ACTIVE lot for (test, instrument)
        List<QCControlLot> activeLots = lots.stream().filter(l -> "ACTIVE".equals(l.getStatus())).toList();
        if (activeLots.size() == 1) {
            LogEvent.logInfo(CLASS_NAME, "findMatchingControlLot",
                    "No strict lot-number match for accession=" + accessionNumber + "; using single ACTIVE lot '"
                            + activeLots.get(0).getLotNumber() + "' for testId=" + testId + " instrumentId="
                            + instrumentId);
            return activeLots.get(0);
        }

        // Also check ESTABLISHMENT lots (single = use it for data accumulation)
        List<QCControlLot> establishmentLots = lots.stream().filter(l -> "ESTABLISHMENT".equals(l.getStatus()))
                .toList();
        if (activeLots.isEmpty() && establishmentLots.size() == 1) {
            LogEvent.logInfo(CLASS_NAME, "findMatchingControlLot",
                    "No active lot; using single ESTABLISHMENT lot '" + establishmentLots.get(0).getLotNumber()
                            + "' for data accumulation (testId=" + testId + " instrumentId=" + instrumentId + ")");
            return establishmentLots.get(0);
        }

        if (activeLots.size() > 1) {
            LogEvent.logWarn(CLASS_NAME, "findMatchingControlLot",
                    "Multiple ACTIVE lots for testId=" + testId + " instrumentId=" + instrumentId
                            + " — cannot resolve without level-based matching. Lot count: " + activeLots.size());
        }

        return null;
    }
}
