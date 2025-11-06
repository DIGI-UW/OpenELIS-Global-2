package org.openelisglobal.notification.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;
import org.springframework.stereotype.Component;

@Component
public class NotificationPayloadTemplateDAOImpl extends BaseDAOImpl<NotificationPayloadTemplate, Integer>
        implements NotificationPayloadTemplateDAO {

    public NotificationPayloadTemplateDAOImpl() {
        super(NotificationPayloadTemplate.class);
    }

    @Override
    public NotificationPayloadTemplate getSystemDefaultPayloadTemplateForType(NotificationPayloadType type) {
        List<NotificationPayloadTemplate> data;
        try {
            String sql = "from NotificationPayloadTemplate as npt where npt.type = :type order by npt.id asc";
            Query<NotificationPayloadTemplate> query = entityManager.unwrap(Session.class).createQuery(sql,
                    NotificationPayloadTemplate.class);
            query.setParameter("type", type.name());
            query.setFirstResult(0);
            query.setMaxResults(1);
            data = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(
                    "Error in TestNotificationConfigDAOImpl getTestNotificationConfigForTestId()", e);
        }

        if (data.size() == 0) {
            return null;
        } else {
            return data.get(0);
        }
    }
}
