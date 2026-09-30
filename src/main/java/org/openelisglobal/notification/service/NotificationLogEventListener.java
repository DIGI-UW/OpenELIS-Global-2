package org.openelisglobal.notification.service;

import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Spring {@code @EventListener} bridge for {@link NotificationFiredEvent}.
 * Lives in its own bean (separate from {@link NotificationLogServiceImpl}) so
 * the call to {@code recordFire} crosses bean boundaries and routes through the
 * service proxy.
 *
 * <p>
 * Diagnosed: the dispatcher fires inside the
 * {@code TransactionSynchronization.afterCommit} hook, which still reports
 * {@code isActualTransactionActive()=true} even though the underlying Hibernate
 * session has been committed and closed. Joining that "zombie" tx with default
 * {@code REQUIRED} propagation leaves Hibernate complaining "no transaction is
 * in progress" during persist. {@code recordFire} uses
 * {@code Propagation.REQUIRES_NEW} to suspend the zombie context and open a
 * real new tx.
 *
 * <p>
 * The dispatcher already wraps the publish in try/catch so any failure here
 * cannot block the dispatch path; we log defensively.
 */
@Component
public class NotificationLogEventListener {

    @Autowired
    private NotificationLogService notificationLogService;

    @EventListener
    public void on(NotificationFiredEvent event) {
        try {
            notificationLogService.recordFire(event);
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "on", "failed to persist NotificationLog for event "
                    + event.getEventCode() + " recipient " + event.getRecipientType());
            LogEvent.logError(e);
        }
    }
}
