package org.openelisglobal.inventory.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.dao.InventoryStorageLocationDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LocationType;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryStorageLocationDAOImpl extends BaseDAOImpl<InventoryStorageLocation, Long>
        implements InventoryStorageLocationDAO {

    public InventoryStorageLocationDAOImpl() {
        super(InventoryStorageLocation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryStorageLocation> getAllActive() throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryStorageLocation s WHERE s.isActive = true ORDER BY s.name";
            Query<InventoryStorageLocation> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryStorageLocation.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting all active storage locations", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryStorageLocation> getByLocationType(LocationType locationType) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryStorageLocation s WHERE s.locationType = :locationType AND s.isActive = true ORDER BY s.name";
            Query<InventoryStorageLocation> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryStorageLocation.class);
            query.setParameter("locationType", locationType);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting storage locations by type", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryStorageLocation> getChildLocations(Long parentLocationId) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryStorageLocation s WHERE s.parentLocation.id = :parentId AND s.isActive = true ORDER BY s.name";
            Query<InventoryStorageLocation> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryStorageLocation.class);
            query.setParameter("parentId", parentLocationId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting child storage locations", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryStorageLocation> getTopLevelLocations() throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryStorageLocation s WHERE s.parentLocation IS NULL AND s.isActive = true ORDER BY s.name";
            Query<InventoryStorageLocation> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryStorageLocation.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting top-level storage locations", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryStorageLocation getByLocationCode(String locationCode) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryStorageLocation s WHERE s.locationCode = :locationCode";
            Query<InventoryStorageLocation> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryStorageLocation.class);
            query.setParameter("locationCode", locationCode);
            query.setMaxResults(1);
            List<InventoryStorageLocation> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting storage location by code", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryStorageLocation getByFhirUuid(String fhirUuid) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryStorageLocation s WHERE s.fhirUuid = :fhirUuid";
            Query<InventoryStorageLocation> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryStorageLocation.class);
            query.setParameter("fhirUuid", java.util.UUID.fromString(fhirUuid));
            query.setMaxResults(1);
            List<InventoryStorageLocation> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting storage location by FHIR UUID", e);
        }
    }
}
