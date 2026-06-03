package org.openelisglobal.notification.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.notification.service.recipient.NotificationRecipientResolver;
import org.openelisglobal.notification.service.recipient.Recipient;
import org.openelisglobal.notification.service.sender.EmailNotificationSender;
import org.openelisglobal.notification.service.sender.WhatsAppNotificationSender;
import org.openelisglobal.notification.valueholder.EmailNotification;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationPayload;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate;
import org.openelisglobal.notification.valueholder.NotificationRecipientType;
import org.openelisglobal.notification.valueholder.NotificationTriggerConfig;
import org.openelisglobal.notification.valueholder.WhatsAppNotification;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Off-thread entry point for configurable notification triggers. Looks up the
 * {@link NotificationTriggerConfig} for an event code, then dispatches the
 * cross-product of configured channels × recipients through the matching
 * sender. Failures are retried up to {@value #MAX_ATTEMPTS} times with a
 * {@value #RETRY_DELAY_MS}ms delay before being logged.
 *
 * <p>
 * After all configured channels are attempted for a recipient, a
 * {@link NotificationFiredEvent} is published (synchronously, wrapped in
 * try/catch). {@code NotificationLogService} listens and persists a {@code
 * notification_log} row. The log write is decoupled — a failure there cannot
 * break dispatch.
 */
@Component
public class NotificationTriggerDispatcher {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000L;

    @Autowired
    private NotificationTriggerConfigService configService;

    @Autowired
    private NotificationRecipientResolver recipientResolver;

    @Autowired
    private ExternalConnectionService externalConnectionService;

    @Autowired
    private EmailNotificationSender emailSender;

    @Autowired
    private WhatsAppNotificationSender whatsAppSender;

    @Autowired
    private SampleWorkflowResolver sampleWorkflowResolver;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

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

        // Channel-level active check is a pure function of the channel — compute once
        // and reuse across recipients (per Java review).
        Set<NotificationChannel> activeChannels = new LinkedHashSet<>();
        for (NotificationChannel channel : config.getChannels()) {
            if (externalConnectionService.isActive(channel.getConnectionType())) {
                activeChannels.add(channel);
            } else {
                LogEvent.logInfo(this.getClass().getSimpleName(), "fire",
                        "channel " + channel + " is not provisioned (external_connection " + channel.getConnectionType()
                                + " missing or inactive); skipping channel for event " + eventCode);
            }
        }
        if (activeChannels.isEmpty()) {
            return;
        }

        // recipient → channel loop: groups outcomes per recipient so the
        // NotificationFiredEvent represents one (event, recipient, fire-instance).
        for (NotificationRecipientType recipientType : config.getRecipientTypes()) {
            Recipient recipient = recipientResolver.resolve(recipientType, context);
            if (recipient == null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "fire",
                        "recipient " + recipientType + " could not be resolved for event " + eventCode + "; skipping");
                continue;
            }

            NotificationPayload payload = renderPayload(config.getPayloadTemplate(), context);
            Map<NotificationChannel, SendOutcome> outcomes = new LinkedHashMap<>();
            for (NotificationChannel channel : activeChannels) {
                if (!recipient.canReceive(channel)) {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "fire", "recipient " + recipientType + " has no "
                            + channel + " contact info for event " + eventCode + "; skipping");
                    continue;
                }
                LogEvent.logInfo(this.getClass().getSimpleName(), "fire",
                        "dispatching event " + eventCode + " on channel " + channel + " to recipient " + recipientType
                                + " (" + recipient.getDisplayName() + ")");
                SendOutcome outcome = sendWithRetry(channel, recipient, payload, config.getPayloadTemplate(), context);
                outcomes.put(channel, outcome);
            }

            if (outcomes.isEmpty()) {
                continue;
            }
            publishFireEvent(eventCode, recipientType, recipient, payload, outcomes, context);
        }
    }

    private void publishFireEvent(String eventCode, NotificationRecipientType recipientType, Recipient recipient,
            NotificationPayload payload, Map<NotificationChannel, SendOutcome> outcomes, NotificationContext context) {
        try {
            Sample sample = context.getSample();
            String accession = sample == null ? null : sample.getAccessionNumber();
            String workflow = sampleWorkflowResolver.resolveWorkflow(sample);
            NotificationFiredEvent event = new NotificationFiredEvent(this, eventCode, recipientType, recipient,
                    payload, outcomes, context.getSubmittingUserId(), accession, workflow);
            applicationEventPublisher.publishEvent(event);
        } catch (RuntimeException e) {
            // Never block dispatch on a log-write failure. Log + swallow.
            LogEvent.logError(this.getClass().getSimpleName(), "publishFireEvent",
                    "failed to publish NotificationFiredEvent for " + eventCode);
            LogEvent.logError(e);
        }
    }

    private SendOutcome sendWithRetry(NotificationChannel channel, Recipient recipient, NotificationPayload payload,
            NotificationPayloadTemplate template, NotificationContext context) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                dispatchOnce(channel, recipient, payload, template, context);
                if (attempt > 1) {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "sendWithRetry",
                            "succeeded on attempt " + attempt + " for event " + context.getEventCode());
                }
                return SendOutcome.success(attempt);
            } catch (RuntimeException e) {
                lastError = e;
                LogEvent.logWarn(this.getClass().getSimpleName(), "sendWithRetry", "attempt " + attempt + " of "
                        + MAX_ATTEMPTS + " failed for event " + context.getEventCode() + ": " + e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return SendOutcome.failed(attempt, "interrupted");
                    }
                }
            }
        }
        LogEvent.logError(this.getClass().getSimpleName(), "sendWithRetry",
                "exhausted " + MAX_ATTEMPTS + " attempts for event " + context.getEventCode());
        if (lastError != null) {
            LogEvent.logError(lastError);
            return SendOutcome.failed(MAX_ATTEMPTS, lastError.getMessage());
        }
        return SendOutcome.failed(MAX_ATTEMPTS, "unknown failure");
    }

    private void dispatchOnce(NotificationChannel channel, Recipient recipient, NotificationPayload payload,
            NotificationPayloadTemplate template, NotificationContext context) {
        // Audit the rendered subject + body before handing off to the sender. Senders
        // can bail (missing creds, disabled, network error) and lose the rendered text
        // forever; this line gives ops + integration tests a way to verify the
        // template editor's saved string is what actually went out.
        LogEvent.logInfo(this.getClass().getSimpleName(), "dispatchOnce", "rendered " + context.getEventCode() + " "
                + channel + " subject=" + payload.getSubject() + " body=" + payload.getMessage());
        switch (channel) {
        case EMAIL -> emailSender.send(buildEmail(recipient, payload));
        case WHATSAPP -> whatsAppSender.send(buildWhatsApp(recipient, payload, template, context));
        }
    }

    private EmailNotification buildEmail(Recipient recipient, NotificationPayload payload) {
        EmailNotification notification = new EmailNotification();
        notification.setRecipientEmailAddress(recipient.getEmail());
        notification.setPayload(payload);
        return notification;
    }

    private WhatsAppNotification buildWhatsApp(Recipient recipient, NotificationPayload payload,
            NotificationPayloadTemplate template, NotificationContext context) {
        WhatsAppNotification notification = new WhatsAppNotification();
        notification.setReceiverPhoneNumber(recipient.getPhone());
        notification.setPayload(payload);
        if (template != null && !GenericValidator.isBlankOrNull(template.getTemplateExternalId())) {
            notification.setTemplateContentSid(template.getTemplateExternalId());
            notification.setTemplateVariables(context.getVariables());
        }
        return notification;
    }

    private NotificationPayload renderPayload(NotificationPayloadTemplate template, NotificationContext context) {
        String subject = template == null ? "" : substitute(template.getSubjectTemplate(), context.getVariables());
        String message = template == null ? "" : substitute(template.getMessageTemplate(), context.getVariables());
        return NotificationPayload.of(subject, message);
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
