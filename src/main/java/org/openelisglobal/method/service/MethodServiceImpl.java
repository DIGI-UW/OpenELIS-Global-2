package org.openelisglobal.method.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.method.dao.MethodDAO;
import org.openelisglobal.method.valueholder.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@DependsOn({ "springContext" })
public class MethodServiceImpl extends AuditableBaseObjectServiceImpl<Method, String> implements MethodService {
    @Autowired
    protected MethodDAO baseObjectDAO;

    private Map<String, String> methodUnitIdToNameMap;

    MethodServiceImpl() {
        super(Method.class);
    }

    @Override
    protected MethodDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Method> getMethods(String filter) {
        return getBaseObjectDAO().getMethods(filter);
    }

    @Override
    public void delete(Method method) {
        Method oldMethod = get(method.getId());
        oldMethod.setIsActive(IActionConstants.NO);
        oldMethod.setSysUserId(method.getSysUserId());
        updateDelete(oldMethod);
    }

    @Override
    public String insert(Method method) {
        if (getBaseObjectDAO().duplicateMethodExists(method)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + method.getMethodName());
        }
        return super.insert(method);
    }

    @Override
    public Method save(Method method) {
        if (getBaseObjectDAO().duplicateMethodExists(method)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + method.getMethodName());
        }
        return super.save(method);
    }

    @Override
    public Method update(Method method) {
        if (getBaseObjectDAO().duplicateMethodExists(method)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + method.getMethodName());
        }
        return super.update(method);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Method> getAllInActiveMethods() {
        return getBaseObjectDAO().getAllInActiveMethods();
    }

    @Override
    public void refreshNames() {
        methodNamesChanged();
    }

    public void methodNamesChanged() {
        createMethodToNameMap();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Method> getAllActiveMethods() {
        return getBaseObjectDAO().getAllActiveMethods();
    }

    private synchronized void createMethodToNameMap() {
        methodUnitIdToNameMap = new HashMap<>();

        List<Method> methods = baseObjectDAO.getAll();

        for (Method method : methods) {
            methodUnitIdToNameMap.put(method.getId(), buildMethodName(method).replace("\n", " "));
        }
    }

    private String buildMethodName(Method method) {
        return method.getLocalization().getLocalizedValue();
    }
}
