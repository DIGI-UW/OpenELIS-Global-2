package org.openelisglobal.batchworkplan.dao;

import java.util.List;
import java.util.Map;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;

/**
 * The QC reads a batch release needs (OGC-427). They are read-only queries over
 * the Westgard tables, scoped to one control lot, which the QC module's own
 * DAOs do not offer: those are keyed by analyzer for the dashboards.
 */
public interface BatchWorkplanQcDAO {

    /**
     * Every control lot for a test on an analyzer, in any status, newest first. A
     * null analyzer means the bench lots, the ones with no analyzer.
     */
    List<QCControlLot> getControlLots(String testId, String instrumentId);

    /** The lot's results, most recent run first, in any status. */
    List<QCResult> getRecentResults(String controlLotId, int limit);

    /** The lot's most recent run in one of the given result statuses. */
    QCResult getLatestInStatuses(String controlLotId, List<String> resultStatuses);

    /**
     * Unresolved REJECTION-severity violations raised by any of the lot's runs, in
     * the QC module's own sense ({@code findUnresolved}, the dashboard's Active
     * list): acknowledging a violation is how a laboratory clears it, so an
     * acknowledged one no longer holds a batch.
     */
    List<QCRuleViolation> getOpenRejections(String controlLotId);

    /** Every violation a run raised, keyed by the run's id. */
    Map<String, List<QCRuleViolation>> getViolationsForResults(List<String> resultIds);

    /** Analyzer names by id, read as a projection (see the method body). */
    Map<String, String> getAnalyzerNames(List<String> analyzerIds);
}
