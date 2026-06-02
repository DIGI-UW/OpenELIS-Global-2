package org.openelisglobal.method.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.method.valueholder.Method;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MethodService extends BaseObjectService<Method, String> {

    @PreAuthorize("hasAuthority('PRIV_METHOD_VIEW')")
    List<Method> getMethods(String filter);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Method> getAllInActiveMethods();

    @PreAuthorize("hasAuthority('PRIV_METHOD_VIEW')")
    Map<String, String> getMethodUnitIdToNameMap();

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void refreshNames();

    @PreAuthorize("hasAuthority('PRIV_METHOD_VIEW')")
    List<Method> getAllActiveMethods();
}
