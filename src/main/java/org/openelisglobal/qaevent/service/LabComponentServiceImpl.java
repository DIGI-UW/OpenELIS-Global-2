package org.openelisglobal.qaevent.service;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qaevent.dao.LabComponentDAO;
import org.openelisglobal.qaevent.valueholder.LabComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LabComponentServiceImpl extends AuditableBaseObjectServiceImpl<LabComponent, String>
        implements LabComponentService {

    @Autowired
    private LabComponentDAO baseObjectDAO;

    public LabComponentServiceImpl() {
        super(LabComponent.class);
    }

    @Override
    protected BaseDAO<LabComponent, String> getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
