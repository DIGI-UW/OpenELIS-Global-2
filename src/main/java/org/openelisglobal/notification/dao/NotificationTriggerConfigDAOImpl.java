package org.openelisglobal.notification.dao;

import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notification.valueholder.NotificationTriggerConfig;
import org.springframework.stereotype.Component;

@Component
public class NotificationTriggerConfigDAOImpl extends BaseDAOImpl<NotificationTriggerConfig, Integer>
        implements NotificationTriggerConfigDAO {

    public NotificationTriggerConfigDAOImpl() {
        super(NotificationTriggerConfig.class);
    }

    @Override
    public Optional<NotificationTriggerConfig> findByEventCode(String eventCode) {
        try {
            String hql = "FROM NotificationTriggerConfig ntc WHERE ntc.eventCode = :eventCode";
            Query<NotificationTriggerConfig> query = entityManager.unwrap(Session.class).createQuery(hql,
                    NotificationTriggerConfig.class);
            query.setParameter("eventCode", eventCode);
            return Optional.ofNullable(query.uniqueResult());
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NotificationTriggerConfigDAOImpl findByEventCode()", e);
        }
    }
}
