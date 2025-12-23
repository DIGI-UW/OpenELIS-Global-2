package org.openelisglobal.panellabunit.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.panellabunit.dao.PanelLabUnitDAO;
import org.openelisglobal.panellabunit.valueholder.PanelLabUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PanelLabUnitServiceImpl extends AuditableBaseObjectServiceImpl<PanelLabUnit, String>
        implements PanelLabUnitService {
    @Autowired
    protected PanelLabUnitDAO baseObjectDAO;

    PanelLabUnitServiceImpl() {
        super(PanelLabUnit.class);
    }

    @Override
    protected PanelLabUnitDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
