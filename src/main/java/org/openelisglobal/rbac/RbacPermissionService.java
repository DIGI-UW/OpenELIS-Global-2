package org.openelisglobal.rbac;

import jakarta.servlet.http.HttpServletRequest;

public interface RbacPermissionService {

    boolean hasPermission(HttpServletRequest request, RbacAction action);
}
