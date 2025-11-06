package org.openelisglobal.notifications.dao;

import org.openelisglobal.notifications.entity.NotificationSubscriptions;

public interface NotificationSubscriptionDAO {

    void save(NotificationSubscriptions notificationSubscription);

    NotificationSubscriptions getNotificationSubscriptionByUserId(Long id);

    void updateNotificationSubscription(NotificationSubscriptions notificationSubscription);

    void saveOrUpdate(NotificationSubscriptions notificationSubscription);

    void delete(NotificationSubscriptions ns);

}