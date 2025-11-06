package org.openelisglobal.testconfiguration.service;

import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.openelisglobal.systemmodule.service.SystemModuleService;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleTypeCreateServiceImpl implements SampleTypeCreateService {

    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private RoleModuleService roleModuleService;
    @Autowired
    private SystemModuleService systemModuleService;
    @Autowired
    private LocalizationService localizationService;

    @Override
    @Transactional
    public void createAndInsertSampleType(Localization localization, TypeOfSample typeOfSample,
            SystemModule workplanModule, SystemModule resultModule, SystemModule validationModule,
            RoleModule workplanResultModule, RoleModule resultResultModule, RoleModule validationValidationModule) {
        localizationService.insert(localization);
        typeOfSample.setLocalization(localization);
        typeOfSampleService.insert(typeOfSample);
        systemModuleService.insert(workplanModule);
        systemModuleService.insert(resultModule);
        systemModuleService.insert(validationModule);
        roleModuleService.insert(workplanResultModule);
        roleModuleService.insert(resultResultModule);
        roleModuleService.insert(validationValidationModule);
    }
}
