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
public class StoragePositionDAOImpl extends BaseDAOImpl<StoragePosition, Integer> implements StoragePositionDAO {

    public StoragePositionDAOImpl() {
        super(StoragePosition.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoragePosition> findByParentRackId(Integer rackId) {
        try {
            String hql = "FROM StoragePosition p WHERE p.parentRack.id = :rackId";
            Query<StoragePosition> query = entityManager.unwrap(Session.class).createQuery(hql, StoragePosition.class);
            query.setParameter("rackId", rackId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StoragePositions by rack ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countOccupied(Integer rackId) {
        try {
            String hql = "SELECT COUNT(*) FROM StoragePosition p WHERE p.parentRack.id = :rackId AND p.occupied = true";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("rackId", rackId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting occupied positions in rack", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countOccupiedInDevice(Integer deviceId) {
        try {
            // Use explicit joins to avoid deep navigation
            String hql = "SELECT COUNT(*) FROM StoragePosition p " + "JOIN p.parentRack r " + "JOIN r.parentShelf s "
                    + "JOIN s.parentDevice d " + "WHERE d.id = :deviceId AND p.occupied = true";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("deviceId", deviceId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            // If query fails, return 0 (data will show but occupancy will be 0)
            return 0;
        }
    }
}
