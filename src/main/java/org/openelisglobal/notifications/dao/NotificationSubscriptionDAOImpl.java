package org.openelisglobal.notifications.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.openelisglobal.notifications.entity.NotificationSubscriptions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class NotificationSubscriptionDAOImpl implements NotificationSubscriptionDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void save(NotificationSubscriptions notificationSubscription) {
        entityManager.persist(notificationSubscription);
    }

    @Override
    public NotificationSubscriptions getNotificationSubscriptionByUserId(Long id) {

        try {
            return entityManager.createQuery("SELECT ns FROM NotificationSubscriptions ns WHERE ns.user.id = :id",
                    NotificationSubscriptions.class).setParameter("id", id).getSingleResult();
        } catch (Exception e) {
            return null;
        }

    }

    @Override
    @Transactional
    public void updateNotificationSubscription(NotificationSubscriptions notificationSubscription) {
        entityManager.merge(notificationSubscription);
    }

    @Override
    @Transactional
    public void delete(NotificationSubscriptions ns) {
        // Reattach the entity to the current session
        NotificationSubscriptions attachedEntity = entityManager.find(NotificationSubscriptions.class, ns.getId());

        if (attachedEntity != null) {
            // Now remove the attached entity
            entityManager.remove(attachedEntity);
        } else {
            throw new IllegalArgumentException("Subscription not found or already deleted");
        }
    }

    // Update saveOrUpdate method

    @Transactional
    public void saveOrUpdate(NotificationSubscriptions notificationSubscription) {

        NotificationSubscriptions existingSubscription = getNotificationSubscriptionByUserId(
                Long.valueOf(notificationSubscription.getUser().getId()));

        if (existingSubscription == null) {
            // Create a new subscription
            entityManager.persist(notificationSubscription); // Use persist() for new entities
        } else {
            // Update the existing subscription
            existingSubscription.setPfEndpoint(notificationSubscription.getPfEndpoint());
            existingSubscription.setPfP256dh(notificationSubscription.getPfP256dh());
            existingSubscription.setPfAuth(notificationSubscription.getPfAuth());
            entityManager.merge(existingSubscription); // Use merge() for existing entities
        }
    }

}
