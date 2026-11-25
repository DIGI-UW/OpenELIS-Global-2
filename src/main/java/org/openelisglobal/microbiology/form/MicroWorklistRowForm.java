package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public class MicroWorklistRowForm {

    public String rowId;
    public String grain;
    public String caseId;
    public String sampleItemId;
    public String accessionNumber;
    public String patientDisplay;
    public String specimenDisplay;
    public Timestamp collectionDate;
    public String specimenTypeId;
    public String patientOrigin;
    public String stage;
    public String priority;
    public String dueAction;
    public String urgency;
    public boolean needsAstReview;
    public boolean hasOpenCriticalCommunication;
    public String isolateId;
    public String isolateLabel;
    public String organismDisplay;
    public String organismId;
    public String isolateSignificance;
    public String astRunId;
    public String panelId;
    public String panelName;
    public String astStatus;
    public Timestamp astStartedAt;
    public boolean analyzerResultsAvailable;
    public String analyzerExpertFlags;
    public Timestamp createdAt;
    public Timestamp lastActivityAt;
    public String lastActivityBy;
}
