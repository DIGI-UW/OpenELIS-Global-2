package org.openelisglobal.testconfiguration.service;

import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.openelisglobal.systemmodule.service.SystemModuleService;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MethodCreateServiceImpl implements MethodCreateService {

    @Autowired
    private MethodService methodService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private RoleModuleService roleModuleService;
    @Autowired
    private SystemModuleService systemModuleService;

    @Override
    @Transactional
    public void insertMethod(Localization localization, Method method, SystemModule workplanModule,
            SystemModule resultModule, SystemModule validationModule, RoleModule workplanResultModule,
            RoleModule resultResultModule, RoleModule validationValidationModule) {
        localizationService.insert(localization);
        method.setLocalization(localization);
        methodService.insert(method);
        systemModuleService.insert(workplanModule);
        systemModuleService.insert(resultModule);
        systemModuleService.insert(validationModule);
        roleModuleService.insert(workplanResultModule);
        roleModuleService.insert(resultResultModule);
        roleModuleService.insert(validationValidationModule);
    }
}
