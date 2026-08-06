package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MicroWorklistRowForm {

    public String rowId;
    public String grain;
    public String caseId;
    public String sampleItemId;
    public String accessionNumber;
    public String patientDisplay;
    public String specimenDisplay;
    public String workflowType;
    public String stage;
    public String priority;
    public String dueAction;
    public String urgency;
    public boolean needsAstReview;
    public boolean hasOpenCriticalCommunication;
    public String isolateId;
    public String isolateLabel;
    public String organismDisplay;
    public String astRunId;
    public String panelId;
    public String panelName;
    public String astStatus;
    public Timestamp astStartedAt;
    public boolean analyzerResultsAvailable;
    public List<String> siblingWorkflows = new ArrayList<>();
    public Timestamp createdAt;
    public Timestamp lastActivityAt;
    public String lastActivityBy;
}
