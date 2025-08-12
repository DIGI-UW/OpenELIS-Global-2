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
import org.openelisglobal.testconfiguration.service.SampleTypeCreateService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleTypeCreateServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleTypeCreateService sampleTypeCreateService;

    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private SystemModuleService systemModuleService;
    @Autowired
    private RoleModuleService roleModuleService;
    @Autowired
    private RoleService roleService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-type-create.xml");
    }

    @Test
    public void createAndInsertSampleType_ShouldInsertANewSampleTypeInTheDB() {
        Localization localization = new Localization();
        localization.setFrench("Salut");
        localization.setEnglish("Hi!");

        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setDescription("Fluidal acids");
        typeOfSample.setDomain("H");
        typeOfSample.setLastupdated(Timestamp.valueOf("2024-11-01 12:00:00"));
        typeOfSample.setLocalAbbreviation("Fluids");

        SystemModule workPlanModule = new SystemModule();
        workPlanModule.setSystemModuleName("sample_type workPlan");
        workPlanModule.setDescription("Work plan module");

        SystemModule resultModule = new SystemModule();
        resultModule.setSystemModuleName("sampleType_resultEntry");
        resultModule.setDescription("Result Entry module");

        SystemModule validationModule = new SystemModule();
        validationModule.setSystemModuleName("sampleTypeValidation");
        validationModule.setDescription("Validation module");

        RoleModule workPlanResultModule = new RoleModule();
        workPlanResultModule.setRole(roleService.get("501"));
        workPlanResultModule.setSystemModule(workPlanModule);
        workPlanResultModule.setNameKey("sample-type_workPlan module");
        workPlanResultModule.setLastupdated(Timestamp.valueOf("2025-07-15 10:32:00"));

        RoleModule resultResultModule = new RoleModule();
        resultResultModule.setRole(roleService.get("502"));
        resultResultModule.setSystemModule(resultModule);
        resultResultModule.setNameKey("sample_type resultEntry");
        resultResultModule.setLastupdated(Timestamp.valueOf("2025-07-15 10:32:00"));

        RoleModule validationValidationModule = new RoleModule();
        validationValidationModule.setRole(roleService.get("503"));
        validationValidationModule.setSystemModule(validationModule);
        validationValidationModule.setNameKey("sample_type validation");
        validationValidationModule.setLastupdated(Timestamp.valueOf("2025-07-15 10:32:00"));

        List<Localization> initialLocalizations = localizationService.getAll();
        int initialLocalizationCount = initialLocalizations.size();

        List<TypeOfSample> initialTypeOfSamples = typeOfSampleService.getAll();
        int initialTypeOfSampleCount = initialTypeOfSamples.size();

        List<SystemModule> initialSystemModules = systemModuleService.getAll();
        int initialSystemModuleCount = initialSystemModules.size();

        List<RoleModule> initialRoleModules = roleModuleService.getAll();
        int initialRoleModuleCount = initialRoleModules.size();

        sampleTypeCreateService.createAndInsertSampleType(localization, typeOfSample, workPlanModule, resultModule,
                validationModule, workPlanResultModule, resultResultModule, validationValidationModule);

        List<Localization> newLocalizations = localizationService.getAll();
        assertEquals((initialLocalizationCount + 1), newLocalizations.size());
        assertEquals("Salut", newLocalizations.get(initialLocalizationCount).getFrench());

        List<TypeOfSample> newTypeOfSamples = typeOfSampleService.getAll();

        assertEquals((initialTypeOfSampleCount + 1), newTypeOfSamples.size());
        assertEquals("Fluidal acids", newTypeOfSamples.get(initialTypeOfSampleCount).getDescription());

        List<SystemModule> newSystemModules = systemModuleService.getAll();
        assertEquals(initialSystemModuleCount + 3, newSystemModules.size());
        // assertEquals("sample_type workPlan",
        // newSystemModules.get(initialSystemModuleCount).getSystemModuleName());

        List<RoleModule> newRoleModules = roleModuleService.getAll();
        assertEquals((initialRoleModuleCount + 3), newRoleModules.size());
    }
}
