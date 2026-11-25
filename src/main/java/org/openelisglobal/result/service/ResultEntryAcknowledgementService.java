package org.openelisglobal.result.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.result.action.util.ResultEntryAlert;
import org.openelisglobal.result.valueholder.ResultEntryAcknowledgement;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.test.beanItems.TestResultItem;

/**
 * OGC-1417: the acknowledgement owed before a result is saved. A critical value
 * is always acknowledged by the person entering it; a value outside the valid
 * range is confirmed when the Result Configuration setting
 * {@code alertWhenInvalidResult} is on. Every screen that writes a result value
 * asks this service what is owed, refuses the save while anything is
 * unacknowledged, and records what was acknowledged with the save.
 */
public interface ResultEntryAcknowledgementService extends BaseObjectService<ResultEntryAcknowledgement, String> {

    /** Every acknowledgement recorded against the analysis, oldest first. */
    List<ResultEntryAcknowledgement> getByAnalysisId(String analysisId);

    /**
     * What the value owes, judged by the same flag Results Entry and Validation
     * show: nothing when it is the value already stored ({@code previousValue}),
     * which was acknowledged when it was saved. A value beyond a critical bound
     * owes its acknowledgement even when it also lies outside the valid range; a
     * comparator ({@code <5}) is read past, as the screens read it.
     */
    List<ResultEntryAlert> alertsFor(ResultLimit limit, String resultType, String value, String previousValue);

    /**
     * Whether the admin has asked for values outside the valid range to be
     * confirmed.
     */
    boolean isInvalidAlertEnabled();

    /**
     * The custom critical message from Result Configuration, or null when it is
     * blank or still the shipped placeholder, so the screen shows its own
     * translated default.
     */
    String getCustomCriticalMessage();

    /** The body of the refusal for a save that still owes acknowledgements. */
    Map<String, Object> refusalBody(List<ResultEntryAlert> unacknowledged);

    /**
     * Records every acknowledged alert, naming the result each one was saved as.
     */
    void recordAcknowledgements(List<ResultEntryAlert> alerts, String source, String sysUserId);

    /**
     * What the Results Entry rows being saved owe, each judged against the limit
     * the row was loaded with (patient, specimen and component) and against the
     * value already stored for it, and marked acknowledged when the row says so.
     */
    List<ResultEntryAlert> alertsForItems(List<TestResultItem> items);

    /** The same, for a value a validator corrects on the Validation page. */
    List<ResultEntryAlert> alertsForValidationItems(List<AnalysisItem> items);

    /**
     * The same, for analyzer review: only a value the reviewer retyped is owed,
     * judged against the value the instrument sent. The staged row, not the
     * request, names the order, test and component.
     */
    List<ResultEntryAlert> alertsForAnalyzerItems(List<AnalyzerResultItem> items);

    /**
     * Records the acknowledged alerts inside the save's own transaction, so a value
     * is never stored without the acknowledgement it was saved with.
     */
    IResultUpdate acknowledgementRecorder(List<ResultEntryAlert> alerts, String source, String sysUserId);
}
