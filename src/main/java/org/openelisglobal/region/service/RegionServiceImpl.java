package org.openelisglobal.region.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.region.dao.RegionDAO;
import org.openelisglobal.region.valueholder.Region;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegionServiceImpl extends AuditableBaseObjectServiceImpl<Region, String> implements RegionService {
    @Autowired
    protected RegionDAO baseObjectDAO;

    RegionServiceImpl() {
        super(Region.class);
    }

    @Override
    protected RegionDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
