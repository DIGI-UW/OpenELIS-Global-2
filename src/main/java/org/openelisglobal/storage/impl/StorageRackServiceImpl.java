package org.openelisglobal.storage.impl;

import jakarta.transaction.Transactional;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.service.StorageRackService;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StorageRackServiceImpl extends BaseObjectServiceImpl<StorageRack, Integer> implements StorageRackService {

    @Autowired
    private StorageRackDAO storageRackDao;

    public StorageRackServiceImpl() {
        super(StorageRack.class);

    }

    @Override
    protected StorageRackDAO getBaseObjectDAO() {
        return storageRackDao;
    }

    @Override
    @Transactional
    public List<StorageRack> findByParentShelfId(Integer shelfId) {
        return storageRackDao.findByParentShelfId(shelfId);
    }

    @Override
    @Transactional
    public StorageRack findByLabel(String label) {
        return storageRackDao.findByLabel(label);
    }

    @Override
    @Transactional
    public StorageRack findByLabelAndParentShelf(String label, StorageShelf parentShelf) {
        return storageRackDao.findByLabelAndParentShelf(label, parentShelf);
    }

    @Override
    @Transactional
    public int countByShelfId(Integer shelfId) {
        return storageRackDao.countByShelfId(shelfId);
    }

    @Override
    @Transactional
    public StorageRack findByLabelAndParentShelfId(String label, Integer parentShelfId) {
        return storageRackDao.findByLabelAndParentShelfId(label, parentShelfId);
    }

    @Override
    @Transactional
    public StorageRack findByCode(String code) {
        return storageRackDao.findByCode(code);
    }

}
