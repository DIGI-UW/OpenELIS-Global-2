package org.openelisglobal.storage.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SampleStorageAssignmentDAOImpl extends BaseDAOImpl<SampleStorageAssignment, Integer>
        implements SampleStorageAssignmentDAO {

    private static final Logger logger = LoggerFactory.getLogger(SampleStorageAssignmentDAOImpl.class);

    public SampleStorageAssignmentDAOImpl() {
        super(SampleStorageAssignment.class);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleStorageAssignment findBySampleItemId(String sampleItemId) {
        try {
            // Note: SampleItem.id uses LIMSStringNumberUserType (String in Java, numeric in
            // DB)
            // When querying through relationships, we must parse String to Integer for the
            // parameter
            // This matches the pattern in SampleItemDAOImpl.getSampleItemsBySampleId()
            String hql = "FROM SampleStorageAssignment ssa WHERE ssa.sampleItem.id = :sampleItemId";
            Query<SampleStorageAssignment> query = entityManager.unwrap(Session.class).createQuery(hql,
                    SampleStorageAssignment.class);
            query.setParameter("sampleItemId", Integer.parseInt(sampleItemId));
            query.setMaxResults(1);
            List<SampleStorageAssignment> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (NumberFormatException e) {
            logger.error("Invalid SampleItem ID format (must be numeric): " + sampleItemId, e);
            return null;
        } catch (Exception e) {
            logger.error("Error finding SampleStorageAssignment by SampleItem ID: " + sampleItemId, e);
            throw new LIMSRuntimeException("Error finding SampleStorageAssignment by SampleItem ID: " + sampleItemId,
                    e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SampleStorageAssignment findByStoragePosition(
            org.openelisglobal.storage.valueholder.StoragePosition position) {
        try {
            if (position == null) {
                return null;
            }
            // Note: This method is deprecated - assignments now use location_id +
            // location_type
            // instead of StoragePosition references. This method is kept for backward
            // compatibility
            // but may not work correctly with the new flexible assignment model.
            // TODO: Consider removing this method or updating it to work with location_id +
            // location_type
            String hql = "FROM SampleStorageAssignment ssa WHERE ssa.locationType = 'rack' AND ssa.locationId = :rackId";
            if (position.getParentRack() != null) {
                Query<SampleStorageAssignment> query = entityManager.unwrap(Session.class).createQuery(hql,
                        SampleStorageAssignment.class);
                query.setParameter("rackId", position.getParentRack().getId());
                query.setMaxResults(1);
                List<SampleStorageAssignment> results = query.list();
                return results.isEmpty() ? null : results.get(0);
            }
            return null;
        } catch (Exception e) {
            logger.error("Error finding SampleStorageAssignment by storage position", e);
            throw new LIMSRuntimeException("Error finding SampleStorageAssignment by storage position", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPositionOccupied(org.openelisglobal.storage.valueholder.StoragePosition position) {
        try {
            if (position == null) {
                return false;
            }

            // If position has a coordinate and parent rack, check for assignment with
            // matching coordinate
            if (position.getCoordinate() != null && !position.getCoordinate().isEmpty()
                    && position.getParentRack() != null) {
                // Check for assignment to this rack with this coordinate
                String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                        + "WHERE ssa.locationType = 'rack' AND ssa.locationId = :rackId "
                        + "AND ssa.positionCoordinate = :coordinate";
                Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
                query.setParameter("rackId", position.getParentRack().getId());
                query.setParameter("coordinate", position.getCoordinate());
                Long count = query.uniqueResult();
                return count != null && count > 0;
            } else if (position.getParentRack() != null) {
                // Position without coordinate but with rack - check for any assignment to this
                // rack
                String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                        + "WHERE ssa.locationType = 'rack' AND ssa.locationId = :rackId";
                Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
                query.setParameter("rackId", position.getParentRack().getId());
                Long count = query.uniqueResult();
                return count != null && count > 0;
            } else if (position.getParentShelf() != null) {
                // Position at shelf level - check for assignment to this shelf
                String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                        + "WHERE ssa.locationType = 'shelf' AND ssa.locationId = :shelfId";
                Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
                query.setParameter("shelfId", position.getParentShelf().getId());
                Long count = query.uniqueResult();
                return count != null && count > 0;
            } else if (position.getParentDevice() != null) {
                // Position at device level - check for assignment to this device
                String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                        + "WHERE ssa.locationType = 'device' AND ssa.locationId = :deviceId";
                Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
                query.setParameter("deviceId", position.getParentDevice().getId());
                Long count = query.uniqueResult();
                return count != null && count > 0;
            }

            return false;
        } catch (Exception e) {
            logger.error("Error checking position occupancy: " + e.getMessage(), e);
            // On error, return false (position appears unoccupied)
            return false;
        }
    }

    // No override needed - BaseDAOImpl.getAll() uses entity fetch strategies
    // All relationships are EAGER at entity level, so they load automatically
}
