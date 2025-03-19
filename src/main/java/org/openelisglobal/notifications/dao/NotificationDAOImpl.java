package org.openelisglobal.notifications.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.OffsetDateTime;
import java.util.List;
import org.openelisglobal.notifications.entity.Notification;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class NotificationDAOImpl implements NotificationDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void save(Notification notification) {
        entityManager.persist(notification);
    }

    @Override
    public List<Notification> getNotifications() {
        TypedQuery<Notification> query = entityManager
                .createQuery("SELECT n FROM Notification n LEFT JOIN FETCH n.user", Notification.class);
        return query.getResultList();
    }

    @Override
    public Notification getNotificationById(Long id) {
        return entityManager.find(Notification.class, id);
    }

    @Override
    public List<Notification> getNotificationsByUserId(Long userId) {
        TypedQuery<Notification> query = entityManager.createQuery(
                "SELECT n FROM Notification n LEFT JOIN FETCH n.user WHERE n.user.id = :userId ORDER BY n.createdDate DESC, n.readAt ASC",
                Notification.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    @Override
    @Transactional
    public void setAllUserNotificationsToRead(Long userId) {
        entityManager.createNativeQuery("UPDATE Notifications " + "SET read_at = :time " + // Remove alias 'n' and
                                                                                           // directly specify column
                                                                                           // name
                "WHERE user_id = :userId").setParameter("time", OffsetDateTime.now()).setParameter("userId", userId)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void updateNotification(Notification notification) {
        entityManager.merge(notification);
    }

    @Override
    public List<SystemUser> getSystemUsers() {
        TypedQuery<SystemUser> query = entityManager.createQuery("SELECT u FROM SystemUser u", SystemUser.class);
        return query.getResultList();
    }
}
