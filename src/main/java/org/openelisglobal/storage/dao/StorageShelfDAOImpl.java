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
}
