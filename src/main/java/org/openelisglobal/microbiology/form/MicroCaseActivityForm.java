package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public class MicroCaseActivityForm {

    public String id;
    public String caseId;
    public String activityType;
    public Timestamp occurredAt;
    public String performedBy;
    public String note;
    public String structuredData;
}
