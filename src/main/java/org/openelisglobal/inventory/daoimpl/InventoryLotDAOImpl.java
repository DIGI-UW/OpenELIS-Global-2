package org.openelisglobal.inventory.daoimpl;

import java.sql.Timestamp;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryLotDAOImpl extends BaseDAOImpl<InventoryLot, Long> implements InventoryLotDAO {

    public InventoryLotDAOImpl() {
        super(InventoryLot.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> getByInventoryItemId(Long itemId) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryLot l WHERE l.inventoryItem.id = :itemId ORDER BY l.expirationDate";
            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("itemId", itemId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting lots by inventory item ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> getAvailableLotsByItemFEFO(Long itemId) throws LIMSRuntimeException {
        try {
            // CRITICAL: FEFO (First Expired, First Out) query
            // Returns lots sorted by earliest expiration date first
            // Only includes lots that are:
            // - ACTIVE or IN_USE status
            // - QC status PASSED
            // - Have quantity available (currentQuantity > 0)
            String hql = "FROM InventoryLot l " + "WHERE l.inventoryItem.id = :itemId "
                    + "AND (l.status = :activeStatus OR l.status = :inUseStatus) " + "AND l.qcStatus = :passedStatus "
                    + "AND l.currentQuantity > 0 "
                    + "ORDER BY l.expirationDate ASC NULLS LAST, l.calculatedExpiryAfterOpening ASC NULLS LAST";

            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("itemId", itemId);
            query.setParameter("activeStatus", LotStatus.ACTIVE.name());
            query.setParameter("inUseStatus", LotStatus.IN_USE.name());
            query.setParameter("passedStatus", QCStatus.PASSED.name());
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting available lots by item (FEFO)", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> getExpiringLots(int daysAhead) throws LIMSRuntimeException {
        try {
            Timestamp futureDate = new Timestamp(System.currentTimeMillis() + ((long) daysAhead * 24 * 60 * 60 * 1000));
            Timestamp now = new Timestamp(System.currentTimeMillis());

            String hql = "FROM InventoryLot l " + "WHERE (l.status = :activeStatus OR l.status = :inUseStatus) "
                    + "AND (l.expirationDate BETWEEN :now AND :futureDate "
                    + "     OR l.calculatedExpiryAfterOpening BETWEEN :now AND :futureDate) "
                    + "ORDER BY l.expirationDate ASC NULLS LAST";

            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("activeStatus", LotStatus.ACTIVE.name());
            query.setParameter("inUseStatus", LotStatus.IN_USE.name());
            query.setParameter("now", now);
            query.setParameter("futureDate", futureDate);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting expiring lots", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> getExpiredActiveLots() throws LIMSRuntimeException {
        try {
            Timestamp now = new Timestamp(System.currentTimeMillis());

            String hql = "FROM InventoryLot l " + "WHERE l.status = :activeStatus "
                    + "AND (l.expirationDate < :now OR l.calculatedExpiryAfterOpening < :now) "
                    + "ORDER BY l.expirationDate";

            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("activeStatus", LotStatus.ACTIVE.name());
            query.setParameter("now", now);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting expired active lots", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryLot getByLotNumber(String lotNumber) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryLot l WHERE l.lotNumber = :lotNumber";
            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("lotNumber", lotNumber);
            query.setMaxResults(1);
            List<InventoryLot> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting lot by lot number", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryLot getByBarcode(String barcode) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryLot l WHERE l.barcode = :barcode";
            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("barcode", barcode);
            query.setMaxResults(1);
            List<InventoryLot> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting lot by barcode", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> getByStorageLocationId(Long locationId) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryLot l WHERE l.storageLocation.id = :locationId ORDER BY l.expirationDate";
            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("locationId", locationId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting lots by storage location ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> getByQCStatus(QCStatus qcStatus) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryLot l WHERE l.qcStatus = :qcStatus ORDER BY l.expirationDate";
            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("qcStatus", qcStatus.name());
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting lots by QC status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> getByStatus(LotStatus status) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryLot l WHERE l.status = :status ORDER BY l.expirationDate";
            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("status", status.name());
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting lots by status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalCurrentQuantity(Long itemId) throws LIMSRuntimeException {
        try {
            String sql = "SELECT COALESCE(SUM(current_quantity), 0.0) FROM clinlims.inventory_lot "
                    + "WHERE inventory_item_id = ?1 " + "AND status IN ('ACTIVE', 'IN_USE')";

            Number result = (Number) entityManager.createNativeQuery(sql).setParameter(1, itemId).getSingleResult();

            return result != null ? result.intValue() : 0;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting total current quantity", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryLot getByFhirUuid(String fhirUuid) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryLot l WHERE l.fhirUuid = :fhirUuid";
            Query<InventoryLot> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryLot.class);
            query.setParameter("fhirUuid", java.util.UUID.fromString(fhirUuid));
            query.setMaxResults(1);
            List<InventoryLot> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting lot by FHIR UUID", e);
        }
    }
}
