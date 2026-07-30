package org.openelisglobal.notifications.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notifications.dao.NotificationDAO;
import org.openelisglobal.notifications.entity.Notification;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HeaderNotificationServiceImpl implements HeaderNotificationService {

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private UserRoleService userRoleService;

    @Override
    @Transactional
    public void notifyUser(String userId, String message) {
        if (userId == null || message == null) {
            return;
        }
        SystemUser user;
        try {
            user = systemUserService.getUserById(userId);
        } catch (RuntimeException e) {
            user = null;
        }
        if (user == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setMessage(message);
        notification.setUser(user);
        notification.setCreatedDate(OffsetDateTime.now());
        notificationDAO.save(notification);
    }

    @Override
    @Transactional
    public void notifyRole(String roleName, String message) {
        if (roleName == null) {
            return;
        }
        List<String> userIds = userRoleService.getUserIdsForRole(roleName);
        if (userIds == null) {
            return;
        }
        for (String userId : userIds) {
            try {
                notifyUser(userId, message);
            } catch (RuntimeException e) {
                LogEvent.logError(e);
            }
        }
    }
}
