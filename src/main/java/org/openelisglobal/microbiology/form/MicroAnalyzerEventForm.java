package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;

public class MicroAnalyzerEventForm {

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

    public static MicroAnalyzerEventForm from(AnalyzerEvent event) {
        MicroAnalyzerEventForm form = new MicroAnalyzerEventForm();
        form.id = event.getId();
        form.externalEventId = event.getExternalEventId();
        form.eventType = event.getEventType();
        form.analyzerId = event.getAnalyzerId();
        form.sourceId = event.getSourceId();
        form.targetReference = event.getTargetReference();
        form.status = event.getStatus();
        form.failureReason = event.getFailureReason();
        form.receivedAt = event.getReceivedAt();
        form.processedAt = event.getProcessedAt();
        form.reconciliationUrl = "/AnalyzerResults?view=import-issues";
        return form;
    }
}
