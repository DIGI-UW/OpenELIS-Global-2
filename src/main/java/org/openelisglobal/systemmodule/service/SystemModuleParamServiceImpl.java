package org.openelisglobal.systemmodule.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.systemmodule.dao.SystemModuleParamDAO;
import org.openelisglobal.systemmodule.valueholder.SystemModuleParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemModuleParamServiceImpl extends AuditableBaseObjectServiceImpl<SystemModuleParam, String>
        implements SystemModuleParamService {
    @Autowired
    protected SystemModuleParamDAO baseObjectDAO;

    SystemModuleParamServiceImpl() {
        super(SystemModuleParam.class);
    }

    @Override
    protected SystemModuleParamDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
