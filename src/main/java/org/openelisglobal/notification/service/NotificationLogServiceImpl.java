package org.openelisglobal.notification.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notification.dao.NotificationLogDAO;
import org.openelisglobal.notification.service.recipient.Recipient;
import org.openelisglobal.notification.service.sender.EmailNotificationSender;
import org.openelisglobal.notification.service.sender.WhatsAppNotificationSender;
import org.openelisglobal.notification.valueholder.EmailNotification;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog;
import org.openelisglobal.notification.valueholder.NotificationLog.OverallStatus;
import org.openelisglobal.notification.valueholder.NotificationLogChannel;
import org.openelisglobal.notification.valueholder.NotificationLogChannel.ChannelStatus;
import org.openelisglobal.notification.valueholder.NotificationPayload;
import org.openelisglobal.notification.valueholder.WhatsAppNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationLogServiceImpl extends AuditableBaseObjectServiceImpl<NotificationLog, Long>
        implements NotificationLogService {

    @Autowired
    private NotificationLogDAO baseDAO;

    @Autowired
    private EmailNotificationSender emailSender;

    @Autowired
    private WhatsAppNotificationSender whatsAppSender;

    public NotificationLogServiceImpl() {
        super(NotificationLog.class);
        // This *is* the audit trail; no need to audit-trail an audit log.
        this.auditTrailLog = false;
    }

    @Override
    protected BaseDAO<NotificationLog, Long> getBaseObjectDAO() {
        return baseDAO;
    }

    /**
     * Persist a {@link NotificationLog} from a fire event. Called by
     * {@code NotificationLogEventListener} (a separate bean) so the call crosses
     * bean boundaries and Spring's {@code @Transactional} advice runs.
     *
     * <p>
     * Uses {@code Propagation.REQUIRES_NEW} because the dispatcher fires inside a
     * {@code TransactionSynchronization.afterCommit} callback. At that point
     * Spring's tx synchronization context is still active
     * ({@code isActualTransactionActive()} returns {@code true}), but the
     * underlying Hibernate session has been committed and closed. With
     * {@code REQUIRED} propagation the listener would join the dead context and
     * Hibernate would fail the persist with "no transaction is in progress".
     * {@code REQUIRES_NEW} suspends the zombie context and opens a fresh tx.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFire(NotificationFiredEvent event) {
        NotificationLog log = buildLogFromEvent(event);
        save(log);
    }

    private NotificationLog buildLogFromEvent(NotificationFiredEvent event) {
        NotificationLog log = new NotificationLog();
        log.setEventCode(event.getEventCode());
        log.setFiredAt(Timestamp.from(Instant.now()));
        log.setTriggeredByUserId(event.getTriggeredByUserId());
        log.setRecipientType(event.getRecipientType());

        Recipient recipient = event.getRecipient();
        if (recipient != null) {
            log.setRecipientDisplayName(recipient.getDisplayName());
            log.setRecipientEmail(recipient.getEmail());
            log.setRecipientPhone(recipient.getPhone());
        }
        log.setReferenceAccession(event.getReferenceAccession());
        log.setReferenceWorkflow(event.getReferenceWorkflow());

        NotificationPayload payload = event.getPayload();
        if (payload != null) {
            log.setRenderedSubject(payload.getSubject());
            log.setRenderedMessage(payload.getMessage());
        }

        Set<NotificationLogChannel> channelRows = new HashSet<>();
        Timestamp now = Timestamp.from(Instant.now());
        for (Map.Entry<NotificationChannel, SendOutcome> entry : event.getOutcomes().entrySet()) {
            SendOutcome outcome = entry.getValue();
            ChannelStatus status = outcome.isSuccess() ? ChannelStatus.SUCCESS : ChannelStatus.FAILED;
            channelRows.add(new NotificationLogChannel(entry.getKey(), status, outcome.errorMessage(),
                    outcome.attempts(), now));
        }
        log.setChannels(channelRows);
        log.setOverallStatus(computeOverallStatus(channelRows));

        log.setSysUserId(event.getTriggeredByUserId() == null ? "1" : event.getTriggeredByUserId());
        return log;
    }

    /**
     * Decision #4 from the plan: SENT iff at least one channel succeeded; FAILED
     * iff every channel failed.
     */
    private OverallStatus computeOverallStatus(Set<NotificationLogChannel> channels) {
        boolean anySuccess = channels.stream().anyMatch(c -> c.getStatus() == ChannelStatus.SUCCESS);
        return anySuccess ? OverallStatus.SENT : OverallStatus.FAILED;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationLog> findPage(Optional<String> eventCode, Optional<String> status, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        return baseDAO.findPage(eventCode, status, safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMatching(Optional<String> eventCode, Optional<String> status) {
        return baseDAO.countMatching(eventCode, status);
    }

    @Override
    @Transactional
    public NotificationLog resend(Long sourceLogId, String sysUserId) {
        NotificationLog source = get(sourceLogId);
        if (source == null) {
            throw new IllegalArgumentException("Source NotificationLog not found: " + sourceLogId);
        }
        Set<NotificationLogChannel> failedChannels = new HashSet<>();
        for (NotificationLogChannel ch : source.getChannels()) {
            if (ch.getStatus() == ChannelStatus.FAILED) {
                failedChannels.add(ch);
            }
        }
        if (failedChannels.isEmpty()) {
            throw new IllegalArgumentException(
                    "Resend rejected: source log " + sourceLogId + " has no FAILED channels to retry");
        }

        // Replay original bytes.
        NotificationPayload payload = NotificationPayload.of(source.getRenderedSubject(), source.getRenderedMessage());

        Set<NotificationLogChannel> outcomes = new HashSet<>();
        Timestamp now = Timestamp.from(Instant.now());
        for (NotificationLogChannel failed : failedChannels) {
            NotificationChannel channel = failed.getChannel();
            ChannelStatus status;
            String errorMessage = null;
            int attempts = 1;
            try {
                switch (channel) {
                case EMAIL: {
                    EmailNotification n = new EmailNotification();
                    n.setRecipientEmailAddress(source.getRecipientEmail());
                    n.setPayload(payload);
                    emailSender.send(n);
                    break;
                }
                case WHATSAPP: {
                    WhatsAppNotification n = new WhatsAppNotification();
                    n.setReceiverPhoneNumber(source.getRecipientPhone());
                    n.setPayload(payload);
                    whatsAppSender.send(n);
                    break;
                }
                }
                status = ChannelStatus.SUCCESS;
            } catch (RuntimeException e) {
                status = ChannelStatus.FAILED;
                errorMessage = e.getMessage();
            }
            outcomes.add(new NotificationLogChannel(channel, status, errorMessage, attempts, now));
        }

        NotificationLog resend = new NotificationLog();
        resend.setEventCode(source.getEventCode());
        resend.setFiredAt(now);
        resend.setTriggeredByUserId(sysUserId);
        resend.setRecipientType(source.getRecipientType());
        resend.setRecipientDisplayName(source.getRecipientDisplayName());
        resend.setRecipientEmail(source.getRecipientEmail());
        resend.setRecipientPhone(source.getRecipientPhone());
        resend.setReferenceAccession(source.getReferenceAccession());
        resend.setReferenceWorkflow(source.getReferenceWorkflow());
        resend.setRenderedSubject(source.getRenderedSubject());
        resend.setRenderedMessage(source.getRenderedMessage());
        resend.setResentFromId(source.getId());
        resend.setChannels(outcomes);
        resend.setOverallStatus(computeOverallStatus(outcomes));
        resend.setSysUserId(sysUserId);

        save(resend);
        return resend;
    }
}
