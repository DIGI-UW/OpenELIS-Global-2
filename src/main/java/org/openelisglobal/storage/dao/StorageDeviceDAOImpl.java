package org.openelisglobal.storage.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class StorageDeviceDAOImpl extends BaseDAOImpl<StorageDevice, Integer> implements StorageDeviceDAO {

    private static final Logger logger = LoggerFactory.getLogger(StorageDeviceDAOImpl.class);

    public StorageDeviceDAOImpl() {
        super(StorageDevice.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageDevice> getAll() {
        try {
            String hql = "FROM StorageDevice d ORDER BY d.id";
            Query<StorageDevice> query = entityManager.unwrap(Session.class).createQuery(hql, StorageDevice.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error getting all StorageDevices", e);
            throw new LIMSRuntimeException("Error getting all StorageDevices", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageDevice> findByParentRoomId(Integer roomId) {
        try {
            String hql = "FROM StorageDevice d WHERE d.parentRoom.id = :roomId";
            Query<StorageDevice> query = entityManager.unwrap(Session.class).createQuery(hql, StorageDevice.class);
            query.setParameter("roomId", roomId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding StorageDevices by room ID", e);
            throw new LIMSRuntimeException("Error finding StorageDevices by room ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageDevice findByParentRoomIdAndCode(Integer roomId, String code) {
        try {
            String hql = "FROM StorageDevice d WHERE d.parentRoom.id = :roomId AND d.code = :code";
            Query<StorageDevice> query = entityManager.unwrap(Session.class).createQuery(hql, StorageDevice.class);
            query.setParameter("roomId", roomId);
            query.setParameter("code", code);
            query.setMaxResults(1);
            List<StorageDevice> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding StorageDevice by room ID and code", e);
            throw new LIMSRuntimeException("Error finding StorageDevice by room ID and code", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countByRoomId(Integer roomId) {
        try {
            String hql = "SELECT COUNT(*) FROM StorageDevice d WHERE d.parentRoom.id = :roomId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("roomId", roomId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            logger.error("Error counting StorageDevices by room ID", e);
            throw new LIMSRuntimeException("Error counting StorageDevices by room ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageDevice findByCode(String code) {
        try {
            String hql = "FROM StorageDevice d WHERE d.code = :code";
            Query<StorageDevice> query = entityManager.unwrap(Session.class).createQuery(hql, StorageDevice.class);
            query.setParameter("code", code);
            query.setMaxResults(1);
            List<StorageDevice> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding StorageDevice by code", e);
            throw new LIMSRuntimeException("Error finding StorageDevice by code", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageDevice findByCodeAndParentRoom(String code,
            org.openelisglobal.storage.valueholder.StorageRoom parentRoom) {
        try {
            if (parentRoom == null) {
                return null;
            }
            String hql = "FROM StorageDevice d WHERE d.code = :code AND d.parentRoom.id = :roomId";
            Query<StorageDevice> query = entityManager.unwrap(Session.class).createQuery(hql, StorageDevice.class);
            query.setParameter("code", code);
            query.setParameter("roomId", parentRoom.getId());
            query.setMaxResults(1);
            List<StorageDevice> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding StorageDevice by code and parent room", e);
            throw new LIMSRuntimeException("Error finding StorageDevice by code and parent room", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StorageDevice findByShortCode(String shortCode) {
        try {
            String hql = "FROM StorageDevice d WHERE d.shortCode = :shortCode";
            Query<StorageDevice> query = entityManager.unwrap(Session.class).createQuery(hql, StorageDevice.class);
            query.setParameter("shortCode", shortCode);
            query.setMaxResults(1);
            List<StorageDevice> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding StorageDevice by short code", e);
            throw new LIMSRuntimeException("Error finding StorageDevice by short code", e);
        }
    }
}
