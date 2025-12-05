package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.StorageLocationDAO;
import org.openelisglobal.inventory.valueholder.StorageLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorageLocationServiceImpl extends AuditableBaseObjectServiceImpl<StorageLocation, String>
        implements StorageLocationService {

    @Autowired
    protected StorageLocationDAO baseObjectDAO;

    StorageLocationServiceImpl() {
        super(StorageLocation.class);
    }

    @Override
    protected StorageLocationDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageLocation> findAllActive() {
        return getBaseObjectDAO().findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public StorageLocation findByLocationCode(String locationCode) {
        return getBaseObjectDAO().findByLocationCode(locationCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageLocation> findByParentLocationId(String parentLocationId) {
        return getBaseObjectDAO().findByParentLocationId(parentLocationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageLocation> findRootLocations() {
        return getBaseObjectDAO().findRootLocations();
    }
}
