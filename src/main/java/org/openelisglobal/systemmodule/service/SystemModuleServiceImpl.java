package org.openelisglobal.systemmodule.service;

import java.util.List;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.systemmodule.dao.SystemModuleDAO;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemModuleServiceImpl extends AuditableBaseObjectServiceImpl<SystemModule, String>
        implements SystemModuleService {
    @Autowired
    protected SystemModuleDAO baseObjectDAO;

    SystemModuleServiceImpl() {
        super(SystemModule.class);
    }

    @Override
    protected SystemModuleDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SystemModule systemModule) {
        getBaseObjectDAO().getData(systemModule);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSystemModuleCount() {
        return getBaseObjectDAO().getTotalSystemModuleCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemModule> getPageOfSystemModules(int startingRecNo) {
        return getBaseObjectDAO().getPageOfSystemModules(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemModule> getAllSystemModules() {
        return getBaseObjectDAO().getAllSystemModules();
    }

    @Override
    @Transactional(readOnly = true)
    public SystemModule getSystemModuleByName(String name) {
        return getBaseObjectDAO().getSystemModuleByName(name);
    }

    @Override
    public String insert(SystemModule systemModule) {
        if (getBaseObjectDAO().duplicateSystemModuleExists(systemModule)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + systemModule.getSystemModuleName());
        }
        return super.insert(systemModule);
    }

    @Override
    public SystemModule save(SystemModule systemModule) {
        if (getBaseObjectDAO().duplicateSystemModuleExists(systemModule)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + systemModule.getSystemModuleName());
        }
        return super.save(systemModule);
    }

    @Override
    public SystemModule update(SystemModule systemModule) {
        if (getBaseObjectDAO().duplicateSystemModuleExists(systemModule)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + systemModule.getSystemModuleName());
        }
        return super.update(systemModule);
    }
}
