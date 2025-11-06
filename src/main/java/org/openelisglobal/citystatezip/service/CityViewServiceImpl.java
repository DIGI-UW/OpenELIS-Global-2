package org.openelisglobal.citystatezip.service;

import org.openelisglobal.citystatezip.dao.CityViewDAO;
import org.openelisglobal.citystatezip.valueholder.CityView;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CityViewServiceImpl extends AuditableBaseObjectServiceImpl<CityView, String> implements CityViewService {
    @Autowired
    protected CityViewDAO baseObjectDAO;

    CityViewServiceImpl() {
        super(CityView.class);
    }

    @Override
    protected CityViewDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
