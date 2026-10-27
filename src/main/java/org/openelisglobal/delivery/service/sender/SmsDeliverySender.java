package org.openelisglobal.delivery.service.sender;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.delivery.service.sender.DeliveryException.Code;
import org.openelisglobal.delivery.valueholder.DeliveryChannel;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notification.service.sender.SMSNotificationSender;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationPayload;
import org.openelisglobal.notification.valueholder.SMSNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SMS through the gateway the laboratory already runs (Ozeki, SMPP or BMP,
 * configured by their properties). The channel row only adds the default
 * country code; the gateway fields move here with their own story.
 */
@Component
public class SmsDeliverySender implements DeliverySender {

    @Value("${org.openelisglobal.ozeki.active:false}")
    private boolean ozekiActive;

    @Autowired
    private SMSNotificationSender smsNotificationSender;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean isSetUp(DeliveryChannel config) {
        if (config == null || !config.isActive()) {
            return false;
        }
        ConfigurationProperties properties = ConfigurationProperties.getInstance();
        return ozekiActive
                || Boolean.TRUE.toString()
                        .equalsIgnoreCase(properties.getPropertyValue(Property.PATIENT_RESULTS_SMPP_SMS_ENABLED))
                || Boolean.TRUE.toString()
                        .equalsIgnoreCase(properties.getPropertyValue(Property.PATIENT_RESULTS_BMP_SMS_ENABLED));
    }

    @Override
    public String send(DeliveryChannel config, DeliveryMessage message) {
        if (!isSetUp(config)) {
            throw new DeliveryException(Code.NOT_SET_UP,
                    MessageUtil.getMessage("delivery.error.notSetUp", MessageUtil.getMessage("delivery.channel.sms")));
        }
        if (GenericValidator.isBlankOrNull(message.getAddress())) {
            throw new DeliveryException(Code.REJECTED, MessageUtil.getMessage("delivery.error.noAddress"));
        }
        SMSNotification notification = new SMSNotification();
        notification.setReceiverPhoneNumber(internationalNumber(message.getAddress(), config.getCountryCode()));
        notification.setPayload(NotificationPayload.of(message.getSubject(), message.getBody()));
        try {
            smsNotificationSender.send(notification);
            return null;
        } catch (RuntimeException e) {
            throw new DeliveryException(Code.OTHER,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), e);
        }
    }

    /**
     * Digits only, with the lab's default country code in front of a local number
     * (leading 0 dropped). A number that cannot be made international is returned
     * as typed; the gateway refuses it and the row says why.
     */
    static String internationalNumber(String raw, String countryCode) {
        StringBuilder digits = new StringBuilder();
        boolean plus = raw.trim().startsWith("+");
        for (char ch : raw.toCharArray()) {
            if (Character.isDigit(ch)) {
                digits.append(ch);
            }
        }
        if (plus || GenericValidator.isBlankOrNull(countryCode)) {
            return (plus ? "+" : "") + digits;
        }
        String code = countryCode.replace("+", "").trim();
        String local = digits.toString();
        if (local.startsWith(code)) {
            return "+" + local;
        }
        if (local.startsWith("0")) {
            local = local.substring(1);
        }
        return "+" + code + local;
    }
}
