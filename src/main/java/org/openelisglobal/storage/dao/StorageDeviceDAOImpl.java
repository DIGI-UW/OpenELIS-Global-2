package org.openelisglobal.storage.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class StorageDeviceDAOImpl extends BaseDAOImpl<StorageDevice, String> implements StorageDeviceDAO {
    
    public StorageDeviceDAOImpl() {
        super(StorageDevice.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageDevice> findByParentRoomId(String roomId) {
        try {
            String hql = "FROM StorageDevice WHERE parentRoom.id = :roomId";
            Query<StorageDevice> query = entityManager.unwrap(Session.class).createQuery(hql, StorageDevice.class);
            query.setParameter("roomId", roomId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageDevices by room ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageDevice findByParentRoomIdAndCode(String roomId, String code) {
        try {
            String hql = "FROM StorageDevice WHERE parentRoom.id = :roomId AND code = :code";
            Query<StorageDevice> query = entityManager.unwrap(Session.class).createQuery(hql, StorageDevice.class);
            query.setParameter("roomId", roomId);
            query.setParameter("code", code);
            List<StorageDevice> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding StorageDevice by room ID and code", e);
        }
    }
}

