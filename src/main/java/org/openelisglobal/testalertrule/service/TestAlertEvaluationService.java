package org.openelisglobal.testalertrule.service;

import org.openelisglobal.result.valueholder.Result;

/**
 * OGC-949 / OGC-763 — runtime processor: when a result is validated, evaluate
 * the test's enabled alert rules and dispatch matches to the in-app header
 * notification bell and (via the testNotificationConfig SMS/Email senders) to
 * external recipients.
 */
public interface TestAlertEvaluationService {

    void evaluateAndDispatch(Result result, String sysUserId);
}
