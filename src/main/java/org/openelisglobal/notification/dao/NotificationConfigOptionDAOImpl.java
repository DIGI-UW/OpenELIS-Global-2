package org.openelisglobal.notification.dao;

import java.util.Arrays;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notification.valueholder.NotificationConfigOption;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@SuppressWarnings("unused")
public class NotificationConfigOptionDAOImpl extends BaseDAOImpl<NotificationConfigOption, Integer>
        implements NotificationConfigOptionDAO {

    public NotificationConfigOptionDAOImpl() {
        super(NotificationConfigOption.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationConfigOption> getByNature(NotificationNature nature) {
        String hql = "FROM NotificationConfigOption nco WHERE nco.notificationNature = :nature";
        Query<NotificationConfigOption> query = entityManager.unwrap(Session.class).createQuery(hql,
                NotificationConfigOption.class);
        // Convert enum to string explicitly to avoid Hibernate type binding issues
        query.setParameter("nature", nature.name());
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationConfigOption> getAllAlertConfigs() {
        String hql = "FROM NotificationConfigOption nco WHERE nco.notificationNature IN :natures";
        Query<NotificationConfigOption> query = entityManager.unwrap(Session.class).createQuery(hql,
                NotificationConfigOption.class);
        query.setParameterList("natures",
                Arrays.asList(NotificationNature.FREEZER_TEMPERATURE_ALERT, NotificationNature.EQUIPMENT_ALERT,
                        NotificationNature.INVENTORY_ALERT).stream().map(e -> e.toString())
                        .collect(java.util.stream.Collectors.toList()));
        return query.list();
    }
}
