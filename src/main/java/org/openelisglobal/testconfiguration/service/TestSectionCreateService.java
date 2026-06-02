package org.openelisglobal.testconfiguration.service;

import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TestSectionCreateService {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void insertTestSection(Localization localization, TestSection testSection, SystemModule workplanModule,
            SystemModule resultModule, SystemModule validationModule, RoleModule workplanResultModule,
            RoleModule resultResultModule, RoleModule validationValidationModule);
}
