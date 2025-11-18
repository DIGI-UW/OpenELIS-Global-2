package org.openelisglobal.storage.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class StorageRackDAOImpl extends BaseDAOImpl<StorageRack, Integer> implements StorageRackDAO {

    public StorageRackDAOImpl() {
        super(StorageRack.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageRack> getAll() {
        try {
            String hql = "FROM StorageRack r ORDER BY r.id";
            Query<StorageRack> query = entityManager.unwrap(Session.class).createQuery(hql, StorageRack.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting all StorageRacks", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageRack> findByParentShelfId(Integer shelfId) {
        try {
            String hql = "FROM StorageRack r WHERE r.parentShelf.id = :shelfId";
            Query<StorageRack> query = entityManager.unwrap(Session.class).createQuery(hql, StorageRack.class);
            query.setParameter("shelfId", shelfId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageRacks by shelf ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countByShelfId(Integer shelfId) {
        try {
            String hql = "SELECT COUNT(*) FROM StorageRack r WHERE r.parentShelf.id = :shelfId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("shelfId", shelfId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting StorageRacks by shelf ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageRack findByLabel(String label) {
        try {
            String hql = "FROM StorageRack r WHERE r.label = :label";
            Query<StorageRack> query = entityManager.unwrap(Session.class).createQuery(hql, StorageRack.class);
            query.setParameter("label", label);
            query.setMaxResults(1);
            List<StorageRack> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageRack by label", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageRack findByLabelAndParentShelf(String label,
            org.openelisglobal.storage.valueholder.StorageShelf parentShelf) {
        try {
            if (parentShelf == null) {
                return null;
            }
            String hql = "FROM StorageRack r WHERE r.label = :label AND r.parentShelf.id = :shelfId";
            Query<StorageRack> query = entityManager.unwrap(Session.class).createQuery(hql, StorageRack.class);
            query.setParameter("label", label);
            query.setParameter("shelfId", parentShelf.getId());
            query.setMaxResults(1);
            List<StorageRack> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageRack by label and parent shelf", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageRack findByShortCode(String shortCode) {
        try {
            String hql = "FROM StorageRack r WHERE r.shortCode = :shortCode";
            Query<StorageRack> query = entityManager.unwrap(Session.class).createQuery(hql, StorageRack.class);
            query.setParameter("shortCode", shortCode);
            query.setMaxResults(1);
            List<StorageRack> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageRack by short code", e);
        }
    }
}
