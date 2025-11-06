package org.openelisglobal.testconfiguration.service;

import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

public interface SampleTypeCreateService {

    void createAndInsertSampleType(Localization localization, TypeOfSample typeOfSample, SystemModule workplanModule,
            SystemModule resultModule, SystemModule validationModule, RoleModule workplanResultModule,
            RoleModule resultResultModule, RoleModule validationValidationModule);
}
