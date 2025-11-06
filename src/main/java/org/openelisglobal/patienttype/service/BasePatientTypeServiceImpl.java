package org.openelisglobal.patienttype.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.patienttype.dao.BasePatientTypeDAO;
import org.openelisglobal.patienttype.valueholder.BasePatientType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BasePatientTypeServiceImpl extends AuditableBaseObjectServiceImpl<BasePatientType, String>
        implements BasePatientTypeService {
    @Autowired
    protected BasePatientTypeDAO baseObjectDAO;

    BasePatientTypeServiceImpl() {
        super(BasePatientType.class);
    }

    @Override
    protected BasePatientTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
