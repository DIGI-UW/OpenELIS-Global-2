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
public class StorageRackDAOImpl extends BaseDAOImpl<StorageRack, String> implements StorageRackDAO {
    
    public StorageRackDAOImpl() {
        super(StorageRack.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageRack> findByParentShelfId(String shelfId) {
        try {
            String hql = "FROM StorageRack WHERE parentShelf.id = :shelfId";
            Query<StorageRack> query = entityManager.unwrap(Session.class).createQuery(hql, StorageRack.class);
            query.setParameter("shelfId", shelfId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageRacks by shelf ID", e);
        }
    }
}

