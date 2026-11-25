package org.openelisglobal.notification.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.delivery.service.DeliveryOutboxService;
import org.openelisglobal.delivery.service.DeliveryRequest;
import org.openelisglobal.notification.service.recipient.NotificationRecipientResolver;
import org.openelisglobal.notification.service.recipient.Recipient;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog.Product;
import org.openelisglobal.notification.valueholder.NotificationPayload;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate;
import org.openelisglobal.notification.valueholder.NotificationRecipientType;
import org.openelisglobal.notification.valueholder.NotificationTriggerConfig;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Off-thread entry point for configurable notification triggers. Looks up the
 * {@link NotificationTriggerConfig} for an event code, renders the template for
 * each configured recipient and queues one message per recipient in the
 * delivery outbox: the Sent Messages row exists before anything is sent, and
 * the delivery worker sends and retries from it.
 */
@Component
public class NotificationTriggerDispatcher {

    @Autowired
    private NotificationTriggerConfigService configService;

    @Autowired
    private NotificationRecipientResolver recipientResolver;

    @Autowired
    private SampleWorkflowResolver sampleWorkflowResolver;

    @Autowired
    private DeliveryOutboxService outbox;

    @Async
    public void fire(NotificationContext context) {
        if (context == null || GenericValidator.isBlankOrNull(context.getEventCode())) {
            return;
        }
        String eventCode = context.getEventCode();

        Optional<NotificationTriggerConfig> configOpt = configService.getByEventCode(eventCode);
        if (configOpt.isEmpty()) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "fire",
                    "no NotificationTriggerConfig row for event " + eventCode + "; skipping");
            return;
        }
        NotificationTriggerConfig config = configOpt.get();
        if (!config.isEnabled()) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "fire",
                    "trigger " + eventCode + " is disabled; skipping");
            return;
        }
        if (config.getChannels().isEmpty() || config.getRecipientTypes().isEmpty()) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "fire",
                    "trigger " + eventCode + " has no channels or no recipients configured; skipping");
            return;
        }

        Sample sample = context.getSample();
        String accession = sample == null ? null : sample.getAccessionNumber();
        String workflow = sampleWorkflowResolver.resolveWorkflow(sample);
        NotificationPayload payload = renderPayload(config.getPayloadTemplate(), context);
        NotificationPayloadTemplate template = config.getPayloadTemplate();

        for (NotificationRecipientType recipientType : config.getRecipientTypes()) {
            Recipient recipient = recipientResolver.resolve(recipientType, context);
            if (recipient == null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "fire",
                        "recipient " + recipientType + " could not be resolved for event " + eventCode + "; skipping");
                continue;
            }
            DeliveryRequest request = new DeliveryRequest().setProduct(Product.TRIGGER).setEventCode(eventCode)
                    .setRecipientType(recipientType).setRecipientDisplayName(recipient.getDisplayName())
                    .setRecipientEmail(recipient.getEmail()).setRecipientPhone(recipient.getPhone())
                    .setSubject(payload.getSubject()).setMessage(payload.getMessage()).setReferenceAccession(accession)
                    .setReferenceWorkflow(workflow).setSentByUserId(context.getSubmittingUserId());
            for (NotificationChannel channel : config.getChannels()) {
                request.addChannel(channel);
            }
            if (template != null && !GenericValidator.isBlankOrNull(template.getTemplateExternalId())) {
                request.setTemplateExternalId(template.getTemplateExternalId());
                request.setTemplateVariables(withoutIdentifyingTokens(context.getVariables()));
            }
            try {
                outbox.enqueue(request);
            } catch (RuntimeException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "fire",
                        "could not queue event " + eventCode + " for " + recipientType);
                LogEvent.logError(e);
            }
        }
    }

    private NotificationPayload renderPayload(NotificationPayloadTemplate template, NotificationContext context) {
        String subject = template == null ? "" : substitute(template.getSubjectTemplate(), context.getVariables());
        String message = template == null ? "" : substitute(template.getMessageTemplate(), context.getVariables());
        return NotificationPayload.of(subject, message);
    }

    /**
     * The gateway template gets every event variable except the ones that name the
     * patient (FR-D1): those never leave the laboratory, not even as template
     * parameters, and are not stored with the message either.
     */
    static Map<String, String> withoutIdentifyingTokens(Map<String, String> variables) {
        Map<String, String> safe = new LinkedHashMap<>();
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                if (!EventVariableRegistry.IDENTIFYING_TOKENS.contains(entry.getKey())) {
                    safe.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return safe;
    }

    private String substitute(String raw, Map<String, String> variables) {
        if (raw == null) {
            return null;
        }
        String result = raw;
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String value = entry.getValue() == null ? "" : entry.getValue();
                result = result.replace("[" + entry.getKey() + "]", value);
            }
        }
        return result;
    }
}
