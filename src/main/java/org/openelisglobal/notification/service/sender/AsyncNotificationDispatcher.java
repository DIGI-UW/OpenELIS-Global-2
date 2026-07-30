package org.openelisglobal.notification.service.sender;

import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notification.valueholder.RemoteNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends an already-prepared {@link RemoteNotification} through the matching
 * {@link ClientNotificationSender} off the request thread, so SMS/Email network
 * calls (SMTP / SMS gateway) don't block result entry — mirroring how the
 * default test-notification flow runs its sends asynchronously.
 *
 * <p>
 * Runs on a separate thread with no Hibernate session, so the notification must
 * already carry a fully-resolved payload (plain strings); do not access lazy
 * entity associations from here.
 */
@Component
public class AsyncNotificationDispatcher {

    // Optional so callers still function (no external send) when no sender is
    // wired in the deployment.
    @SuppressWarnings("rawtypes")
    @Autowired(required = false)
    private List<ClientNotificationSender> notificationSenders;

    @Async
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void dispatch(RemoteNotification notification) {
        if (notificationSenders == null) {
            return;
        }
        for (ClientNotificationSender sender : notificationSenders) {
            try {
                if (sender.forClass().isInstance(notification)) {
                    sender.send(notification);
                }
            } catch (RuntimeException e) {
                LogEvent.logError(e);
            }
        }
    }
}
