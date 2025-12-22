package org.openelisglobal.storage.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class StorageShelfDAOImpl extends BaseDAOImpl<StorageShelf, Integer> implements StorageShelfDAO {

    public StorageShelfDAOImpl() {
        super(StorageShelf.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageShelf> getAll() {
        try {
            String hql = "FROM StorageShelf s ORDER BY s.id";
            Query<StorageShelf> query = entityManager.unwrap(Session.class).createQuery(hql, StorageShelf.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting all StorageShelves", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageShelf> findByParentDeviceId(Integer deviceId) {
        try {
            String hql = "FROM StorageShelf s WHERE s.parentDevice.id = :deviceId";
            Query<StorageShelf> query = entityManager.unwrap(Session.class).createQuery(hql, StorageShelf.class);
            query.setParameter("deviceId", deviceId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageShelves by device ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countByDeviceId(Integer deviceId) {
        try {
            String hql = "SELECT COUNT(*) FROM StorageShelf s WHERE s.parentDevice.id = :deviceId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("deviceId", deviceId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting StorageShelves by device ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageShelf findByLabel(String label) {
        try {
            String hql = "FROM StorageShelf s WHERE s.label = :label";
            Query<StorageShelf> query = entityManager.unwrap(Session.class).createQuery(hql, StorageShelf.class);
            query.setParameter("label", label);
            query.setMaxResults(1);
            List<StorageShelf> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageShelf by label", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageShelf findByLabelAndParentDevice(String label,
            org.openelisglobal.storage.valueholder.StorageDevice parentDevice) {
        try {
            if (parentDevice == null) {
                return null;
            }
            String hql = "FROM StorageShelf s WHERE s.label = :label AND s.parentDevice.id = :deviceId";
            Query<StorageShelf> query = entityManager.unwrap(Session.class).createQuery(hql, StorageShelf.class);
            query.setParameter("label", label);
            query.setParameter("deviceId", parentDevice.getId());
            query.setMaxResults(1);
            List<StorageShelf> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageShelf by label and parent device", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageShelf findByLabelAndParentDeviceId(String label, Integer parentDeviceId) {
        try {
            String hql = "FROM StorageShelf s WHERE s.label = :label AND s.parentDevice.id = :deviceId";
            Query<StorageShelf> query = entityManager.unwrap(Session.class).createQuery(hql, StorageShelf.class);
            query.setParameter("label", label);
            query.setParameter("deviceId", parentDeviceId);
            query.setMaxResults(1);
            List<StorageShelf> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageShelf by label and parent device ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageShelf findByCode(String code) {
        try {
            if (code == null || code.trim().isEmpty()) {
                return null;
            }
            String hql = "FROM StorageShelf s WHERE s.code = :code";
            Query<StorageShelf> query = entityManager.unwrap(Session.class).createQuery(hql, StorageShelf.class);
            query.setParameter("code", code.trim());
            query.setMaxResults(1);
            List<StorageShelf> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageShelf by code", e);
        }
    }

}
