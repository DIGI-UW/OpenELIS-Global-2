package org.openelisglobal.notification.service;

import java.util.Map;
import org.openelisglobal.notification.service.recipient.Recipient;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationPayload;
import org.openelisglobal.notification.valueholder.NotificationRecipientType;
import org.springframework.context.ApplicationEvent;

/**
 * Spring ApplicationEvent emitted by {@link NotificationTriggerDispatcher} once
 * per (event, recipient) fire-instance — after all configured channels have
 * been attempted. {@link NotificationLogService} consumes it via
 * {@code @EventListener} to persist a {@code notification_log} row.
 *
 * <p>
 * Decoupling rationale (per Senior-Dev review): the dispatcher's job is to
 * send. Persisting the audit trail is a separate concern; a failure in the log
 * write must not block dispatch. The dispatcher wraps the publish in try/catch
 * so even an unhandled listener exception is contained.
 */
public class NotificationFiredEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final String eventCode;
    private final NotificationRecipientType recipientType;
    private final Recipient recipient;
    private final NotificationPayload payload;
    private final Map<NotificationChannel, SendOutcome> outcomes;
    private final String triggeredByUserId;
    private final String referenceAccession;
    private final String referenceWorkflow;

    public NotificationFiredEvent(Object source, String eventCode, NotificationRecipientType recipientType,
            Recipient recipient, NotificationPayload payload, Map<NotificationChannel, SendOutcome> outcomes,
            String triggeredByUserId, String referenceAccession, String referenceWorkflow) {
        super(source);
        this.eventCode = eventCode;
        this.recipientType = recipientType;
        this.recipient = recipient;
        this.payload = payload;
        this.outcomes = outcomes;
        this.triggeredByUserId = triggeredByUserId;
        this.referenceAccession = referenceAccession;
        this.referenceWorkflow = referenceWorkflow;
    }

    public String getEventCode() {
        return eventCode;
    }

    public NotificationRecipientType getRecipientType() {
        return recipientType;
    }

    public Recipient getRecipient() {
        return recipient;
    }

    public NotificationPayload getPayload() {
        return payload;
    }

    public Map<NotificationChannel, SendOutcome> getOutcomes() {
        return outcomes;
    }

    public String getTriggeredByUserId() {
        return triggeredByUserId;
    }

    public String getReferenceAccession() {
        return referenceAccession;
    }

    public String getReferenceWorkflow() {
        return referenceWorkflow;
    }
}
