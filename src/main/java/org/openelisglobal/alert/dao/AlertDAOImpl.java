package org.openelisglobal.alert.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AlertDAOImpl extends BaseDAOImpl<Alert, Long> implements AlertDAO {

    private static final Logger logger = LoggerFactory.getLogger(AlertDAOImpl.class);

    public AlertDAOImpl() {
        super(Alert.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> getAlertsByEntity(String entityType, Long entityId) {
        try {
            String hql = "FROM Alert a WHERE a.alertEntityType = :entityType AND a.alertEntityId = :entityId "
                    + "ORDER BY a.startTime DESC";
            Query<Alert> query = entityManager.unwrap(Session.class).createQuery(hql, Alert.class);
            query.setParameter("entityType", entityType);
            query.setParameter("entityId", entityId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving alerts for entity type: {}, ID: {}", entityType, entityId, e);
            throw new LIMSRuntimeException("Error retrieving alerts for entity: " + entityType + "/" + entityId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> getAlertsByAlertType(AlertType alertType) {
        try {
            String hql = "FROM Alert a WHERE a.alertType = :alertType ORDER BY a.startTime DESC";
            Query<Alert> query = entityManager.unwrap(Session.class).createQuery(hql, Alert.class);
            query.setParameter("alertType", alertType);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving alerts by type: {}", alertType, e);
            throw new LIMSRuntimeException("Error retrieving alerts by type: " + alertType, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> getAlertsByStatus(AlertStatus status) {
        try {
            String hql = "FROM Alert a WHERE a.status = :status ORDER BY a.startTime DESC";
            Query<Alert> query = entityManager.unwrap(Session.class).createQuery(hql, Alert.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving alerts by status: {}", status, e);
            throw new LIMSRuntimeException("Error retrieving alerts by status: " + status, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long countActiveAlertsForEntity(String entityType, Long entityId) {
        try {
            String sql = "SELECT COUNT(*) FROM clinlims.alert a " + "WHERE a.alert_entity_type = :entityType "
                    + "AND a.alert_entity_id = :entityId " + "AND a.status IN ('OPEN', 'ACKNOWLEDGED')";

            @SuppressWarnings("unchecked")
            Query<Number> query = (Query<Number>) entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("entityType", entityType);
            query.setParameter("entityId", entityId);

            Number count = query.uniqueResult();
            return count != null ? count.longValue() : 0L;
        } catch (Exception e) {
            logger.error("Error counting active alerts for entity type: {}, ID: {}", entityType, entityId, e);
            throw new LIMSRuntimeException("Error counting active alerts for entity: " + entityType + "/" + entityId,
                    e);
        }
    }
}
