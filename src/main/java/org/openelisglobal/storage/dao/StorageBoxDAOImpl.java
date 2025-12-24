package org.openelisglobal.storage.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class StorageBoxDAOImpl extends BaseDAOImpl<StorageBox, Integer> implements StorageBoxDAO {

    public StorageBoxDAOImpl() {
        super(StorageBox.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageBox> findByParentRackId(Integer rackId) {
        try {
            String hql = "FROM StorageBox b WHERE b.parentRack.id = :rackId";
            Query<StorageBox> query = entityManager.unwrap(Session.class).createQuery(hql, StorageBox.class);
            query.setParameter("rackId", rackId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageBoxes by rack ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageBox findByCoordinates(String coordinates) {
        try {
            // Search by code (the identifier used in barcodes)
            String hql = "FROM StorageBox b WHERE b.code = :coordinates";
            Query<StorageBox> query = entityManager.unwrap(Session.class).createQuery(hql, StorageBox.class);
            query.setParameter("coordinates", coordinates);
            List<StorageBox> results = query.list();
            return results != null && !results.isEmpty() ? results.get(0) : null;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageBox by coordinates", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageBox findByCoordinatesAndParentRack(String coordinates, StorageRack parentRack) {
        try {
            // Search by code (the identifier used in barcodes)
            String hql = "FROM StorageBox b WHERE b.code = :coordinates AND b.parentRack.id = :rackId";
            Query<StorageBox> query = entityManager.unwrap(Session.class).createQuery(hql, StorageBox.class);
            query.setParameter("coordinates", coordinates);
            query.setParameter("rackId", parentRack.getId());
            List<StorageBox> results = query.list();
            return results != null && !results.isEmpty() ? results.get(0) : null;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageBox by coordinates and parent rack", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countOccupied(Integer rackId) {
        try {
            String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                    + "WHERE ssa.locationType = 'box' AND ssa.locationId = :rackId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("rackId", rackId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting occupied boxes in rack", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countOccupiedInShelf(Integer shelfId) {
        try {
            String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                    + "WHERE ssa.locationType = 'box' AND ssa.locationId IN "
                    + "(SELECT b.id FROM StorageBox b WHERE b.parentRack.parentShelf.id = :shelfId)";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("shelfId", shelfId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting occupied boxes in shelf", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countOccupiedInDevice(Integer deviceId) {
        try {
            String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                    + "WHERE ssa.locationType = 'box' AND ssa.locationId IN "
                    + "(SELECT b.id FROM StorageBox b WHERE b.parentRack.parentShelf.parentDevice.id = :deviceId)";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("deviceId", deviceId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting occupied boxes in device", e);
        }
    }
}
