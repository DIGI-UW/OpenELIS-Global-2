package org.openelisglobal.batchworkplan.form;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.batchworkplan.valueholder.BatchQcStatus;
import org.openelisglobal.qc.valueholder.QCFrequencyType;

/**
 * The QC standing of one control lot behind a batch. {@code bench} lots take an
 * inline QC entry through the bench capture endpoint; an analyzer's lots are
 * fed by the analyzer, so {@code canRecordInline} is false for them.
 */
public record ControlLotQcResponse(String controlLotId, String lotNumber, String productName, String manufacturer,
        String controlLevel, String testId, String testName, String testSectionId, String instrumentId, boolean bench,
        String lotStatus, Timestamp expirationDate, QCFrequencyType frequencyType, Integer frequencyHours,
        Integer shiftDefinitionHours, boolean qcRequired, BatchQcStatus status, Timestamp lastPassingAt,
        Timestamp nextDueAt, boolean evaluationPending, boolean canRecordInline, List<String> ruleCodes,
        List<String> openViolationIds, QcRunResponse lastRun, List<QcRunResponse> recentRuns) {
}
