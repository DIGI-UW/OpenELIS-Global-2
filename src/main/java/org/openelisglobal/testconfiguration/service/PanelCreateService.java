package org.openelisglobal.testconfiguration.service;

import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;

public interface PanelCreateService {

    void insert(Localization localization, Panel panel, SystemModule workplanModule, SystemModule resultModule,
            SystemModule validationModule, RoleModule workplanResultModule, RoleModule resultResultModule,
            RoleModule validationValidationModule, String sampleTypeId, String systemUserId);
}
