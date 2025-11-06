package org.openelisglobal.county.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.county.dao.CountyDAO;
import org.openelisglobal.county.valueholder.County;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CountyServiceImpl extends AuditableBaseObjectServiceImpl<County, String> implements CountyService {
    @Autowired
    protected CountyDAO baseObjectDAO;

    CountyServiceImpl() {
        super(County.class);
    }

    @Override
    protected CountyDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
