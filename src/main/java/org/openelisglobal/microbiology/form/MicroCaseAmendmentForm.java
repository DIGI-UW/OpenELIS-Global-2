package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public class MicroCaseAmendmentForm {
    public String id;
    public String caseId;
    public Integer sequenceNumber;
    public String status;
    public String reason;
    public Timestamp openedAt;
    public String openedBy;
    public Timestamp closedAt;
    public String closedBy;
    public String closingReason;
}
