package org.openelisglobal.method.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.method.valueholder.Method;

public interface MethodService extends BaseObjectService<Method, String> {
    List<Method> getMethods(String filter);

    List<Method> getAllInActiveMethods();

    Map<String, String> getMethodUnitIdToNameMap();

    void refreshNames();

    List<Method> getAllActiveMethods();

    /**
     * Nullable lookup by id — returns null instead of throwing
     * ObjectNotFoundException when the method does not exist. Use on read paths
     * that must tolerate a dangling reference (e.g. an orphaned test_method link).
     */
    Method findById(String id);
}
