package org.openelisglobal.storage.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SampleStorageAssignmentDAOImpl extends BaseDAOImpl<SampleStorageAssignment, Integer>
        implements SampleStorageAssignmentDAO {

    private static final Logger logger = LoggerFactory.getLogger(SampleStorageAssignmentDAOImpl.class);

    public SampleStorageAssignmentDAOImpl() {
        super(SampleStorageAssignment.class);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleStorageAssignment findBySampleItemId(String sampleItemId) {
        if (sampleItemId == null || sampleItemId.trim().isEmpty()) {
            return null;
        }

        // Parse String to Integer since DB column is numeric
        Integer sampleItemIdInt;
        try {
            sampleItemIdInt = Integer.parseInt(sampleItemId.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid SampleItem ID format (must be numeric): {}", sampleItemId);
            return null;
        }

        try {
            // Query directly using sampleItemId column (no join to HBM-mapped entity)
            String hql = "FROM SampleStorageAssignment ssa WHERE ssa.sampleItemId = :sampleItemId";
            Query<SampleStorageAssignment> query = entityManager.unwrap(Session.class).createQuery(hql,
                    SampleStorageAssignment.class);
            query.setParameter("sampleItemId", sampleItemIdInt);
            query.setMaxResults(1);

            List<SampleStorageAssignment> results = query.list();
            return results.isEmpty() ? null : results.getFirst();
        } catch (Exception e) {
            logger.error("Error finding SampleStorageAssignment by SampleItem ID: {}", sampleItemId, e);
            throw new LIMSRuntimeException("Error finding SampleStorageAssignment by SampleItem ID: " + sampleItemId,
                    e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SampleStorageAssignment findByStorageBox(StorageBox box) {
        try {
            if (box == null) {
                return null;
            }
            String hql = "FROM SampleStorageAssignment ssa WHERE ssa.locationType = 'box' AND ssa.locationId = :boxId";
            Query<SampleStorageAssignment> query = entityManager.unwrap(Session.class).createQuery(hql,
                    SampleStorageAssignment.class);
            query.setParameter("boxId", box.getId());
            query.setMaxResults(1);
            List<SampleStorageAssignment> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding SampleStorageAssignment by storage box", e);
            throw new LIMSRuntimeException("Error finding SampleStorageAssignment by storage box", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBoxOccupied(StorageBox box) {
        try {
            if (box == null) {
                return false;
            }

            String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                    + "WHERE ssa.locationType = 'box' AND ssa.locationId = :boxId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("boxId", box.getId());
            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Error checking box occupancy: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SampleStorageAssignment findByBoxAndCoordinate(Integer boxId, String coordinate) {
        try {
            if (boxId == null || coordinate == null || coordinate.trim().isEmpty()) {
                return null;
            }

            String hql = "FROM SampleStorageAssignment ssa " + "WHERE ssa.locationType = 'box' "
                    + "AND ssa.locationId = :boxId " + "AND ssa.positionCoordinate = :coordinate";
            Query<SampleStorageAssignment> query = entityManager.unwrap(Session.class).createQuery(hql,
                    SampleStorageAssignment.class);
            query.setParameter("boxId", boxId);
            query.setParameter("coordinate", coordinate.trim());
            query.setMaxResults(1);
            List<SampleStorageAssignment> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding assignment by box and coordinate: " + e.getMessage(), e);
            throw new LIMSRuntimeException("Error finding assignment by box and coordinate", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<String> getOccupiedCoordinatesByBoxId(Integer boxId) {
        try {
            if (boxId == null) {
                return new java.util.ArrayList<>();
            }

            String hql = "SELECT ssa.positionCoordinate FROM SampleStorageAssignment ssa "
                    + "WHERE ssa.locationType = 'box' " + "AND ssa.locationId = :boxId "
                    + "AND ssa.positionCoordinate IS NOT NULL";
            Query<String> query = entityManager.unwrap(Session.class).createQuery(hql, String.class);
            query.setParameter("boxId", boxId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error getting occupied coordinates for box: " + e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, java.util.Map<String, String>> getOccupiedCoordinatesWithSampleInfo(Integer boxId) {
        java.util.Map<String, java.util.Map<String, String>> result = new java.util.HashMap<>();
        try {
            if (boxId == null) {
                return result;
            }

            // Use native SQL to join sample_storage_assignment with sample_item
            // to get the external_id (SampleItem uses HBM mapping, can't use HQL join)
            String sql = "SELECT ssa.position_coordinate, ssa.sample_item_id, si.external_id "
                    + "FROM sample_storage_assignment ssa " + "LEFT JOIN sample_item si ON ssa.sample_item_id = si.id "
                    + "WHERE ssa.location_type = 'box' " + "AND ssa.location_id = :boxId "
                    + "AND ssa.position_coordinate IS NOT NULL";

            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager.unwrap(Session.class).createNativeQuery(sql)
                    .setParameter("boxId", boxId).list();

            for (Object[] row : rows) {
                String positionCoordinate = (String) row[0];
                Number sampleItemIdNum = (Number) row[1];
                String externalId = (String) row[2];

                if (positionCoordinate != null && sampleItemIdNum != null) {
                    java.util.Map<String, String> sampleInfo = new java.util.HashMap<>();
                    sampleInfo.put("sampleItemId", sampleItemIdNum.toString());
                    sampleInfo.put("externalId", externalId != null ? externalId : "");
                    result.put(positionCoordinate, sampleInfo);
                }
            }
        } catch (Exception e) {
            logger.error("Error getting occupied coordinates with sample info: " + e.getMessage(), e);
        }
        return result;
    }

    // No override needed - BaseDAOImpl.getAll() uses entity fetch strategies
    // All relationships are EAGER at entity level, so they load automatically

    @Override
    @Transactional(readOnly = true)
    public int countByLocationTypeAndId(String locationType, Integer locationId) {
        try {
            if (locationType == null || locationId == null) {
                return 0;
            }

            String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                    + "WHERE ssa.locationType = :locationType AND ssa.locationId = :locationId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("locationType", locationType);
            query.setParameter("locationId", locationId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            logger.error("Error counting sample storage assignments by location: " + e.getMessage(), e);
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SampleStorageAssignment> findAll(Pageable pageable) {
        try {
            Session session = entityManager.unwrap(Session.class);

            // Count query for total elements
            String countHql = "SELECT COUNT(ssa) FROM SampleStorageAssignment ssa";
            Long total = session.createQuery(countHql, Long.class).uniqueResult();
            if (total == null) {
                total = 0L;
            }

            // Data query with pagination and sorting
            String dataHql = "SELECT ssa FROM SampleStorageAssignment ssa ORDER BY ssa.assignedDate DESC";
            Query<SampleStorageAssignment> query = session.createQuery(dataHql, SampleStorageAssignment.class);
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());

            List<SampleStorageAssignment> content = query.list();

            return new PageImpl<>(content, pageable, total);
        } catch (Exception e) {
            logger.error("Error finding paginated sample storage assignments: " + e.getMessage(), e);
            throw new LIMSRuntimeException("Error finding paginated sample storage assignments", e);
        }
    }
}
