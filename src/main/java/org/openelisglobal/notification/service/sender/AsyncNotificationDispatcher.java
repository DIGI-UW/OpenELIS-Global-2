package org.openelisglobal.notification.service.sender;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.delivery.service.DeliveryOutboxService;
import org.openelisglobal.delivery.service.DeliveryRequest;
import org.openelisglobal.notification.valueholder.EmailNotification;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog.Product;
import org.openelisglobal.notification.valueholder.NotificationRecipientType;
import org.openelisglobal.notification.valueholder.RemoteNotification;
import org.openelisglobal.notification.valueholder.SMSNotification;
import org.openelisglobal.notification.valueholder.WhatsAppNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Queues an already-rendered {@link RemoteNotification} in the delivery outbox,
 * so an alert's email or SMS never blocks result entry and is recorded, retried
 * and visible in Sent Messages like every other message.
 */
@Component
public class AsyncNotificationDispatcher {

    @Autowired
    private DeliveryOutboxService outbox;

    public void dispatch(RemoteNotification notification) {
        dispatch(notification, null, null, NotificationRecipientType.TYPED_ADDRESS, null);
    }

    /**
     * Queues the notification under product Alert with the rule that raised it and
     * the order it concerns.
     */
    public void dispatch(RemoteNotification notification, String ruleName, String accession,
            NotificationRecipientType recipientType, String sysUserId) {
        if (notification == null) {
            return;
        }
        DeliveryRequest request = new DeliveryRequest().setProduct(Product.ALERT).setEventCode("ALERT")
                .setRuleName(ruleName).setReferenceAccession(accession)
                .setRecipientType(recipientType == null ? NotificationRecipientType.TYPED_ADDRESS : recipientType)
                .setSubject(notification.getSubject()).setMessage(notification.getMessage()).setSentByUserId(sysUserId);
        if (notification instanceof EmailNotification email) {
            request.setRecipientEmail(email.getRecipientEmailAddress())
                    .setRecipientDisplayName(email.getRecipientEmailAddress()).addChannel(NotificationChannel.EMAIL);
        } else if (notification instanceof SMSNotification sms) {
            request.setRecipientPhone(sms.getReceiverPhoneNumber())
                    .setRecipientDisplayName(sms.getReceiverPhoneNumber()).addChannel(NotificationChannel.SMS);
        } else if (notification instanceof WhatsAppNotification whatsApp) {
            request.setRecipientPhone(whatsApp.getReceiverPhoneNumber())
                    .setRecipientDisplayName(whatsApp.getReceiverPhoneNumber())
                    .addChannel(NotificationChannel.WHATSAPP);
        } else {
            return;
        }
        try {
            outbox.enqueue(request);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
    }
}
