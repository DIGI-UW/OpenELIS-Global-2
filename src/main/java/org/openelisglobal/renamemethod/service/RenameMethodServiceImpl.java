package org.openelisglobal.renamemethod.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Hibernate;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.renamemethod.dao.RenameMethodDAO;
import org.openelisglobal.renamemethod.valueholder.RenameMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RenameMethodServiceImpl extends AuditableBaseObjectServiceImpl<RenameMethod, String>
        implements RenameMethodService {
    @Autowired
    protected RenameMethodDAO baseObjectDAO;

    private Map<String, String> testUnitIdToNameMap;

    RenameMethodServiceImpl() {
        super(RenameMethod.class);
    }

    @Override
    protected RenameMethodDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RenameMethod> getMethods(String filter) {
        return getBaseObjectDAO().getMethods(filter);
    }

    @Override
    public void delete(RenameMethod method) {
        RenameMethod oldMethod = get(method.getId());
        oldMethod.setIsActive(IActionConstants.NO);
        oldMethod.setSysUserId(method.getSysUserId());
        updateDelete(oldMethod);
    }

    @Override
    public String insert(RenameMethod method) {
        if (getBaseObjectDAO().duplicateMethodExists(method)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + method.getMethodName());
        }
        return super.insert(method);
    }

    @Override
    public RenameMethod save(RenameMethod method) {
        if (getBaseObjectDAO().duplicateMethodExists(method)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + method.getMethodName());
        }
        return super.save(method);
    }

    @Override
    public RenameMethod update(RenameMethod method) {
        if (getBaseObjectDAO().duplicateMethodExists(method)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + method.getMethodName());
        }
        return super.update(method);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RenameMethod> getAllInActiveMethods() {
        return getBaseObjectDAO().getAllInActiveMethods();
    }

    @Override
    public void refreshNames() {
        methodNamesChanged();
    }

    public void methodNamesChanged() {
        createMethodToNameMap();
    }

    private synchronized void createMethodToNameMap() {
        testUnitIdToNameMap = new HashMap<>();

        List<RenameMethod> methods = baseObjectDAO.getAll();

        for (RenameMethod method : methods) {
            testUnitIdToNameMap.put(method.getId(), buildMethodName(method).replace("\n", " "));
        }
    }

    private String buildMethodName(RenameMethod method) {
        return method.getLocalization().getLocalizedValue();
    }

    @Override
    @Transactional(readOnly = true)
    public Localization getLocalizationForRenameMethod(String id) {
        RenameMethod renameMethod = get(id);
        Localization localization = renameMethod != null ? renameMethod.getLocalization() : null;
        Hibernate.initialize(localization);
        return localization;
    }
}
