package org.openelisglobal.coldstorage.dao;

import java.time.OffsetDateTime;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.coldstorage.valueholder.CorrectiveAction;
import org.openelisglobal.coldstorage.valueholder.CorrectiveActionStatus;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data access implementation for corrective actions on cold storage devices.
 * Provides queries by freezer, status, date range, and dashboard views.
 */
@Component
@Transactional
public class CorrectiveActionDAOImpl extends BaseDAOImpl<CorrectiveAction, Long> implements CorrectiveActionDAO {

    private static final Logger logger = LoggerFactory.getLogger(CorrectiveActionDAOImpl.class);

    public CorrectiveActionDAOImpl() {
        super(CorrectiveAction.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorrectiveAction> getActionsByFreezerId(Long freezerId) {
        try {
            String hql = "FROM CorrectiveAction ca " + "LEFT JOIN FETCH ca.createdBy " + "LEFT JOIN FETCH ca.updatedBy "
                    + "WHERE ca.freezer.id = :freezerId " + "ORDER BY ca.createdAt DESC";
            Query<CorrectiveAction> query = entityManager.unwrap(Session.class).createQuery(hql,
                    CorrectiveAction.class);
            query.setParameter("freezerId", freezerId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving corrective actions for freezer ID: {}", freezerId, e);
            throw new LIMSRuntimeException("Error retrieving corrective actions for freezer ID: " + freezerId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorrectiveAction> getActionsByStatus(CorrectiveActionStatus status) {
        try {
            String sql = "SELECT * FROM clinlims.corrective_action ca "
                    + "WHERE ca.status = :status ORDER BY ca.created_at DESC";

            Query<CorrectiveAction> query = entityManager.unwrap(Session.class).createNativeQuery(sql,
                    CorrectiveAction.class);
            query.setParameter("status", status.name());
            List<CorrectiveAction> actions = query.list();

            // Eagerly initialize lazy associations within transaction
            actions.forEach(action -> {
                if (action.getCreatedBy() != null) {
                    org.hibernate.Hibernate.initialize(action.getCreatedBy());
                }
                if (action.getUpdatedBy() != null) {
                    org.hibernate.Hibernate.initialize(action.getUpdatedBy());
                }
            });

            return actions;
        } catch (Exception e) {
            logger.error("Error retrieving corrective actions by status: {}", status, e);
            throw new LIMSRuntimeException("Error retrieving corrective actions by status: " + status, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorrectiveAction> getActionsByDateRange(OffsetDateTime startDate, OffsetDateTime endDate) {
        try {
            String hql = "FROM CorrectiveAction ca " + "LEFT JOIN FETCH ca.createdBy " + "LEFT JOIN FETCH ca.updatedBy "
                    + "WHERE ca.createdAt BETWEEN :startDate AND :endDate " + "ORDER BY ca.createdAt DESC";
            Query<CorrectiveAction> query = entityManager.unwrap(Session.class).createQuery(hql,
                    CorrectiveAction.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving corrective actions by date range: {} to {}", startDate, endDate, e);
            throw new LIMSRuntimeException(
                    "Error retrieving corrective actions for date range: " + startDate + " to " + endDate, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorrectiveAction> getAllActions() {
        try {
            String hql = "FROM CorrectiveAction ca " + "LEFT JOIN FETCH ca.createdBy " + "LEFT JOIN FETCH ca.updatedBy "
                    + "ORDER BY ca.createdAt DESC";
            Query<CorrectiveAction> query = entityManager.unwrap(Session.class).createQuery(hql,
                    CorrectiveAction.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving all corrective actions", e);
            throw new LIMSRuntimeException("Error retrieving all corrective actions", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long countPendingActions() {
        try {
            String sql = "SELECT COUNT(*) FROM clinlims.corrective_action ca WHERE ca.status = 'PENDING'";

            @SuppressWarnings("unchecked")
            Query<Number> query = (Query<Number>) entityManager.unwrap(Session.class).createNativeQuery(sql);

            Number count = query.uniqueResult();
            return count != null ? count.longValue() : 0L;
        } catch (Exception e) {
            logger.error("Error counting pending corrective actions", e);
            throw new LIMSRuntimeException("Error counting pending actions", e);
        }
    }
}
