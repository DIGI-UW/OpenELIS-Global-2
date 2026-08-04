package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public class MicroIdentificationEventForm {
    public String id;
    public String isolateId;
    public String amendmentId;
    public String eventType;
    public String previousOrganismId;
    public String previousOrganismText;
    public String previousSignificance;
    public String previousIdentificationStatus;
    public String newOrganismId;
    public String newOrganismText;
    public String newSignificance;
    public String newIdentificationStatus;
    public String reason;
    public Timestamp changedAt;
    public String changedBy;
}
