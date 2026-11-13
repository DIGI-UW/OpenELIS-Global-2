package org.openelisglobal.notification.service.sender;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.apache.commons.validator.GenericValidator;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.PDUStringException;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.MessageClass;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.TimeFormatter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.delivery.service.sender.DeliveryException;
import org.openelisglobal.delivery.service.sender.DeliveryException.Code;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notification.valueholder.SMSNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sends an SMS through the SMPP server set up under External Connections
 * (SMPP_SERVER). A send that does not reach the SMSC throws a
 * {@link DeliveryException} saying why, so the delivery outbox records it as
 * failed and retries it.
 */
@Component
public class SMPPNotificationSender {

    static final int DEFAULT_SMPP_PORT = 2775;
    static final int MAX_SYSTEM_ID_LENGTH = 15;
    static final int MAX_PASSWORD_LENGTH = 8;

    @Value("${org.openelisglobal.smsc.serviceType:CMT}")
    private String serviceType;

    @Value("${org.openelisglobal.smsc.bindParamSystemType:}")
    private String bindParamSystemType;

    private static final TimeFormatter TIME_FORMATTER = new AbsoluteTimeFormatter();

    public void send(SMSNotification notification) {
        ConfigurationProperties properties = ConfigurationProperties.getInstance();
        URI smsc = smscAddress(properties.getPropertyValue(Property.PATIENT_RESULTS_SMPP_SMS_ADDRESS));
        int port = smsc.getPort() == -1 ? DEFAULT_SMPP_PORT : smsc.getPort();
        String username = properties.getPropertyValue(Property.PATIENT_RESULTS_SMPP_SMS_USERNAME);
        String password = properties.getPropertyValue(Property.PATIENT_RESULTS_SMPP_SMS_PASSWORD);
        if (tooLong(username, MAX_SYSTEM_ID_LENGTH) || tooLong(password, MAX_PASSWORD_LENGTH)) {
            throw new DeliveryException(Code.AUTHENTICATION,
                    MessageUtil.getMessage("delivery.error.smppCredentialsTooLong"));
        }
        SMPPSession session = new SMPPSession();
        try {
            String systemId = session.connectAndBind(smsc.getHost(), port, new BindParameter(BindType.BIND_TX, username,
                    password, bindParamSystemType, TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, null));
            LogEvent.logDebug(this.getClass().getSimpleName(), "send",
                    "Connected with SMPP with system id {" + systemId + "}");
        } catch (IOException e) {
            session.unbindAndClose();
            throw bindFailure(e, smsc.getHost(), port);
        }
        try {
            String messageId = session.submitShortMessage(serviceType, TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN, "OpenELIS", TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN,
                    notification.getReceiverPhoneNumber(), new ESMClass(), (byte) 0, (byte) 1,
                    TIME_FORMATTER.format(new Date()), null, new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0, new GeneralDataCoding(Alphabet.ALPHA_DEFAULT, MessageClass.CLASS1, false), (byte) 0,
                    notification.getMessage().getBytes(StandardCharsets.UTF_8));
            LogEvent.logDebug(this.getClass().getSimpleName(), "send", "sms messageId: " + messageId);
        } catch (NegativeResponseException e) {
            throw new DeliveryException(Code.REJECTED, MessageUtil.getMessage("delivery.error.gatewayRefused",
                    new Object[] { "SMPP", refusalReason(e.getCommandStatus(), e.getMessage()) }), e);
        } catch (PDUException | ResponseTimeoutException | InvalidResponseException | IOException
                | IllegalArgumentException e) {
            throw new DeliveryException(Code.OTHER,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), e);
        } finally {
            session.unbindAndClose();
        }
    }

    private static boolean tooLong(String value, int max) {
        return value != null && value.getBytes(StandardCharsets.US_ASCII).length > max;
    }

    static String refusalReason(int commandStatus, String fallback) {
        String status = String.format("0x%08X", commandStatus);
        String key = "delivery.smpp.status." + commandStatus;
        String words = MessageUtil.getMessage(key);
        if (words == null || words.isBlank() || words.equals(key)) {
            return fallback == null ? status : fallback;
        }
        return words + " (" + status + ")";
    }

    static URI smscAddress(String address) {
        if (GenericValidator.isBlankOrNull(address)) {
            throw new DeliveryException(Code.NOT_SET_UP, MessageUtil.getMessage("delivery.error.notSetUp", "SMPP"));
        }
        String trimmed = address.trim();
        URI uri;
        try {
            uri = URI.create(trimmed.contains("://") ? trimmed : "smpp://" + trimmed);
        } catch (IllegalArgumentException e) {
            throw new DeliveryException(Code.NOT_SET_UP, MessageUtil.getMessage("delivery.error.notSetUp", "SMPP"), e);
        }
        if (GenericValidator.isBlankOrNull(uri.getHost())) {
            throw new DeliveryException(Code.NOT_SET_UP, MessageUtil.getMessage("delivery.error.notSetUp", "SMPP"));
        }
        return uri;
    }

    private static DeliveryException bindFailure(IOException e, String host, int port) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof NegativeResponseException || cause instanceof PDUStringException) {
                return new DeliveryException(Code.AUTHENTICATION, MessageUtil.getMessage("delivery.error.auth"), e);
            }
            if (cause instanceof ConnectException || cause instanceof UnknownHostException) {
                return new DeliveryException(Code.UNREACHABLE, MessageUtil.getMessage("delivery.error.unreachable",
                        new Object[] { host, String.valueOf(port) }), e);
            }
        }
        String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (reason.toLowerCase().contains("negative bind response")) {
            return new DeliveryException(Code.AUTHENTICATION, MessageUtil.getMessage("delivery.error.auth"), e);
        }
        return new DeliveryException(Code.OTHER, reason, e);
    }
}
