package org.openelisglobal.storage.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class StorageRoomDAOImpl extends BaseDAOImpl<StorageRoom, Integer> implements StorageRoomDAO {

    public StorageRoomDAOImpl() {
        super(StorageRoom.class);
    }

    @Override
    @Transactional(readOnly = true)
    public StorageRoom findByCode(String code) {
        try {
            String hql = "FROM StorageRoom WHERE code = :code";
            Query<StorageRoom> query = entityManager.unwrap(Session.class).createQuery(hql, StorageRoom.class);
            query.setParameter("code", code);
            query.setMaxResults(1); // Ensure only one result is returned
            List<StorageRoom> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageRoom by code", e);
        }
    }
}
