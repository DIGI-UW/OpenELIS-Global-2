package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public class MicroIncomingForm {
    public String id, caseId, source, testId, testName, sampleItemId, state, runSourceKey, rawResultValue;
    public String placedBy, placementTarget, failureReason;
    public Timestamp receivedAt, placedAt;
    public MicroCaseResultRequestForm result;
}
