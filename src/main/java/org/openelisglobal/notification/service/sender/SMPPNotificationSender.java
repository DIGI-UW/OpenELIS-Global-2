package org.openelisglobal.notification.service.sender;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
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
import org.openelisglobal.notification.valueholder.SMSNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SMPPNotificationSender {

    @Value("${org.openelisglobal.smsc.serviceType:CMT}")
    private String serviceType;

    @Value("${org.openelisglobal.smsc.bindParamSystemType:}")
    private String bindParamSystemType;

    private static final TimeFormatter TIME_FORMATTER = new AbsoluteTimeFormatter();

    public void send(SMSNotification notification) {
        SMPPSession session;
        try {
            session = initSession();

            try {
                String messageId = session.submitShortMessage(serviceType, TypeOfNumber.UNKNOWN,
                        NumberingPlanIndicator.UNKNOWN, "OpenELIS", TypeOfNumber.UNKNOWN,
                        NumberingPlanIndicator.UNKNOWN, notification.getReceiverPhoneNumber(), new ESMClass(), (byte) 0,
                        (byte) 1, TIME_FORMATTER.format(new Date()), null,
                        new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT), (byte) 0,
                        new GeneralDataCoding(Alphabet.ALPHA_DEFAULT, MessageClass.CLASS1, false), (byte) 0,
                        notification.getMessage().getBytes());

                LogEvent.logDebug(this.getClass().getSimpleName(), "send", "sms messageId: " + messageId);
            } catch (IllegalArgumentException | PDUException | ResponseTimeoutException | InvalidResponseException
                    | NegativeResponseException | IOException e) {
                LogEvent.logError(e);
            }

            session.unbindAndClose();
        } catch (IOException | URISyntaxException e) {
            LogEvent.logError(e);
        }
    }

    private SMPPSession initSession() throws IOException, URISyntaxException {
        ConfigurationProperties configurationProperties = ConfigurationProperties.getInstance();
        SMPPSession session = new SMPPSession();

        String address = configurationProperties.getPropertyValue(Property.PATIENT_RESULTS_BMP_SMS_ADDRESS);
        String username = configurationProperties.getPropertyValue(Property.PATIENT_RESULTS_BMP_SMS_USERNAME);
        String password = configurationProperties.getPropertyValue(Property.PATIENT_RESULTS_BMP_SMS_PASSWORD);
        URI uri = new URI(address);
        String systemId = session.connectAndBind(uri.getHost(), uri.getPort(), new BindParameter(BindType.BIND_TX,
                username, password, bindParamSystemType, TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, null));
        LogEvent.logDebug(this.getClass().getSimpleName(), "initSession",
                "Connected with SMPP with system id {" + systemId + "}");
        return session;
    }
}
