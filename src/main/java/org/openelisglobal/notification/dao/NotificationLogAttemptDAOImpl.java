package org.openelisglobal.notification.dao;

import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notification.valueholder.NotificationLogAttempt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NotificationLogAttemptDAOImpl extends BaseDAOImpl<NotificationLogAttempt, Long>
        implements NotificationLogAttemptDAO {

    public NotificationLogAttemptDAOImpl() {
        super(NotificationLogAttempt.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationLogAttempt> findByLog(Long notificationLogId) {
        try {
            return entityManager
                    .createQuery("FROM NotificationLogAttempt a WHERE a.notificationLogId = :logId"
                            + " ORDER BY a.attemptedAt, a.id", NotificationLogAttempt.class)
                    .setParameter("logId", notificationLogId).getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NotificationLogAttempt findByLog()", e);
        }
    }

    @Override
    public NotificationLogAttempt save(NotificationLogAttempt attempt) {
        try {
            if (attempt.getId() == null) {
                entityManager.persist(attempt);
                return attempt;
            }
            return entityManager.merge(attempt);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NotificationLogAttempt save()", e);
        }
    }
}
