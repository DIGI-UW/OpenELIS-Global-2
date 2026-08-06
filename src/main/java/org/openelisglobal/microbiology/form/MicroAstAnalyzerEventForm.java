package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public class MicroAstAnalyzerEventForm {

    public String id;
    public String externalEventId;
    public String eventType;
    public String analyzerId;
    public String sourceId;
    public String targetReference;
    public String status;
    public String failureReason;
    public Timestamp receivedAt;
    public Timestamp processedAt;
    public String reconciliationUrl;
}
