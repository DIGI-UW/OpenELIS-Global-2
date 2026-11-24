package org.openelisglobal.resultrun.service;

import java.util.List;
import org.openelisglobal.resultrun.form.ResultRunException;
import org.openelisglobal.resultrun.form.RunOrderMatch;
import org.openelisglobal.resultrun.form.RunViewer;
import org.openelisglobal.resultrun.service.ResultRunRowClassifier.ClassifiedRow;

/**
 * The run's Exceptions block (OGC-1200 FR-E4): the records a run cannot accept
 * until someone resolves them, and their resolutions. Each resolution names the
 * values it is taken on, touches only those, and is recorded on the run with
 * its reason.
 */
public interface RunExceptionService {

    /**
     * The run's unresolved records, the values of one cause together, in review
     * order, each with the latest hold put on it.
     */
    List<ResultRunException> exceptionsOf(Long runId, String analyzerId, List<ClassifiedRow> rows);

    /**
     * Up to ten orders whose lab number starts with {@code query} (three characters
     * at least), saying where {@code testId} (and its part {@code componentId})
     * stands on each.
     */
    List<RunOrderMatch> ordersMatching(String query, String testId, String componentId);

    /**
     * Places values whose lab number named no order, or whose test is not on their
     * order, on the order the technician found. Each decision names the record the
     * page showed ({@code key}); a value now under another record is refused, so a
     * decision never lands on a value someone else has resolved since.
     *
     * @throws IllegalArgumentException when the reason is blank or no order has the
     *                                  lab number
     * @throws IllegalStateException    when a value is gone or no longer such an
     *                                  exception
     */
    void match(Long runId, RunViewer viewer, List<String> rowIds, String key, String accessionNumber, String reason,
            String sysUserId);

    /** Keeps the values blocked with a reason the run shows. */
    void hold(Long runId, RunViewer viewer, List<String> rowIds, String key, String reason, String sysUserId);

    /** Takes the values off the run for good, with the reason. */
    void ignore(Long runId, RunViewer viewer, List<String> rowIds, String key, String reason, String sysUserId);
}
