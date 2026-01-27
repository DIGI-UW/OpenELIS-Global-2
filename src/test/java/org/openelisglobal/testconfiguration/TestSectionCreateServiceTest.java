package org.openelisglobal.testconfiguration;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.openelisglobal.systemmodule.service.SystemModuleService;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testconfiguration.service.TestSectionCreateService;
import org.springframework.beans.factory.annotation.Autowired;

public class TestSectionCreateServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestSectionCreateService testSectionCreateService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private SystemModuleService systemModuleService;
    @Autowired
    private RoleModuleService roleModuleService;
    @Autowired
    private RoleService roleService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test-section-create.xml");
    }

    @Test
    public void insertMethod_ShouldInsertANewMethodInTheDB() {
        Localization localization = new Localization();
        localization.setFrench("je vais bein");
        localization.setEnglish("I'm doing well");

        TestSection testSection = new TestSection();
        testSection.setTestSectionName("test_Section");
        testSection.setDescription("operation test Sections");
        testSection.setSortOrder("3");

        SystemModule workPlanModule = new SystemModule();
        workPlanModule.setSystemModuleName("test_section workPlan");
        workPlanModule.setDescription("Work plan module");

        SystemModule resultModule = new SystemModule();
        resultModule.setSystemModuleName("testSection_resultEntry");
        resultModule.setDescription("Result Entry module");

        SystemModule validationModule = new SystemModule();
        validationModule.setSystemModuleName("testSection_Validation");
        validationModule.setDescription("Validation module");

        RoleModule workPlanResultModule = new RoleModule();
        workPlanResultModule.setRole(roleService.get("501"));
        workPlanResultModule.setSystemModule(workPlanModule);
        workPlanResultModule.setNameKey("testSection_workplan");
        workPlanResultModule.setLastupdated(Timestamp.valueOf("2025-07-15 10:32:00"));

        RoleModule resultResultModule = new RoleModule();
        resultResultModule.setRole(roleService.get("502"));
        resultResultModule.setSystemModule(resultModule);
        resultResultModule.setNameKey("testSection_resultEntry");
        resultResultModule.setLastupdated(Timestamp.valueOf("2025-07-15 10:32:00"));

        RoleModule validationValidationModule = new RoleModule();
        validationValidationModule.setRole(roleService.get("503"));
        validationValidationModule.setSystemModule(validationModule);
        validationValidationModule.setNameKey("testSection_validation");
        validationValidationModule.setLastupdated(Timestamp.valueOf("2025-07-15 10:32:00"));

        List<Localization> initialLocalizations = localizationService.getAll();
        int initialLocalizationCount = initialLocalizations.size();

        List<TestSection> initialTestSections = testSectionService.getAll();
        int initialTestSectionCount = initialTestSections.size();

        List<SystemModule> initialSystemModules = systemModuleService.getAll();
        int initialSystemModuleCount = initialSystemModules.size();

        List<RoleModule> initialRoleModules = roleModuleService.getAll();
        int initialRoleModuleCount = initialRoleModules.size();

        testSectionCreateService.insertTestSection(localization, testSection, workPlanModule, resultModule,
                validationModule, workPlanResultModule, resultResultModule, validationValidationModule);

        List<Localization> newLocalizations = localizationService.getAll();
        assertEquals((initialLocalizationCount + 1), newLocalizations.size());
        assertEquals("je vais bein", newLocalizations.get(initialLocalizationCount).getFrench());

        List<TestSection> newTestSections = testSectionService.getAll();
        assertEquals((initialTestSectionCount + 1), newTestSections.size());
        assertEquals("test_Section", newTestSections.get(initialTestSectionCount).getTestSectionName());

        List<SystemModule> newSystemModules = systemModuleService.getAll();
        assertEquals(initialSystemModuleCount + 3, newSystemModules.size());
        // assertEquals("test_section workPlan",
        // newSystemModules.get(initialSystemModuleCount).getSystemModuleName());

        List<RoleModule> newRoleModules = roleModuleService.getAll();
        assertEquals((initialRoleModuleCount + 3), newRoleModules.size());
    }
}
