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
    public void processQCResult(String analyzerId, String testId, String accessionNumber, String lotNumber,
            String controlLevel, BigDecimal resultValue, String unit, LocalDateTime timestamp) {

        if (testId == null || analyzerId == null || accessionNumber == null) {
            LogEvent.logWarn(CLASS_NAME, "processQCResult", "Skipping QC processing — missing required field: testId="
                    + testId + " analyzerId=" + analyzerId + " accession=" + accessionNumber);
            return;
        }

        // analyzerId and testId are already String — they match the
        // String-typed instrumentId/testId on QCControlLot (bridged to
        // NUMERIC SQL via LIMSStringNumberUserType). No parsing needed.
        QCControlLot lot = findMatchingControlLot(accessionNumber, lotNumber, controlLevel, testId, analyzerId);
        if (lot == null) {
            LogEvent.logError(CLASS_NAME, "processQCResult",
                    "No matching QC control lot for accession=" + accessionNumber + " lotNumber=" + lotNumber
                            + " controlLevel=" + controlLevel + " testId=" + testId + " instrumentId=" + analyzerId
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
     * Three-tier resolution. Each tier consumes metadata that the bridge propagated
     * upstream — no guessing, no substring heuristics:
     *
     * <ol>
     * <li><b>Tier 1 — explicit lot match</b>: bridge extracted a canonical lot
     * identifier (ASTM Q-segment field 3 component 2, or future FILE profile
     * mappings). Equality match on {@code lotNumber}.</li>
     * <li><b>Tier 2 — level match</b>: bridge surfaced a control level (ASTM
     * Q-segment field 3 component 3, or matched FILE qcRule SPECIMEN_ID_PREFIX
     * operand like "LPC"/"HPC"/"CNEG"). Equality match on {@code controlLevel} for
     * the (test, instrument). Handles the normal multi-level QC case where labs run
     * LPC + HPC simultaneously.</li>
     * <li><b>Tier 3 — single-lot fallback</b>: backwards-compat for pre-extension
     * bundles or ad-hoc registrations with exactly one ACTIVE lot on (test,
     * instrument).</li>
     * </ol>
     *
     * <p>
     * Returns {@code null} when no tier matches. Caller logs the error including
     * all metadata so a clinician can identify which lot was expected.
     */
    private QCControlLot findMatchingControlLot(String accessionNumber, String lotNumber, String controlLevel,
            String testId, String instrumentId) {
        List<QCControlLot> lots = controlLotDAO.getByTestAndInstrument(testId, instrumentId);

        // Tier 1: explicit lot_number from FHIR extension OR from accession
        // when operator encoded it in specimen ID (BioRad-Lot-12345 pattern)
        if (lotNumber != null && !lotNumber.isEmpty()) {
            for (QCControlLot lot : lots) {
                if (isUsable(lot) && lotNumber.equals(lot.getLotNumber())) {
                    LogEvent.logInfo(CLASS_NAME, "findMatchingControlLot", "Tier 1: explicit lot match '" + lotNumber
                            + "' for testId=" + testId + " instrumentId=" + instrumentId);
                    return lot;
                }
            }
        }
        for (QCControlLot lot : lots) {
            if (isUsable(lot) && accessionNumber.equals(lot.getLotNumber())) {
                LogEvent.logInfo(CLASS_NAME, "findMatchingControlLot", "Tier 1: accession-as-lot match '"
                        + accessionNumber + "' for testId=" + testId + " instrumentId=" + instrumentId);
                return lot;
            }
        }

        // Tier 2: level match — controlLevel from FHIR extension picks the
        // right lot when (testId, instrumentId) has multiple usable lots
        // (the normal multi-level QC case: LPC + HPC simultaneously). The
        // schema does not enforce uniqueness on (test, instrument, controlLevel)
        // so we require exactly-one match; ambiguity returns null + logWarn
        // rather than picking the first by DAO order. Uses isUsable so
        // ESTABLISHMENT lots resolve deterministically when the bridge
        // surfaces an explicit controlLevel (consistent with Tier 1).
        if (controlLevel != null && !controlLevel.trim().isEmpty()) {
            String trimmedLevel = controlLevel.trim();
            List<QCControlLot> levelMatches = lots.stream()
                    .filter(l -> isUsable(l) && trimmedLevel.equalsIgnoreCase(l.getControlLevel())).toList();
            if (levelMatches.size() == 1) {
                QCControlLot match = levelMatches.get(0);
                LogEvent.logInfo(CLASS_NAME, "findMatchingControlLot",
                        "Tier 2: level match controlLevel='" + trimmedLevel + "' → lot '" + match.getLotNumber()
                                + "' for testId=" + testId + " instrumentId=" + instrumentId);
                return match;
            }
            if (levelMatches.size() > 1) {
                String matchedLotNumbers = levelMatches.stream().map(QCControlLot::getLotNumber)
                        .reduce((a, b) -> a + ", " + b).orElse("");
                LogEvent.logWarn(CLASS_NAME, "findMatchingControlLot",
                        "Tier 2: ambiguous level match controlLevel='" + trimmedLevel + "' for testId=" + testId
                                + " instrumentId=" + instrumentId + " — " + levelMatches.size()
                                + " usable lots share this level [" + matchedLotNumbers
                                + "]; cannot disambiguate, returning null.");
                return null;
            }
        }

        // Tier 3: single-ACTIVE-lot fallback (backwards-compat / pre-extension bundles)
        List<QCControlLot> activeLots = lots.stream().filter(l -> "ACTIVE".equals(l.getStatus())).toList();
        if (activeLots.size() == 1) {
            LogEvent.logInfo(CLASS_NAME, "findMatchingControlLot",
                    "Tier 3: single-ACTIVE-lot fallback — using '" + activeLots.get(0).getLotNumber()
                            + "' for accession=" + accessionNumber + " testId=" + testId + " instrumentId="
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
                    "Multiple ACTIVE lots for testId=" + testId + " instrumentId=" + instrumentId + " (count="
                            + activeLots.size() + "); bridge did not propagate lotNumber/"
                            + "controlLevel — cannot disambiguate.");
        }

        return null;
    }

    private static boolean isUsable(QCControlLot lot) {
        String status = lot.getStatus();
        return "ACTIVE".equals(status) || "ESTABLISHMENT".equals(status);
    }
}
