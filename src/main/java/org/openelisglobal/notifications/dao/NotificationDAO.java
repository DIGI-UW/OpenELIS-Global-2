package org.openelisglobal.notifications.dao;

import java.util.List;
import org.openelisglobal.notifications.entity.Notification;
import org.openelisglobal.systemuser.valueholder.SystemUser;

public interface NotificationDAO {

    void save(Notification notification);

    List<Notification> getNotifications();

    List<Notification> getNotificationsByUserId(Long userId);

    Notification getNotificationById(Long id);

    void updateNotification(Notification notification);

    void setAllUserNotificationsToRead(Long userId);

    List<SystemUser> getSystemUsers();
}
