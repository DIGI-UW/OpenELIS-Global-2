package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MicroCaseDetailForm {

    public String id;
    public String sampleItemId;
    public String workflowType;
    public String stage;
    public String priority;
    public String cultureMethodId;
    public Timestamp createdAt;
    public String createdBy;
    public Timestamp closedAt;
    public String closedBy;
    public String finalReleaseState;
    public List<MicroCaseActivityForm> activities = new ArrayList<>();
    public List<MicroIsolateForm> isolates = new ArrayList<>();
}
