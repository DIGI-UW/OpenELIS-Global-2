package org.openelisglobal.analyzerqc.service;

import java.util.List;
import org.openelisglobal.analyzerqc.form.QcRunForm;
import org.openelisglobal.analyzerqc.valueholder.AnalyzerQcRun;
import org.openelisglobal.analyzerqc.valueholder.AnalyzerQcStatus;

/**
 * Business logic for Analyzer Manual QC Recording (Issue #3490).
 */
public interface AnalyzerQcService {

    /**
     * Returns the current QC validity status for an analyzer.
     * Implements BR-AQC-001, BR-AQC-002, BR-AQC-004.
     *
     * @param analyzerId the analyzer's String ID
     */
    AnalyzerQcStatus getQcStatus(String analyzerId);

    /**
     * Records a new manual QC run.
     * Implements FR-AQC-012, BR-AQC-006.
     *
     * @param analyzerId    the analyzer's String ID
     * @param form          request body
     * @param currentUserId the authenticated user's sysUserId
     */
    void recordQcRun(String analyzerId, QcRunForm form, String currentUserId);

    /**
     * Returns all QC runs for an analyzer, newest first.
     */
    List<AnalyzerQcRun> getQcHistory(String analyzerId);
}
