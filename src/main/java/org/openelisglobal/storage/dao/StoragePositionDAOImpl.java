package org.openelisglobal.storage.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.valueholder.StoragePosition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class StoragePositionDAOImpl extends BaseDAOImpl<StoragePosition, String> implements StoragePositionDAO {

    public StoragePositionDAOImpl() {
        super(StoragePosition.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoragePosition> findByParentRackId(String rackId) {
        try {
            String hql = "FROM StoragePosition p WHERE p.parentRack.id = :rackId";
            Query<StoragePosition> query = entityManager.unwrap(Session.class).createQuery(hql, StoragePosition.class);
            query.setParameter("rackId", Integer.parseInt(rackId));
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StoragePositions by rack ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countOccupied(String rackId) {
        try {
            String hql = "SELECT COUNT(*) FROM StoragePosition p WHERE p.parentRack.id = :rackId AND p.occupied = true";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("rackId", Integer.parseInt(rackId));
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting occupied positions in rack", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countOccupiedInDevice(String deviceId) {
        try {
            String hql = "SELECT COUNT(*) FROM StoragePosition p "
                    + "WHERE p.parentRack.parentShelf.parentDevice.id = :deviceId AND p.occupied = true";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("deviceId", Integer.parseInt(deviceId));
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting occupied positions in device", e);
        }
    }
}
