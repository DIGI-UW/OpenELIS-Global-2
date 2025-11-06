package org.openelisglobal.testconfiguration.service;

import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.openelisglobal.test.valueholder.TestSection;

public interface TestSectionCreateService {

    void insertTestSection(Localization localization, TestSection testSection, SystemModule workplanModule,
            SystemModule resultModule, SystemModule validationModule, RoleModule workplanResultModule,
            RoleModule resultResultModule, RoleModule validationValidationModule);
}
