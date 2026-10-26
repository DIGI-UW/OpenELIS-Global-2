package org.openelisglobal.delivery.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.delivery.dao.DeliveryChannelDAO;
import org.openelisglobal.delivery.dao.DeliveryQueueDAO;
import org.openelisglobal.delivery.dao.DeliveryQueueDAO.Claim;
import org.openelisglobal.delivery.dao.DeliveryQueueSearch;
import org.openelisglobal.delivery.dto.SentMessageDTO;
import org.openelisglobal.delivery.dto.SentMessagesPageDTO;
import org.openelisglobal.delivery.service.sender.DeliveryException;
import org.openelisglobal.delivery.service.sender.DeliveryMessage;
import org.openelisglobal.delivery.service.sender.DeliverySender;
import org.openelisglobal.delivery.valueholder.DeliveryChannel;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notification.dao.NotificationLogAttemptDAO;
import org.openelisglobal.notification.dao.NotificationLogDAO;
import org.openelisglobal.notification.service.NotificationLogService;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog;
import org.openelisglobal.notification.valueholder.NotificationLog.OverallStatus;
import org.openelisglobal.notification.valueholder.NotificationLog.Product;
import org.openelisglobal.notification.valueholder.NotificationLogAttempt;
import org.openelisglobal.notification.valueholder.NotificationLogChannel;
import org.openelisglobal.notification.valueholder.NotificationLogChannel.ChannelStatus;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryOutboxServiceImpl implements DeliveryOutboxService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_PAGE_SIZE = 200;

    @Autowired
    private DeliveryQueueDAO queueDAO;
    @Autowired
    private NotificationLogDAO notificationLogDAO;
    @Autowired
    private NotificationLogService notificationLogService;
    @Autowired
    private NotificationLogAttemptDAO attemptDAO;
    @Autowired
    private DeliveryChannelDAO channelDAO;
    @Autowired
    private DeliverySettingsService settingsService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private List<DeliverySender> senderBeans;

    private Map<NotificationChannel, DeliverySender> senders() {
        Map<NotificationChannel, DeliverySender> byChannel = new EnumMap<>(NotificationChannel.class);
        for (DeliverySender sender : senderBeans) {
            byChannel.put(sender.channel(), sender);
        }
        return byChannel;
    }

    // ----- writing rows

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationLog enqueue(DeliveryRequest request) {
        NotificationLog log = rowFor(request);
        Timestamp now = Timestamp.from(Instant.now());
        Set<NotificationLogChannel> rows = new HashSet<>();
        Map<NotificationChannel, DeliverySender> senders = senders();
        for (NotificationChannel channel : request.getChannels()) {
            NotificationLogChannel row = new NotificationLogChannel(channel, ChannelStatus.QUEUED, null, 0, now);
            String address = addressFor(request.getRecipientEmail(), request.getRecipientPhone(), channel);
            if (GenericValidator.isBlankOrNull(address)) {
                row.setStatus(ChannelStatus.SKIPPED);
                row.setErrorMessage(MessageUtil.getMessage("delivery.skip.noAddress", channelName(channel)));
            } else if (!channelReady(senders, channel)) {
                row.setStatus(ChannelStatus.FAILED);
                row.setErrorMessage(MessageUtil.getMessage("delivery.error.notSetUp", channelName(channel)));
            } else {
                row.setNextAttemptAt(now);
            }
            rows.add(row);
        }
        log.setChannels(rows);
        log.setOverallStatus(overall(rows));
        notificationLogService.save(log);
        return log;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationLog recordImmediate(DeliveryRequest request, NotificationChannel channel, boolean success,
            String error, String gatewayMessageId) {
        NotificationLog log = rowFor(request);
        Timestamp now = Timestamp.from(Instant.now());
        NotificationLogChannel row = new NotificationLogChannel(channel,
                success ? ChannelStatus.SUCCESS : ChannelStatus.FAILED, error, 1, now);
        row.setGatewayMessageId(gatewayMessageId);
        Set<NotificationLogChannel> rows = new HashSet<>();
        rows.add(row);
        log.setChannels(rows);
        log.setOverallStatus(overall(rows));
        notificationLogService.save(log);
        recordAttemptRow(log.getId(), channel, success, error);
        return log;
    }

    @Override
    public NotificationLog resend(Long sourceLogId, String sysUserId) {
        NotificationLog source = notificationLogDAO.get(sourceLogId)
                .orElseThrow(() -> new IllegalArgumentException("Source NotificationLog not found: " + sourceLogId));
        if (source.getOverallStatus() == OverallStatus.QUEUED) {
            throw new IllegalArgumentException("Resend rejected: message " + sourceLogId + " is still queued");
        }
        DeliveryRequest request = new DeliveryRequest().setProduct(source.getProduct())
                .setEventCode(source.getEventCode()).setRecipientType(source.getRecipientType())
                .setRecipientDisplayName(source.getRecipientDisplayName()).setRecipientEmail(source.getRecipientEmail())
                .setRecipientPhone(source.getRecipientPhone()).setSubject(source.getRenderedSubject())
                .setMessage(source.getRenderedMessage()).setReferenceAccession(source.getReferenceAccession())
                .setReferenceWorkflow(source.getReferenceWorkflow()).setRuleName(source.getRuleName())
                .setReportVersionId(source.getReportVersionId()).setReportFormat(source.getReportFormat())
                .setTemplateExternalId(source.getTemplateExternalId())
                .setTemplateVariables(parseVariables(source.getTemplateVariables())).setSentByUserId(sysUserId)
                .setResentFromId(source.getId());
        boolean anyFailed = source.getChannels().stream()
                .anyMatch(channel -> channel.getStatus() != ChannelStatus.SUCCESS);
        for (NotificationLogChannel channel : source.getChannels()) {
            if (!anyFailed || channel.getStatus() != ChannelStatus.SUCCESS) {
                request.addChannel(channel.getChannel());
            }
        }
        return enqueue(request);
    }

    private NotificationLog rowFor(DeliveryRequest request) {
        NotificationLog log = new NotificationLog();
        log.setProduct(request.getProduct());
        log.setEventCode(GenericValidator.isBlankOrNull(request.getEventCode())
                ? request.getProduct() == null ? "TRIGGER" : request.getProduct().name()
                : request.getEventCode());
        log.setFiredAt(Timestamp.from(Instant.now()));
        log.setTriggeredByUserId(request.getSentByUserId());
        log.setRecipientType(request.getRecipientType() == null
                ? org.openelisglobal.notification.valueholder.NotificationRecipientType.TYPED_ADDRESS
                : request.getRecipientType());
        log.setRecipientDisplayName(request.getRecipientDisplayName());
        log.setRecipientEmail(request.getRecipientEmail());
        log.setRecipientPhone(request.getRecipientPhone());
        log.setReferenceAccession(request.getReferenceAccession());
        log.setReferenceWorkflow(request.getReferenceWorkflow());
        log.setRenderedSubject(request.getSubject());
        log.setRenderedMessage(request.getMessage());
        log.setRuleName(request.getRuleName());
        log.setReportVersionId(request.getReportVersionId());
        log.setReportFormat(request.getReportFormat());
        log.setTemplateExternalId(request.getTemplateExternalId());
        log.setTemplateVariables(serializeVariables(request.getTemplateVariables()));
        log.setResentFromId(request.getResentFromId());
        log.setSysUserId(GenericValidator.isBlankOrNull(request.getSentByUserId()) ? "1" : request.getSentByUserId());
        return log;
    }

    // ----- the worker side

    /** A claim covers the batch; each send renews its own row for this long. */
    static final int CLAIM_LEASE_SECONDS = 120;
    static final int ATTEMPT_LEASE_SECONDS = 180;

    @Override
    public List<Claim> claim(String worker, int limit) {
        return queueDAO.claimDue(worker, limit, CLAIM_LEASE_SECONDS);
    }

    /**
     * The lease is renewed for this row alone before the send, so a row whose batch
     * lease lapsed while earlier sends ran slowly is never sent twice: the worker
     * that lost it steps back and the one that claimed it again sends.
     */
    @Override
    public void attempt(Claim claim, String worker) {
        Optional<NotificationLog> found = notificationLogDAO.get(claim.logId());
        if (found.isEmpty()) {
            queueDAO.release(claim.logId(), claim.channel(), worker);
            return;
        }
        NotificationLog log = found.get();
        NotificationChannel channel = claim.channel();
        if (!queueDAO.extendLease(log.getId(), channel, worker, ATTEMPT_LEASE_SECONDS)) {
            return;
        }
        int attempts = claim.attempts() + 1;
        DeliverySender sender = senders().get(channel);
        DeliveryChannel config = channelDAO.findByChannel(DeliveryChannel.Channel.valueOf(channel.name())).orElse(null);
        String address = addressFor(log.getRecipientEmail(), log.getRecipientPhone(), channel);
        DeliveryMessage message = new DeliveryMessage(channel, address, log.getRenderedSubject(),
                log.getRenderedMessage(), log.getTemplateExternalId(), parseVariables(log.getTemplateVariables()));
        try {
            if (sender == null) {
                throw new DeliveryException(DeliveryException.Code.NOT_SET_UP,
                        MessageUtil.getMessage("delivery.error.notSetUp", channelName(channel)));
            }
            String gatewayId = sender.send(config, message);
            queueDAO.recordAttempt(log.getId(), channel, ChannelStatus.SUCCESS, attempts, null, null, gatewayId);
            recordAttemptRow(log.getId(), channel, true, null);
            channelDAO.recordOutcome(DeliveryChannel.Channel.valueOf(channel.name()), true, null);
        } catch (DeliveryException e) {
            fail(log.getId(), channel, attempts, e.getMessage(), e.isRetryable());
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            fail(log.getId(), channel, attempts, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(),
                    true);
        } finally {
            queueDAO.refreshOverallStatus(log.getId());
        }
    }

    private void fail(Long logId, NotificationChannel channel, int attempts, String error, boolean retryable) {
        List<Integer> delays = settingsService.retryDelays();
        Timestamp next = null;
        ChannelStatus status = ChannelStatus.FAILED;
        if (retryable && attempts <= delays.size()) {
            next = Timestamp.from(Instant.now().plusSeconds(60L * delays.get(attempts - 1)));
            status = ChannelStatus.QUEUED;
        }
        queueDAO.recordAttempt(logId, channel, status, attempts, error, next, null);
        recordAttemptRow(logId, channel, false, error);
        channelDAO.recordOutcome(DeliveryChannel.Channel.valueOf(channel.name()), false, error);
    }

    private void recordAttemptRow(Long logId, NotificationChannel channel, boolean success, String error) {
        NotificationLogAttempt attempt = new NotificationLogAttempt();
        attempt.setNotificationLogId(logId);
        attempt.setChannel(channel);
        attempt.setAttemptedAt(OffsetDateTime.now());
        attempt.setSucceeded(success);
        attempt.setErrorMessage(error == null ? null : error.substring(0, Math.min(error.length(), 2000)));
        attempt.setSysUserId("1");
        attemptDAO.save(attempt);
    }

    // ----- reading

    @Override
    @Transactional(readOnly = true)
    public SentMessagesPageDTO list(DeliveryQueueSearch search, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
        SentMessagesPageDTO result = new SentMessagesPageDTO();
        result.setPage(safePage);
        result.setPageSize(safeSize);
        result.setTotal(queueDAO.count(search));
        List<SentMessageDTO> rows = new ArrayList<>();
        for (NotificationLog log : queueDAO.search(search, safePage, safeSize)) {
            rows.add(toDTO(log, false));
        }
        result.setRows(rows);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SentMessageDTO> detail(Long id) {
        return notificationLogDAO.get(id).map(log -> toDTO(log, true));
    }

    private SentMessageDTO toDTO(NotificationLog log, boolean withContent) {
        SentMessageDTO dto = new SentMessageDTO();
        dto.setId(log.getId());
        dto.setProduct(log.getProduct() == null ? Product.TRIGGER.name() : log.getProduct().name());
        dto.setEventCode(log.getEventCode());
        dto.setRuleName(log.getRuleName());
        dto.setFiredAt(iso(log.getFiredAt()));
        dto.setAutomatic(GenericValidator.isBlankOrNull(log.getTriggeredByUserId()));
        dto.setSentBy(userName(log.getTriggeredByUserId()));
        dto.setRecipientType(log.getRecipientType() == null ? null : log.getRecipientType().name());
        dto.setRecipientDisplayName(log.getRecipientDisplayName());
        dto.setRecipientEmail(log.getRecipientEmail());
        dto.setRecipientPhone(log.getRecipientPhone());
        dto.setLabNumber(log.getReferenceAccession());
        dto.setWorkflow(log.getReferenceWorkflow());
        dto.setStatus(log.getOverallStatus() == null ? null : log.getOverallStatus().name());
        dto.setResentFromId(log.getResentFromId());
        dto.setReportVersionId(log.getReportVersionId());
        dto.setReportFormat(log.getReportFormat());
        List<SentMessageDTO.ChannelDTO> channels = new ArrayList<>();
        for (NotificationLogChannel row : log.getChannels()) {
            SentMessageDTO.ChannelDTO channel = new SentMessageDTO.ChannelDTO();
            channel.setChannel(row.getChannel().name());
            channel.setStatus(row.getStatus() == ChannelStatus.SUCCESS ? "SENT" : row.getStatus().name());
            channel.setErrorMessage(row.getErrorMessage());
            channel.setAttempts(row.getAttempts());
            channel.setLastAttemptedAt(row.getAttempts() == 0 ? null : iso(row.getLastAttemptedAt()));
            channel.setNextAttemptAt(row.getStatus() == ChannelStatus.QUEUED ? iso(row.getNextAttemptAt()) : null);
            channel.setGatewayMessageId(row.getGatewayMessageId());
            channels.add(channel);
        }
        channels.sort((a, b) -> a.getChannel().compareTo(b.getChannel()));
        dto.setChannels(channels);
        if (withContent) {
            dto.setRenderedSubject(log.getRenderedSubject());
            dto.setRenderedMessage(log.getRenderedMessage());
            List<SentMessageDTO.AttemptDTO> attempts = new ArrayList<>();
            for (NotificationLogAttempt attempt : attemptDAO.findByLog(log.getId())) {
                SentMessageDTO.AttemptDTO row = new SentMessageDTO.AttemptDTO();
                row.setChannel(attempt.getChannel().name());
                row.setAttemptedAt(attempt.getAttemptedAt() == null ? null : attempt.getAttemptedAt().toString());
                row.setSucceeded(attempt.isSucceeded());
                row.setErrorMessage(attempt.getErrorMessage());
                attempts.add(row);
            }
            dto.setAttempts(attempts);
        }
        return dto;
    }

    // ----- helpers

    static String addressFor(String email, String phone, NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL ? email : phone;
    }

    private boolean channelReady(Map<NotificationChannel, DeliverySender> senders, NotificationChannel channel) {
        DeliverySender sender = senders.get(channel);
        if (sender == null) {
            return false;
        }
        return channelDAO.findByChannel(DeliveryChannel.Channel.valueOf(channel.name())).map(sender::isSetUp)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isChannelSetUp(NotificationChannel channel) {
        return channelReady(senders(), channel);
    }

    static OverallStatus overall(Set<NotificationLogChannel> rows) {
        boolean sent = false;
        boolean queued = false;
        boolean failed = false;
        for (NotificationLogChannel row : rows) {
            sent |= row.getStatus() == ChannelStatus.SUCCESS;
            queued |= row.getStatus() == ChannelStatus.QUEUED;
            failed |= row.getStatus() == ChannelStatus.FAILED;
        }
        if (sent) {
            return OverallStatus.SENT;
        }
        if (queued) {
            return OverallStatus.QUEUED;
        }
        return failed ? OverallStatus.FAILED : OverallStatus.SKIPPED;
    }

    private static String channelName(NotificationChannel channel) {
        return MessageUtil.getMessage("delivery.channel." + channel.name().toLowerCase());
    }

    private String userName(String userId) {
        if (GenericValidator.isBlankOrNull(userId)) {
            return null;
        }
        try {
            SystemUser user = systemUserService.getUserById(userId);
            return user == null ? userId : user.getNameForDisplay();
        } catch (RuntimeException e) {
            return userId;
        }
    }

    private static String iso(Timestamp value) {
        return value == null ? null : value.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime().toString();
    }

    private static String serializeVariables(Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        try {
            return JSON.writeValueAsString(variables);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return null;
        }
    }

    private static Map<String, String> parseVariables(String json) {
        if (GenericValidator.isBlankOrNull(json)) {
            return null;
        }
        try {
            return JSON.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
