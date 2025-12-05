package org.openelisglobal.inventory.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.StorageLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class StorageLocationDAOImpl extends BaseDAOImpl<StorageLocation, String> implements StorageLocationDAO {

    private static final Logger logger = LoggerFactory.getLogger(StorageLocationDAOImpl.class);

    public StorageLocationDAOImpl() {
        super(StorageLocation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageLocation> findAllActive() {
        try {
            String hql = "FROM StorageLocation l WHERE l.isActive = true ORDER BY l.name";
            Query<StorageLocation> query = entityManager.unwrap(Session.class).createQuery(hql, StorageLocation.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding active storage locations", e);
            throw new LIMSRuntimeException("Error finding active storage locations", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageLocation findByLocationCode(String locationCode) {
        try {
            String hql = "FROM StorageLocation l WHERE l.locationCode = :code";
            Query<StorageLocation> query = entityManager.unwrap(Session.class).createQuery(hql, StorageLocation.class);
            query.setParameter("code", locationCode);
            query.setMaxResults(1);
            List<StorageLocation> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding storage location by code", e);
            throw new LIMSRuntimeException("Error finding storage location by code", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageLocation> findByParentLocationId(String parentLocationId) {
        try {
            String hql = "FROM StorageLocation l WHERE l.parentLocation.id = :parentId ORDER BY l.name";
            Query<StorageLocation> query = entityManager.unwrap(Session.class).createQuery(hql, StorageLocation.class);
            query.setParameter("parentId", parentLocationId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding storage locations by parent ID", e);
            throw new LIMSRuntimeException("Error finding storage locations by parent ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageLocation> findRootLocations() {
        try {
            String hql = "FROM StorageLocation l WHERE l.parentLocation IS NULL ORDER BY l.name";
            Query<StorageLocation> query = entityManager.unwrap(Session.class).createQuery(hql, StorageLocation.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding root storage locations", e);
            throw new LIMSRuntimeException("Error finding root storage locations", e);
        }
    }
}
