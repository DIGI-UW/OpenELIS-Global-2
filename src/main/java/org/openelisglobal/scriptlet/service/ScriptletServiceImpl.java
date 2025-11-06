package org.openelisglobal.scriptlet.service;

import java.util.List;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.scriptlet.dao.ScriptletDAO;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScriptletServiceImpl extends AuditableBaseObjectServiceImpl<Scriptlet, String>
        implements ScriptletService {
    @Autowired
    protected ScriptletDAO baseObjectDAO;

    ScriptletServiceImpl() {
        super(Scriptlet.class);
    }

    @Override
    protected ScriptletDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Scriptlet scriptlet) {
        getBaseObjectDAO().getData(scriptlet);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalScriptletCount() {
        return getBaseObjectDAO().getTotalScriptletCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scriptlet> getPageOfScriptlets(int startingRecNo) {
        return getBaseObjectDAO().getPageOfScriptlets(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public Scriptlet getScriptletByName(Scriptlet scriptlet) {
        return getBaseObjectDAO().getScriptletByName(scriptlet);
    }

    @Override
    @Transactional(readOnly = true)
    public Scriptlet getScriptletById(String scriptletId) {
        return getBaseObjectDAO().getScriptletById(scriptletId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scriptlet> getScriptlets(String filter) {
        return getBaseObjectDAO().getScriptlets(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scriptlet> getAllScriptlets() {
        return getBaseObjectDAO().getAllScriptlets();
    }

    @Override
    public String insert(Scriptlet scriptlet) {
        if (duplicateScriptletExists(scriptlet)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + scriptlet.getScriptletName());
        }
        return super.insert(scriptlet);
    }

    @Override
    public Scriptlet save(Scriptlet scriptlet) {
        if (duplicateScriptletExists(scriptlet)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + scriptlet.getScriptletName());
        }
        return super.save(scriptlet);
    }

    @Override
    public Scriptlet update(Scriptlet scriptlet) {
        if (duplicateScriptletExists(scriptlet)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + scriptlet.getScriptletName());
        }
        return super.update(scriptlet);
    }

    private boolean duplicateScriptletExists(Scriptlet scriptlet) {
        return baseObjectDAO.duplicateScriptletExists(scriptlet);
    }
}
