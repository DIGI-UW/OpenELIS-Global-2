package org.openelisglobal.inventory.dao;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.LotStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryLotDAOImpl extends BaseDAOImpl<InventoryLot, String> implements InventoryLotDAO {

    private static final Logger logger = LoggerFactory.getLogger(InventoryLotDAOImpl.class);

    public InventoryLotDAOImpl() {
        super(InventoryLot.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> findByInventoryItemId(String inventoryItemId) {
        try {
            String hql = "FROM InventoryLot l WHERE l.inventoryItem.id = :itemId ORDER BY l.expirationDate";
            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("itemId", inventoryItemId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding lots by inventory item ID", e);
            throw new LIMSRuntimeException("Error finding lots by inventory item ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> findAvailableLotsByItemFEFO(String inventoryItemId) {
        try {
            String hql = "FROM InventoryLot l WHERE l.inventoryItem.id = :itemId " + "AND l.status = :status "
                    + "AND l.qcStatus = :qcStatus " + "AND l.currentQuantity > 0 " + "ORDER BY l.expirationDate ASC";
            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("itemId", inventoryItemId);
            query.setParameter("status", LotStatus.ACTIVE);
            query.setParameter("qcStatus", org.openelisglobal.inventory.valueholder.QCStatus.PASSED);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding available lots by FEFO", e);
            throw new LIMSRuntimeException("Error finding available lots by FEFO", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryLot findByBarcode(String barcode) {
        try {
            String hql = "FROM InventoryLot l WHERE l.barcode = :barcode";
            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("barcode", barcode);
            query.setMaxResults(1);
            List<InventoryLot> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding lot by barcode", e);
            throw new LIMSRuntimeException("Error finding lot by barcode", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> findExpiringSoon(int days) {
        try {
            Date futureDate = Date.valueOf(LocalDate.now().plusDays(days));
            Date today = Date.valueOf(LocalDate.now());

            String hql = "FROM InventoryLot l WHERE l.status = :status "
                    + "AND l.expirationDate BETWEEN :today AND :futureDate " + "ORDER BY l.expirationDate ASC";
            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("status", LotStatus.ACTIVE);
            query.setParameter("today", today);
            query.setParameter("futureDate", futureDate);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding lots expiring soon", e);
            throw new LIMSRuntimeException("Error finding lots expiring soon", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> findExpiredActiveLots() {
        try {
            Date today = Date.valueOf(LocalDate.now());

            String hql = "FROM InventoryLot l WHERE l.status = :status " + "AND l.expirationDate < :today "
                    + "ORDER BY l.expirationDate ASC";
            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("status", LotStatus.ACTIVE);
            query.setParameter("today", today);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding expired active lots", e);
            throw new LIMSRuntimeException("Error finding expired active lots", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> findByStatus(LotStatus status) {
        try {
            String hql = "FROM InventoryLot l WHERE l.status = :status ORDER BY l.expirationDate";
            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding lots by status", e);
            throw new LIMSRuntimeException("Error finding lots by status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalAvailableQuantity(String inventoryItemId) {
        try {
            String hql = "SELECT COALESCE(SUM(l.currentQuantity), 0) FROM InventoryLot l "
                    + "WHERE l.inventoryItem.id = :itemId " + "AND l.status = :status " + "AND l.qcStatus = :qcStatus";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("itemId", inventoryItemId);
            query.setParameter("status", LotStatus.ACTIVE);
            query.setParameter("qcStatus", org.openelisglobal.inventory.valueholder.QCStatus.PASSED);
            Long result = query.uniqueResult();
            return result != null ? result.intValue() : 0;
        } catch (Exception e) {
            logger.error("Error getting total available quantity", e);
            throw new LIMSRuntimeException("Error getting total available quantity", e);
        }
    }
}
