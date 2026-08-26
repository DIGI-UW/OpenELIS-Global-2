package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MicroWorklistRowForm {

    public String caseId;
    public String sampleItemId;
    public String workflowType;
    public String stage;
    public String priority;
    public String dueAction;
    public String urgency;
    public boolean needsAstReview;
    public boolean hasOpenCriticalCommunication;
    public List<String> siblingWorkflows = new ArrayList<>();
    public Timestamp createdAt;
}
