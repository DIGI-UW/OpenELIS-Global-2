package org.openelisglobal.storage.impl;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.storage.dao.StorageRoomDAO;
import org.openelisglobal.storage.service.StorageRoomService;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StorageRoomServiceImpl extends BaseObjectServiceImpl<StorageRoom, Integer> implements StorageRoomService {
    @Autowired
    private StorageRoomDAO storageRoomDao;

    public StorageRoomServiceImpl() {
        super(StorageRoom.class);
    }

    @Override
    protected StorageRoomDAO getBaseObjectDAO() {
        return storageRoomDao;

    }

    @Override
    @Transactional
    public StorageRoom findByCode(String code) {
        return storageRoomDao.findByCode(code);
    }

    @Override
    @Transactional
    public StorageRoom findByName(String name) {
        return storageRoomDao.findByName(name);
    }

    @Override
    public Optional<StorageRoom> getRoom(int id) {
        return storageRoomDao.get(id);
    }

}
