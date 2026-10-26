package org.openelisglobal.delivery.service.sender;

import org.openelisglobal.delivery.valueholder.DeliveryChannel;
import org.openelisglobal.notification.valueholder.NotificationChannel;

/** One transport. Implementations read their settings from the channel row. */
public interface DeliverySender {

    NotificationChannel channel();

    /** True when the channel row holds enough to attempt a send. */
    boolean isSetUp(DeliveryChannel config);

    /**
     * Sends one message. Returns the gateway's message id when it gives one,
     * otherwise null. Throws {@link DeliveryException} on every path where the
     * message did not go out.
     */
    String send(DeliveryChannel config, DeliveryMessage message);
}
