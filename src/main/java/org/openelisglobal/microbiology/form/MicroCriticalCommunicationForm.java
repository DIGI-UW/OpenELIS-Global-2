package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public class MicroCriticalCommunicationForm {

    public String id;
    public String caseId;
    public String recipient;
    public String message;
    public Timestamp communicatedAt;
    public String communicatedBy;
    public String acknowledgementStatus;
    public Timestamp acknowledgedAt;
    public String acknowledgedBy;
    public boolean followUpNeeded;
    public String correctionOfId;
}
