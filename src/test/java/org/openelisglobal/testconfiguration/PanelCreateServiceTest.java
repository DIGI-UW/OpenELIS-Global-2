package org.openelisglobal.testconfiguration;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.openelisglobal.systemmodule.service.SystemModuleService;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.openelisglobal.testconfiguration.service.PanelCreateService;
import org.springframework.beans.factory.annotation.Autowired;

public class PanelCreateServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PanelCreateService panelCreateService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private PanelService panelService;
    @Autowired
    private SystemModuleService systemModuleService;
    @Autowired
    private RoleModuleService roleModuleService;
    @Autowired
    private RoleService roleService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/panel-create.xml");
    }

    @Test
    public void insert_ShouldInsertANewPanelInTheDB() {
        Localization localization = new Localization();
        localization.setFrench("Ça va?");
        localization.setEnglish("How is the going?");
        localization.setDescription("Testing description");

        Panel panel = new Panel();
        panel.setPanelName("Test panel");
        panel.setDescription("Urinalysis panel test");
        panel.setIsActive("Y");

        SystemModule workPlanModule = new SystemModule();
        workPlanModule.setSystemModuleName("panel workPlan");
        workPlanModule.setDescription("Work plan module");

        SystemModule resultModule = new SystemModule();
        resultModule.setSystemModuleName("panel resultEntry");
        resultModule.setDescription("Result Entry module");

        SystemModule validationModule = new SystemModule();
        validationModule.setSystemModuleName("panelValidation");
        validationModule.setDescription("Validation module");

        RoleModule workPlanResultModule = new RoleModule();
        workPlanResultModule.setRole(roleService.get("503"));
        workPlanResultModule.setSystemModule(workPlanModule);
        workPlanResultModule.setNameKey("panel_workplan");
        workPlanResultModule.setLastupdated(Timestamp.valueOf("2025-07-15 10:32:00"));

        RoleModule resultResultModule = new RoleModule();
        resultResultModule.setRole(roleService.get("502"));
        resultResultModule.setSystemModule(resultModule);
        resultResultModule.setNameKey("panel resultEntry");
        resultResultModule.setLastupdated(Timestamp.valueOf("2025-07-15 10:32:00"));

        RoleModule validationValidationModule = new RoleModule();
        validationValidationModule.setRole(roleService.get("501"));
        validationValidationModule.setSystemModule(validationModule);
        validationValidationModule.setNameKey("panel_validation");
        validationValidationModule.setLastupdated(Timestamp.valueOf("2025-07-15 10:32:00"));

        List<Localization> initialLocalizations = localizationService.getAll();
        int initialLocalizationCount = initialLocalizations.size();

        List<Panel> initialMethods = panelService.getAll();
        int initialPanelCount = initialMethods.size();

        List<SystemModule> initialSystemModules = systemModuleService.getAll();
        int initialSystemModuleCount = initialSystemModules.size();

        List<RoleModule> initialRoleModules = roleModuleService.getAll();
        int initialRoleModuleCount = initialRoleModules.size();

        panelCreateService.insert(localization, panel, workPlanModule, resultModule, validationModule,
                workPlanResultModule, resultResultModule, validationValidationModule, "1506", "4101");

        List<Localization> newLocalizations = localizationService.getAll();
        assertEquals((initialLocalizationCount + 1), newLocalizations.size());
        assertEquals("Ça va?", newLocalizations.get(initialLocalizationCount).getFrench());

        List<Panel> newMethods = panelService.getAll();
        assertEquals((initialPanelCount + 1), newMethods.size());
        assertEquals("Test panel", newMethods.get(initialPanelCount).getPanelName());

        List<SystemModule> newSystemModules = systemModuleService.getAll();
        assertEquals(initialSystemModuleCount + 3, newSystemModules.size());
        // assertEquals("panel workPlan",
        // newSystemModules.get(initialSystemModuleCount).getSystemModuleName());

        List<RoleModule> newRoleModules = roleModuleService.getAll();
        assertEquals((initialRoleModuleCount + 3), newRoleModules.size());
    }
}
