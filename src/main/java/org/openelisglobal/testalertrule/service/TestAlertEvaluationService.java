package org.openelisglobal.testalertrule.service;

import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultvalidation.event.ResultsValidatedEvent;

/**
 * Evaluates a test's alert rules: the in-app alert when a result is entered,
 * email and SMS outside the laboratory only once the result is finalized.
 */
public interface TestAlertEvaluationService {

    void evaluateAndDispatch(Result result, String sysUserId);

    void dispatchFinalized(ResultsValidatedEvent event);
}
