package org.openelisglobal.testconfiguration.service;

import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MethodCreateService {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void insertMethod(Localization localization, Method method, SystemModule workplanModule, SystemModule resultModule,
            SystemModule validationModule, RoleModule workplanResultModule, RoleModule resultResultModule,
            RoleModule validationValidationModule);
}
