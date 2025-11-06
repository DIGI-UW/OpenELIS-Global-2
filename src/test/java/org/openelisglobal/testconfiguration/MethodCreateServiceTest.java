package org.openelisglobal.testconfiguration;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.openelisglobal.systemmodule.service.SystemModuleService;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.openelisglobal.testconfiguration.service.MethodCreateService;
import org.springframework.beans.factory.annotation.Autowired;

public class MethodCreateServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MethodCreateService methodCreateService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private MethodService methodService;
    @Autowired
    private SystemModuleService systemModuleService;
    @Autowired
    private RoleModuleService roleModuleService;
    @Autowired
    private RoleService roleService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/method-create.xml");
    }

    @Test
    public void insertMethod_ShouldInsertANewMethodInTheDB() {
        Localization localization = new Localization();
        localization.setEnglish("Please");
        localization.setFrench("S'il vous plait!");

        Method method = new Method();
        method.setMethodName("hydro-therapy");
        method.setDescription("using therapy");
        method.setLocalization(localization);

        SystemModule workPlanModule = new SystemModule();
        workPlanModule.setSystemModuleName("workPlan");
        workPlanModule.setDescription("Work plan module");

        SystemModule resultModule = new SystemModule();
        resultModule.setSystemModuleName("resultEntry");
        resultModule.setDescription("Result Entry module");

        SystemModule validationModule = new SystemModule();
        validationModule.setSystemModuleName("Validation");
        validationModule.setDescription("Validation module");

        RoleModule workPlanResultModule = new RoleModule();
        workPlanResultModule.setRole(roleService.get("701"));
        workPlanResultModule.setSystemModule(workPlanModule);
        workPlanResultModule.setNameKey("workplan");
        workPlanResultModule.setLastupdated(Timestamp.valueOf("2025-07-15 10:32:00"));

        RoleModule resultResultModule = new RoleModule();
        resultResultModule.setRole(roleService.get("702"));
        resultResultModule.setSystemModule(resultModule);
        resultResultModule.setNameKey("resultEntry");
        resultResultModule.setLastupdated(Timestamp.valueOf("2025-07-15 10:32:00"));

        RoleModule validationValidationModule = new RoleModule();
        validationValidationModule.setRole(roleService.get("703"));
        validationValidationModule.setSystemModule(validationModule);
        validationValidationModule.setNameKey("validation");
        validationValidationModule.setLastupdated(Timestamp.valueOf("2025-07-15 10:32:00"));

        List<Localization> initialLocalizations = localizationService.getAll();
        int initialLocalizationCount = initialLocalizations.size();

        List<Method> initialMethods = methodService.getAll();
        int initialMethodCount = initialMethods.size();

        List<SystemModule> initialSystemModules = systemModuleService.getAll();
        int initialSystemModuleCount = initialSystemModules.size();

        List<RoleModule> initialRoleModules = roleModuleService.getAll();
        int initialRoleModuleCount = initialRoleModules.size();

        methodCreateService.insertMethod(localization, method, workPlanModule, resultModule, validationModule,
                workPlanResultModule, resultResultModule, validationValidationModule);

        List<Localization> newLocalizations = localizationService.getAll();
        assertEquals((initialLocalizationCount + 1), newLocalizations.size());
        assertEquals("S'il vous plait!", newLocalizations.get(initialLocalizationCount).getFrench());

        List<Method> newMethods = methodService.getAll();
        assertEquals((initialMethodCount + 1), newMethods.size());
        assertEquals("hydro-therapy", newMethods.get(initialMethodCount).getMethodName());

        List<SystemModule> newSystemModules = systemModuleService.getAll();
        assertEquals(initialSystemModuleCount + 3, newSystemModules.size());
        // assertEquals("workPlan",
        // newSystemModules.get(initialSystemModuleCount).getSystemModuleName());

        List<RoleModule> newRoleModules = roleModuleService.getAll();
        assertEquals((initialRoleModuleCount + 3), newRoleModules.size());
        // assertEquals("workplan",
        // newRoleModules.get(initialRoleModuleCount).getNameKey());
    }
}
