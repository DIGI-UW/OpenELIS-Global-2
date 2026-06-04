package org.openelisglobal.storage.impl;

import jakarta.transaction.Transactional;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.storage.dao.StorageBoxDAO;
import org.openelisglobal.storage.service.StorageBoxService;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StorageBoxServiceImpl extends BaseObjectServiceImpl<StorageBox, Integer> implements StorageBoxService {

    @Autowired
    private StorageBoxDAO storageBoxDao;

    public StorageBoxServiceImpl() {
        super(StorageBox.class);
    }

    @Override
    protected BaseDAO<StorageBox, Integer> getBaseObjectDAO() {
        return storageBoxDao;

    }

    @Override
    @Transactional
    public List<StorageBox> findByParentRackId(Integer rackId) {
        return storageBoxDao.findByParentRackId(rackId);
    }

    @Override
    @Transactional
    public StorageBox findByCoordinates(String coordinates) {
        return storageBoxDao.findByCoordinates(coordinates);
    }

    @Override
    @Transactional
    public StorageBox findByCoordinatesAndParentRack(String coordinates, StorageRack parentRack) {
        return storageBoxDao.findByCoordinatesAndParentRack(coordinates, parentRack);
    }

    @Override
    @Transactional
    public int countOccupied(Integer rackId) {
        return storageBoxDao.countOccupied(rackId);
    }

    @Override
    @Transactional
    public int countOccupiedInShelf(Integer shelfId) {
        return storageBoxDao.countOccupiedInShelf(shelfId);
    }

    @Override
    @Transactional
    public int countOccupiedInDevice(Integer deviceId) {
        return storageBoxDao.countOccupiedInDevice(deviceId);
    }

}
