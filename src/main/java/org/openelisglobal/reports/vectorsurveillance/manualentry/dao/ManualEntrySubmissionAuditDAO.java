package org.openelisglobal.reports.vectorsurveillance.manualentry.dao;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntrySubmissionAudit;

public interface ManualEntrySubmissionAuditDAO extends BaseDAO<ManualEntrySubmissionAudit, Integer> {

    /**
     * Audit rows, newest first, optionally filtered by period. Null bounds mean
     * unfiltered. Re-submissions of a week appear as distinct rows (FR-008).
     */
    List<ManualEntrySubmissionAudit> getByPeriod(LocalDate periodStart, LocalDate periodEnd);
}
