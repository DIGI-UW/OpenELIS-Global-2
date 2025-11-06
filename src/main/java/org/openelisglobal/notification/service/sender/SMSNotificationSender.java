package org.openelisglobal.notification.service.sender;

import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.notification.valueholder.SMSNotification;
import org.ozeki.sms.service.OzekiMessageOutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SMSNotificationSender implements ClientNotificationSender<SMSNotification> {

    @Value("${org.openelisglobal.ozeki.active:false}")
    private Boolean ozekiActive;

    @Autowired
    private SMPPNotificationSender smppNotificationSender;
    @Autowired
    private BMPSMSNotificationSender bmpSMSNotificationSender;
    @Autowired
    private OzekiMessageOutService ozekiMessageOutService;

    @Override
    public Class<SMSNotification> forClass() {
        return SMSNotification.class;
    }

    @Override
    public void send(SMSNotification notification) {
        ConfigurationProperties configurationProperties = ConfigurationProperties.getInstance();
        // always try to send via Ozeki
        if (ozekiActive) {
            ozekiMessageOutService.send(notification);
        } else if (Boolean.TRUE.toString().equalsIgnoreCase(
                configurationProperties.getPropertyValue(Property.PATIENT_RESULTS_SMPP_SMS_ENABLED))) {
            smppNotificationSender.send(notification);
        } else if (Boolean.TRUE.toString()
                .equalsIgnoreCase(configurationProperties.getPropertyValue(Property.PATIENT_RESULTS_BMP_SMS_ENABLED))) {
            bmpSMSNotificationSender.send(notification);
        }
    }
}
