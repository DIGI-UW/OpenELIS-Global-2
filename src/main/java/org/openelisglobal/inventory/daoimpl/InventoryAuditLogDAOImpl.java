package org.openelisglobal.inventory.daoimpl;

import java.sql.Timestamp;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.dao.InventoryAuditLogDAO;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog.EntityType;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog.OperationType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryAuditLogDAOImpl extends BaseDAOImpl<InventoryAuditLog, Long> implements InventoryAuditLogDAO {

    private static final String ENTITY_NAME = "InventoryAuditLog";

    private static final String BASE_SELECT = "SELECT a FROM " + ENTITY_NAME + " a";

    public InventoryAuditLogDAOImpl() {
        super(InventoryAuditLog.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryAuditLog> getByEntity(EntityType entityType, Long entityId) {
        try {
            String hql = BASE_SELECT
                    + " WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.timestamp DESC";
            Query<InventoryAuditLog> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryAuditLog.class);
            query.setParameter("entityType", entityType.name());
            query.setParameter("entityId", entityId);
            return query.list();
        } catch (RuntimeException e) {
            handleException(e, "getByEntity");
            throw new LIMSRuntimeException("Error in getByEntity", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryAuditLog> getByItemId(Long itemId) {
        try {
            String hql = BASE_SELECT + " WHERE a.itemId = :itemId ORDER BY a.timestamp DESC";
            Query<InventoryAuditLog> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryAuditLog.class);
            query.setParameter("itemId", itemId);
            return query.list();
        } catch (RuntimeException e) {
            handleException(e, "getByItemId");
            throw new LIMSRuntimeException("Error in getByItemId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryAuditLog> getByLotId(Long lotId) {
        try {
            String hql = BASE_SELECT + " WHERE a.lotId = :lotId ORDER BY a.timestamp DESC";
            Query<InventoryAuditLog> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryAuditLog.class);
            query.setParameter("lotId", lotId);
            return query.list();
        } catch (RuntimeException e) {
            handleException(e, "getByLotId");
            throw new LIMSRuntimeException("Error in getByLotId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryAuditLog> getByLocationId(Long locationId) {
        try {
            String hql = BASE_SELECT + " WHERE a.locationId = :locationId ORDER BY a.timestamp DESC";
            Query<InventoryAuditLog> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryAuditLog.class);
            query.setParameter("locationId", locationId);
            return query.list();
        } catch (RuntimeException e) {
            handleException(e, "getByLocationId");
            throw new LIMSRuntimeException("Error in getByLocationId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryAuditLog> getByOperationType(OperationType operationType) {
        try {
            String hql = BASE_SELECT + " WHERE a.operationType = :operationType ORDER BY a.timestamp DESC";
            Query<InventoryAuditLog> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryAuditLog.class);
            query.setParameter("operationType", operationType.name());
            return query.list();
        } catch (RuntimeException e) {
            handleException(e, "getByOperationType");
            throw new LIMSRuntimeException("Error in getByOperationType", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryAuditLog> getByUser(Integer userId) {
        try {
            String hql = BASE_SELECT + " WHERE a.performedByUser = :userId ORDER BY a.timestamp DESC";
            Query<InventoryAuditLog> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryAuditLog.class);
            query.setParameter("userId", userId);
            return query.list();
        } catch (RuntimeException e) {
            handleException(e, "getByUser");
            throw new LIMSRuntimeException("Error in getByUser", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryAuditLog> getByEntityAndDateRange(EntityType entityType, Long entityId, Timestamp startDate,
            Timestamp endDate) {
        try {
            String hql = BASE_SELECT + " WHERE a.entityType = :entityType AND a.entityId = :entityId "
                    + "AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC";
            Query<InventoryAuditLog> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryAuditLog.class);
            query.setParameter("entityType", entityType.name());
            query.setParameter("entityId", entityId);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (RuntimeException e) {
            handleException(e, "getByEntityAndDateRange");
            throw new LIMSRuntimeException("Error in getByEntityAndDateRange", e);
        }
    }

}
