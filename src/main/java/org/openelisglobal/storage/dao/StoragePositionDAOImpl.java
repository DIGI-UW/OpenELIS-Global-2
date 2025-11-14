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
            // Updated to handle nullable rack - positions can have rack or not
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
            // Count SampleStorageAssignment records where locationType='rack' and
            // locationId matches rackId
            // This reflects actual sample assignments (source of truth) instead of
            // StoragePosition.occupied flag
            String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                    + "WHERE ssa.locationType = 'rack' AND ssa.locationId = :rackId";
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
            // Count SampleStorageAssignment records that match device hierarchy:
            // - locationType='device' AND locationId=deviceId OR
            // - locationType='shelf' AND locationId IN (shelves in device) OR
            // - locationType='rack' AND locationId IN (racks in shelves in device)
            // This reflects actual sample assignments (source of truth) instead of
            // StoragePosition.occupied flag
            String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                    + "WHERE (ssa.locationType = 'device' AND ssa.locationId = :deviceId) "
                    + "OR (ssa.locationType = 'shelf' AND ssa.locationId IN "
                    + "(SELECT s.id FROM StorageShelf s WHERE s.parentDevice.id = :deviceId)) "
                    + "OR (ssa.locationType = 'rack' AND ssa.locationId IN "
                    + "(SELECT r.id FROM StorageRack r WHERE r.parentShelf.parentDevice.id = :deviceId))";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("deviceId", deviceId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            // If query fails, return 0 (data will show but occupancy will be 0)
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countOccupiedInShelf(Integer shelfId) {
        try {
            // Count SampleStorageAssignment records that match shelf hierarchy:
            // - locationType='shelf' AND locationId=shelfId OR
            // - locationType='rack' AND locationId IN (racks in shelf)
            // This reflects actual sample assignments (source of truth) instead of
            // StoragePosition.occupied flag
            String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                    + "WHERE (ssa.locationType = 'shelf' AND ssa.locationId = :shelfId) "
                    + "OR (ssa.locationType = 'rack' AND ssa.locationId IN "
                    + "(SELECT r.id FROM StorageRack r WHERE r.parentShelf.id = :shelfId))";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("shelfId", shelfId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            // If query fails, return 0 (data will show but occupancy will be 0)
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoragePosition> findByParentDeviceId(Integer deviceId) {
        try {
            String hql = "FROM StoragePosition p WHERE p.parentDevice.id = :deviceId";
            Query<StoragePosition> query = entityManager.unwrap(Session.class).createQuery(hql, StoragePosition.class);
            query.setParameter("deviceId", deviceId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StoragePositions by device ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoragePosition> findByParentShelfId(Integer shelfId) {
        try {
            String hql = "FROM StoragePosition p WHERE p.parentShelf.id = :shelfId";
            Query<StoragePosition> query = entityManager.unwrap(Session.class).createQuery(hql, StoragePosition.class);
            query.setParameter("shelfId", shelfId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StoragePositions by shelf ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoragePosition> findPositionsByHierarchyLevel(int level) {
        try {
            String hql;
            switch (level) {
            case 2:
                // Device level: has parent_device, no parent_shelf, no parent_rack
                hql = "FROM StoragePosition p WHERE p.parentDevice.id IS NOT NULL "
                        + "AND p.parentShelf.id IS NULL AND p.parentRack.id IS NULL";
                break;
            case 3:
                // Shelf level: has parent_device and parent_shelf, no parent_rack
                hql = "FROM StoragePosition p WHERE p.parentDevice.id IS NOT NULL "
                        + "AND p.parentShelf.id IS NOT NULL AND p.parentRack.id IS NULL";
                break;
            case 4:
                // Rack level: has parent_device, parent_shelf, and parent_rack, no coordinate
                hql = "FROM StoragePosition p WHERE p.parentDevice.id IS NOT NULL "
                        + "AND p.parentShelf.id IS NOT NULL AND p.parentRack.id IS NOT NULL "
                        + "AND (p.coordinate IS NULL OR p.coordinate = '')";
                break;
            case 5:
                // Position level: has full hierarchy with coordinate
                hql = "FROM StoragePosition p WHERE p.parentDevice.id IS NOT NULL "
                        + "AND p.parentShelf.id IS NOT NULL AND p.parentRack.id IS NOT NULL "
                        + "AND p.coordinate IS NOT NULL AND p.coordinate != ''";
                break;
            default:
                throw new IllegalArgumentException("Invalid hierarchy level: " + level + ". Must be 2-5.");
            }
            Query<StoragePosition> query = entityManager.unwrap(Session.class).createQuery(hql, StoragePosition.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StoragePositions by hierarchy level", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateHierarchyIntegrity(Integer positionId) {
        try {
            StoragePosition position = get(positionId).orElse(null);
            if (position == null) {
                return false;
            }
            return position.validateHierarchyIntegrity();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error validating hierarchy integrity for position", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StoragePosition findByCoordinates(String coordinates) {
        try {
            String hql = "FROM StoragePosition p WHERE p.coordinate = :coordinates";
            Query<StoragePosition> query = entityManager.unwrap(Session.class).createQuery(hql, StoragePosition.class);
            query.setParameter("coordinates", coordinates);
            query.setMaxResults(1);
            List<StoragePosition> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StoragePosition by coordinates", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StoragePosition findByCoordinatesAndParentRack(String coordinates,
            org.openelisglobal.storage.valueholder.StorageRack parentRack) {
        try {
            if (parentRack == null) {
                return null;
            }
            String hql = "FROM StoragePosition p WHERE p.coordinate = :coordinates AND p.parentRack.id = :rackId";
            Query<StoragePosition> query = entityManager.unwrap(Session.class).createQuery(hql, StoragePosition.class);
            query.setParameter("coordinates", coordinates);
            query.setParameter("rackId", parentRack.getId());
            query.setMaxResults(1);
            List<StoragePosition> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StoragePosition by coordinates and parent rack", e);
        }
    }
}
