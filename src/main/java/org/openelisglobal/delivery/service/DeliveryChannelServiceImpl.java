package org.openelisglobal.delivery.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.delivery.dao.DeliveryChannelDAO;
import org.openelisglobal.delivery.dto.DeliveryChannelDTO;
import org.openelisglobal.delivery.service.sender.DeliveryException;
import org.openelisglobal.delivery.service.sender.DeliveryMessage;
import org.openelisglobal.delivery.service.sender.DeliverySender;
import org.openelisglobal.delivery.valueholder.DeliveryChannel;
import org.openelisglobal.delivery.valueholder.DeliveryChannel.Channel;
import org.openelisglobal.delivery.valueholder.DeliveryChannel.Security;
import org.openelisglobal.delivery.valueholder.DeliveryChannel.SmsGateway;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog;
import org.openelisglobal.notification.valueholder.NotificationLog.Product;
import org.openelisglobal.notification.valueholder.NotificationRecipientType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryChannelServiceImpl implements DeliveryChannelService {

    public static final String STATUS_READY = "READY";
    public static final String STATUS_NOT_SET_UP = "NOT_SET_UP";
    public static final String STATUS_FAILING = "FAILING";

    @Autowired
    private DeliveryChannelDAO channelDAO;
    @Autowired
    private DeliveryOutboxService outbox;
    @Autowired
    private List<DeliverySender> senderBeans;

    private Map<NotificationChannel, DeliverySender> senders() {
        Map<NotificationChannel, DeliverySender> byChannel = new EnumMap<>(NotificationChannel.class);
        for (DeliverySender sender : senderBeans) {
            byChannel.put(sender.channel(), sender);
        }
        return byChannel;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryChannelDTO> list() {
        List<DeliveryChannelDTO> rows = new ArrayList<>();
        for (DeliveryChannel channel : channelDAO.findAllOrdered()) {
            rows.add(toDTO(channel));
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeliveryChannelDTO> get(Channel channel) {
        return channelDAO.findByChannel(channel).map(this::toDTO);
    }

    /**
     * A changed connection setting retires the last failure: the status then
     * describes the configuration the laboratory just saved, until its next send.
     */
    @Override
    @Transactional
    public DeliveryChannelDTO save(Channel channel, DeliveryChannelDTO wanted, String sysUserId) {
        DeliveryChannel row = channelDAO.findByChannel(channel).orElseGet(() -> {
            DeliveryChannel created = new DeliveryChannel();
            created.setChannel(channel);
            return created;
        });
        String before = connectionSignature(row);
        row.setActive(wanted.isActive());
        switch (channel) {
        case EMAIL -> {
            row.setHost(trim(wanted.getHost()));
            row.setPort(wanted.getPort());
            row.setSecurity(parse(Security.class, wanted.getSecurity(), Security.STARTTLS));
            row.setUsername(trim(wanted.getUsername()));
            row.setSenderAddress(trim(wanted.getSenderAddress()));
            row.setSenderName(trim(wanted.getSenderName()));
            row.setReplyTo(trim(wanted.getReplyTo()));
            if (row.getPort() != null && (row.getPort() < 1 || row.getPort() > 65535)) {
                throw new IllegalArgumentException("port");
            }
            if (!GenericValidator.isBlankOrNull(row.getSenderAddress()) && !isEmail(row.getSenderAddress())) {
                throw new IllegalArgumentException("senderAddress");
            }
            if (!GenericValidator.isBlankOrNull(row.getReplyTo()) && !isEmail(row.getReplyTo())) {
                throw new IllegalArgumentException("replyTo");
            }
        }
        case WHATSAPP -> {
            row.setHost(trim(wanted.getHost()));
            row.setUsername(trim(wanted.getUsername()));
            row.setSenderAddress(trim(wanted.getSenderAddress()));
        }
        case SMS -> {
            row.setGateway(parse(SmsGateway.class, wanted.getGateway(), null));
            row.setCountryCode(trim(wanted.getCountryCode()));
            if (!GenericValidator.isBlankOrNull(row.getCountryCode())
                    && !row.getCountryCode().matches("\\+?[0-9]{1,4}")) {
                throw new IllegalArgumentException("countryCode");
            }
        }
        }
        boolean secretChanged = false;
        if (!GenericValidator.isBlankOrNull(wanted.getSecret())) {
            secretChanged = !wanted.getSecret().equals(row.getSecret());
            row.setSecret(wanted.getSecret());
        }
        if (secretChanged || !before.equals(connectionSignature(row))) {
            row.setLastFailureAt(null);
            row.setLastError(null);
        }
        row.setSysUserId(sysUserId);
        return toDTO(channelDAO.save(row));
    }

    private static String connectionSignature(DeliveryChannel row) {
        return String.join("|", String.valueOf(row.getHost()), String.valueOf(row.getPort()),
                String.valueOf(row.getSecurity()), String.valueOf(row.getUsername()),
                String.valueOf(row.getSenderAddress()), String.valueOf(row.getGateway()));
    }

    @Override
    public TestOutcome sendTest(Channel channel, String address, String sysUserId) {
        NotificationChannel notificationChannel = NotificationChannel.valueOf(channel.name());
        DeliverySender sender = senders().get(notificationChannel);
        DeliveryChannel config = channelDAO.findByChannel(channel).orElse(null);
        String subject = MessageUtil.getMessage("delivery.test.subject");
        String body = MessageUtil.getMessage("delivery.test.body");
        boolean success;
        String outcome;
        String gatewayId = null;
        try {
            if (sender == null) {
                throw new DeliveryException(DeliveryException.Code.NOT_SET_UP,
                        MessageUtil.getMessage("delivery.error.notSetUp",
                                MessageUtil.getMessage("delivery.channel." + channel.name().toLowerCase())));
            }
            gatewayId = sender.send(config, new DeliveryMessage(notificationChannel, address, subject, body));
            success = true;
            outcome = MessageUtil.getMessage("delivery.test.success", address);
        } catch (DeliveryException e) {
            success = false;
            outcome = e.getMessage();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            success = false;
            outcome = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
        channelDAO.recordOutcome(channel, success, success ? null : outcome);
        DeliveryRequest request = new DeliveryRequest().setProduct(Product.TEST).setEventCode("TEST")
                .setRecipientType(NotificationRecipientType.TYPED_ADDRESS).setRecipientDisplayName(address)
                .setRecipientEmail(notificationChannel == NotificationChannel.EMAIL ? address : null)
                .setRecipientPhone(notificationChannel == NotificationChannel.EMAIL ? null : address)
                .setSubject(subject).setMessage(body).setSentByUserId(sysUserId);
        NotificationLog log = outbox.recordImmediate(request, notificationChannel, success, success ? null : outcome,
                gatewayId);
        return new TestOutcome(success, outcome, log.getId());
    }

    private DeliveryChannelDTO toDTO(DeliveryChannel channel) {
        DeliveryChannelDTO dto = new DeliveryChannelDTO();
        dto.setChannel(channel.getChannel().name());
        dto.setActive(channel.isActive());
        dto.setHost(channel.getHost());
        dto.setPort(channel.getPort());
        dto.setSecurity(channel.getSecurity() == null ? null : channel.getSecurity().name());
        dto.setUsername(channel.getUsername());
        dto.setSecretSet(!GenericValidator.isBlankOrNull(channel.getSecret()));
        dto.setSenderAddress(channel.getSenderAddress());
        dto.setSenderName(channel.getSenderName());
        dto.setReplyTo(channel.getReplyTo());
        dto.setGateway(channel.getGateway() == null ? null : channel.getGateway().name());
        dto.setCountryCode(channel.getCountryCode());
        dto.setLastSuccessAt(channel.getLastSuccessAt() == null ? null : channel.getLastSuccessAt().toString());
        dto.setLastFailureAt(channel.getLastFailureAt() == null ? null : channel.getLastFailureAt().toString());
        dto.setLastError(channel.getLastError());
        dto.setEditable(channel.getChannel() != Channel.SMS);
        DeliverySender sender = senders().get(NotificationChannel.valueOf(channel.getChannel().name()));
        boolean setUp = sender != null && sender.isSetUp(channel);
        boolean failing = channel.getLastFailureAt() != null && (channel.getLastSuccessAt() == null
                || channel.getLastFailureAt().isAfter(channel.getLastSuccessAt()));
        dto.setStatus(!setUp ? STATUS_NOT_SET_UP : failing ? STATUS_FAILING : STATUS_READY);
        return dto;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, E fallback) {
        if (GenericValidator.isBlankOrNull(value)) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(type.getSimpleName().toLowerCase());
        }
    }

    private static String trim(String value) {
        return GenericValidator.isBlankOrNull(value) ? null : value.trim();
    }

    private static boolean isEmail(String value) {
        return value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
}
