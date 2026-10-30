package org.openelisglobal.analyzer.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qc.dao.QCControlLotDAO;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.testcatalog.service.TestQcTargetService;
import org.openelisglobal.testcatalog.valueholder.TestQcTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records a control only when its specimen identifier or reported level
 * identifies a usable lot.
 */
@Service
@Transactional
public class QCResultProcessingServiceImpl implements QCResultProcessingService {

    private static final String CLASS_NAME = "QCResultProcessingServiceImpl";

    @Autowired
    private QCResultService qcResultService;

    @Autowired
    private QCControlLotDAO controlLotDAO;

    @Autowired
    private TestQcTargetService testQcTargetService;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Outcome processQCResult(String analyzerId, String testId, String accessionNumber, String controlLevel,
            BigDecimal resultValue, String unit, LocalDateTime timestamp) {

        if (testId == null || analyzerId == null || accessionNumber == null) {
            LogEvent.logWarn(CLASS_NAME, "processQCResult", "Skipping QC processing — missing required field: testId="
                    + testId + " analyzerId=" + analyzerId + " accession=" + accessionNumber);
            return Outcome.NO_LOT;
        }

        // analyzerId and testId are already String — they match the
        // String-typed instrumentId/testId on QCControlLot (bridged to
        // NUMERIC SQL via LIMSStringNumberUserType). No parsing needed.
        QCControlLot lot = findMatchingControlLot(accessionNumber, controlLevel, testId, analyzerId);
        if (lot == null) {
            LogEvent.logError(CLASS_NAME, "processQCResult",
                    "No matching QC control lot for accession=" + accessionNumber + " controlLevel=" + controlLevel
                            + " testId=" + testId + " instrumentId=" + analyzerId
                            + " — create an ACTIVE control lot for this test+instrument via the QC dashboard");
            return Outcome.NO_LOT;
        }

        // Persistence failures must roll back the caller's result staging and delivery
        // receipt too.
        qcResultService.createQCResult(analyzerId, testId, lot.getId(), lot.getControlLevel(), resultValue, unit,
                timestamp);
        LogEvent.logInfo(CLASS_NAME, "processQCResult",
                "QC result created for lot=" + lot.getLotNumber() + " test=" + testId + " instrument=" + analyzerId);
        return Outcome.RECORDED;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Outcome processQualitativeQCResult(String analyzerId, String testId, String componentId,
            String accessionNumber, String controlLevel, String answerDictionaryId, LocalDateTime timestamp) {
        QCControlLot lot = findMatchingControlLot(accessionNumber, controlLevel, testId, analyzerId);
        if (lot == null) {
            LogEvent.logError(CLASS_NAME, "processQualitativeQCResult",
                    "No matching QC control lot for accession=" + accessionNumber + " controlLevel=" + controlLevel
                            + " testId=" + testId + " instrumentId=" + analyzerId);
            return Outcome.NO_LOT;
        }
        TestQcTarget target = testQcTargetService.resolveEffectiveTarget(testId, componentId, lot.getControlLevel(),
                lot.getId());
        if (target == null || target.getExpectedDictResultId() == null) {
            return Outcome.NO_TARGET;
        }
        QCQualitativeOutcome outcome = target.getExpectedDictResultId().equals(answerDictionaryId)
                ? QCQualitativeOutcome.PASS
                : QCQualitativeOutcome.FAIL;
        qcResultService.createAnalyzerQualitativeQCResult(analyzerId, testId, lot.getId(), outcome, timestamp);
        return Outcome.RECORDED;
    }

    /**
     * Exact specimen matches take precedence; otherwise a reported level must
     * identify one usable lot.
     */
    private QCControlLot findMatchingControlLot(String specimenId, String controlLevel, String testId,
            String instrumentId) {
        if (testId == null || instrumentId == null || specimenId == null) {
            return null;
        }
        List<QCControlLot> lots = controlLotDAO.getByTestAndInstrument(testId, instrumentId).stream()
                .filter(QCResultProcessingServiceImpl::isUsable).toList();
        List<QCControlLot> exact = lots.stream().filter(lot -> specimenId.equals(lot.getLotNumber())).toList();
        if (!exact.isEmpty()) {
            // An explicit specimen identity must not be redirected to another lot when its
            // level conflicts.
            List<QCControlLot> matching = exact.stream().filter(lot -> controlLevel == null || controlLevel.isBlank()
                    || controlLevel.trim().equalsIgnoreCase(lot.getControlLevel())).toList();
            return matching.size() == 1 ? matching.get(0) : null;
        }
        if (controlLevel == null || controlLevel.isBlank()) {
            return null;
        }
        List<QCControlLot> matching = lots.stream()
                .filter(lot -> controlLevel.trim().equalsIgnoreCase(lot.getControlLevel())).toList();
        return matching.size() == 1 ? matching.get(0) : null;
    }

    private static boolean isUsable(QCControlLot lot) {
        String status = lot.getStatus();
        return "ACTIVE".equals(status) || "ESTABLISHMENT".equals(status);
    }
}
