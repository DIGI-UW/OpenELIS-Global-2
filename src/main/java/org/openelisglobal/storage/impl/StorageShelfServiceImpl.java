package org.openelisglobal.storage.impl;

import jakarta.transaction.Transactional;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.service.StorageShelfService;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StorageShelfServiceImpl extends BaseObjectServiceImpl<StorageShelf, Integer>
        implements StorageShelfService {
    @Autowired
    private StorageShelfDAO storageShelfDao;

    public StorageShelfServiceImpl() {
        super(StorageShelf.class);
    }

    @Override
    protected StorageShelfDAO getBaseObjectDAO() {
        return storageShelfDao;

    }

    @Override
    @Transactional
    public List<StorageShelf> findByParentDeviceId(Integer deviceId) {
        return storageShelfDao.findByParentDeviceId(deviceId);
    }

    @Override
    @Transactional
    public StorageShelf findByLabel(String label) {
        return storageShelfDao.findByLabel(label);
    }

    @Override
    @Transactional
    public StorageShelf findByLabelAndParentDevice(String label, StorageDevice parentDevice) {
        return storageShelfDao.findByLabelAndParentDevice(label, parentDevice);
    }

    @Override
    @Transactional
    public int countByDeviceId(Integer deviceId) {
        return storageShelfDao.countByDeviceId(deviceId);
    }

    @Override
    @Transactional
    public StorageShelf findByLabelAndParentDeviceId(String label, Integer parentDeviceId) {
        return storageShelfDao.findByLabelAndParentDeviceId(label, parentDeviceId);
    }

    @Override
    @Transactional
    public StorageShelf findByCode(String code) {
        return storageShelfDao.findByCode(code);
    }

}
