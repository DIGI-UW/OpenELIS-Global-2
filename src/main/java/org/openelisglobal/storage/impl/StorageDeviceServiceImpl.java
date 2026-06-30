package org.openelisglobal.storage.impl;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.service.StorageDeviceService;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StorageDeviceServiceImpl extends BaseObjectServiceImpl<StorageDevice, Integer>
        implements StorageDeviceService {
    @Autowired
    private StorageDeviceDAO storageDeviceDao;

    public StorageDeviceServiceImpl() {
        super(StorageDevice.class);
    }

    @Override
    protected StorageDeviceDAO getBaseObjectDAO() {
        return storageDeviceDao;

    }

    @Override
    @Transactional
    public List<StorageDevice> findByParentRoomId(Integer roomId) {
        return storageDeviceDao.findByParentRoomId(roomId);
    }

    @Override
    @Transactional
    public StorageDevice findByParentRoomIdAndCode(Integer roomId, String code) {
        return storageDeviceDao.findByParentRoomIdAndCode(roomId, code);
    }

    @Override
    @Transactional
    public StorageDevice findByCode(String code) {
        return storageDeviceDao.findByCode(code);
    }

    @Override
    @Transactional
    public StorageDevice findByCodeAndParentRoom(String code, StorageRoom parentRoom) {
        return storageDeviceDao.findByCodeAndParentRoom(code, parentRoom);
    }

    @Override
    @Transactional
    public int countByRoomId(Integer roomId) {
        return storageDeviceDao.countByRoomId(roomId);
    }

    @Override
    @Transactional
    public StorageDevice findByNameAndParentRoomId(String name, Integer parentRoomId) {
        return storageDeviceDao.findByNameAndParentRoomId(name, parentRoomId);
    }

    @Override
    @Transactional
    public Optional<StorageDevice> getDevice(int id) {
        return storageDeviceDao.get(id);
    }

}
