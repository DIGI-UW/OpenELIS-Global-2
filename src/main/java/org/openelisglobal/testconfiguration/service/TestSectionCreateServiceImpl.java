package org.openelisglobal.testconfiguration.service;

import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.openelisglobal.systemmodule.service.SystemModuleService;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestSectionCreateServiceImpl implements TestSectionCreateService {

    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private RoleModuleService roleModuleService;
    @Autowired
    private SystemModuleService systemModuleService;

    @Override
    @Transactional
    public void insertTestSection(Localization localization, TestSection testSection, SystemModule workplanModule,
            SystemModule resultModule, SystemModule validationModule, RoleModule workplanResultModule,
            RoleModule resultResultModule, RoleModule validationValidationModule) {
        localizationService.insert(localization);
        testSection.setLocalization(localization);
        testSectionService.insert(testSection);
        systemModuleService.insert(workplanModule);
        systemModuleService.insert(resultModule);
        systemModuleService.insert(validationModule);
        roleModuleService.insert(workplanResultModule);
        roleModuleService.insert(resultResultModule);
        roleModuleService.insert(validationValidationModule);
    }
}
