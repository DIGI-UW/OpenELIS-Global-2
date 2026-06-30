package org.openelisglobal.analyzerqc.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.analyzer.valueholder.QcStatus;

/**
 * Response payload for GET /rest/analyzers/{id}/qc-status.
 * Also returned by POST after a new QC run is recorded.
 *
 * QcStatus enum is already defined in:
 *   org.openelisglobal.analyzer.valueholder.QcStatus
 *   Values: PASS, OVERDUE, FAILED, NOT_RUN
 */
public class AnalyzerQcStatus {

    /** Which analyzer this status belongs to. */
    private String analyzerId;

    /**
     * Computed QC validity: PASS, OVERDUE, FAILED, or NOT_RUN.
     * Derived from the last PASS run date and the analyzer's frequency rule.
     */
    private QcStatus status;

    /** Whether QC is mandatory (true) or informational only (false). */
    private boolean qcRequired;

    /** Timestamp of the most recent QC run (any result). */
    private Timestamp lastRunDate;

    /** Result string of the most recent run: "PASS" or "FAIL". */
    private String lastRunResult;

    /** Optional measurement value from the most recent run. */
    private String lastRunValue;

    /** Entry point of the most recent run: ANALYZER_IMPORT, etc. */
    private String lastRunSource;

    /** When the next QC run is due based on the frequency rule. */
    private Timestamp nextQcDue;

    // ── Getters & setters ────────────────────────────────────────────────────

    public String getAnalyzerId() { return analyzerId; }
    public void setAnalyzerId(String analyzerId) { this.analyzerId = analyzerId; }

    public QcStatus getStatus() { return status; }
    public void setStatus(QcStatus status) { this.status = status; }

    public boolean isQcRequired() { return qcRequired; }
    public void setQcRequired(boolean qcRequired) { this.qcRequired = qcRequired; }

    public Timestamp getLastRunDate() { return lastRunDate; }
    public void setLastRunDate(Timestamp lastRunDate) { this.lastRunDate = lastRunDate; }

    public String getLastRunResult() { return lastRunResult; }
    public void setLastRunResult(String lastRunResult) { this.lastRunResult = lastRunResult; }

    public String getLastRunValue() { return lastRunValue; }
    public void setLastRunValue(String lastRunValue) { this.lastRunValue = lastRunValue; }

    public String getLastRunSource() { return lastRunSource; }
    public void setLastRunSource(String lastRunSource) { this.lastRunSource = lastRunSource; }

    public Timestamp getNextQcDue() { return nextQcDue; }
    public void setNextQcDue(Timestamp nextQcDue) { this.nextQcDue = nextQcDue; }
}
