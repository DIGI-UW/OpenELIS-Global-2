package org.openelisglobal.datasubmission.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.datasubmission.dao.DataValueDAO;
import org.openelisglobal.datasubmission.valueholder.DataValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataValueServiceImpl extends AuditableBaseObjectServiceImpl<DataValue, String>
        implements DataValueService {
    @Autowired
    protected DataValueDAO baseObjectDAO;

    DataValueServiceImpl() {
        super(DataValue.class);
    }

    @Override
    protected DataValueDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
