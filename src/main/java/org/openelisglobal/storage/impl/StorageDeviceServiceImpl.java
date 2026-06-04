package org.openelisglobal.storage.impl;

import java.util.List;
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
    public List<StorageDevice> findByParentRoomId(Integer roomId) {
        return storageDeviceDao.findByParentRoomId(roomId);
    }

    @Override
    public StorageDevice findByParentRoomIdAndCode(Integer roomId, String code) {
        return storageDeviceDao.findByParentRoomIdAndCode(roomId, code);
    }

    @Override
    public StorageDevice findByCode(String code) {
        return findByCode(code);
    }

    @Override
    public StorageDevice findByCodeAndParentRoom(String code, StorageRoom parentRoom) {
        return storageDeviceDao.findByCodeAndParentRoom(code, parentRoom);
    }

    @Override
    public int countByRoomId(Integer roomId) {
        return storageDeviceDao.countByRoomId(roomId);
    }

    @Override
    public StorageDevice findByNameAndParentRoomId(String name, Integer parentRoomId) {
        return storageDeviceDao.findByNameAndParentRoomId(name, parentRoomId);
    }

}
