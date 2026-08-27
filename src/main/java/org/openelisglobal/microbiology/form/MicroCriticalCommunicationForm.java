package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public class MicroCriticalCommunicationForm {

    public String id;
    public String caseId;
    public String targetType;
    public String targetId;
    public String recipient;
    public String recipientContact;
    public String communicationMethod;
    public String message;
    public Timestamp communicatedAt;
    public String communicatedBy;
    public String acknowledgementStatus;
    public Timestamp acknowledgedAt;
    public String acknowledgedBy;
    public Timestamp closedAt;
    public String closedBy;
    public String resolutionNote;
    public Long alertId;
    public boolean followUpNeeded;
    public String correctionOfId;
}
