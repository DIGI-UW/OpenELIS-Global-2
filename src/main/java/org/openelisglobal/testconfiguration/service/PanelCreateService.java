package org.openelisglobal.testconfiguration.service;

import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.springframework.security.access.prepost.PreAuthorize;

public interface PanelCreateService {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void insert(Localization localization, Panel panel, SystemModule workplanModule, SystemModule resultModule,
            SystemModule validationModule, RoleModule workplanResultModule, RoleModule resultResultModule,
            RoleModule validationValidationModule, String sampleTypeId, String systemUserId);
}
