package org.openelisglobal.testconfiguration.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import javax.validation.Valid;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.openelisglobal.testconfiguration.form.SampleTypeCreateForm;
import org.openelisglobal.testconfiguration.service.SampleTypeCreateService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
@PreAuthorize("hasRole('ADMIN')")
public class SampleTypeCreateRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "sampleTypeEnglishName", "sampleTypeFrenchName", "domain" };

    public static final String NAME_SEPARATOR = "$";

    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private SampleTypeCreateService sampleTypeCreateService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/SampleTypeCreate")
    public SampleTypeCreateForm showSampleTypeCreate(HttpServletRequest request) {

        SampleTypeCreateForm form = new SampleTypeCreateForm();

        setupDisplayItems(form);

        // return findForward(FWD_SUCCESS, form);
        return form;
    }

    private void setupDisplayItems(SampleTypeCreateForm form) {
        form.setExistingSampleTypeList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE));
        form.setInactiveSampleTypeList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE));
        List<TypeOfSample> typeOfSamples = typeOfSampleService.getAllTypeOfSamples();
        form.setExistingEnglishNames(getExistingTestNames(typeOfSamples, Locale.ENGLISH));
        form.setExistingFrenchNames(getExistingTestNames(typeOfSamples, Locale.FRENCH));
    }

    private String getExistingTestNames(List<TypeOfSample> typeOfSamples, Locale locale) {
        StringBuilder builder = new StringBuilder(NAME_SEPARATOR);

        for (TypeOfSample typeOfSample : typeOfSamples) {
            builder.append(typeOfSample.getLocalization().getLocalizedValue(locale));
            builder.append(NAME_SEPARATOR);
        }

        return builder.toString();
    }

    @PostMapping(value = "/SampleTypeCreate")
    public SampleTypeCreateForm postSampleTypeCreate(HttpServletRequest request,
            @RequestBody @Valid SampleTypeCreateForm form, BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            setupDisplayItems(form);
            // return findForward(FWD_FAIL_INSERT, form);
            return form;
        }
        String identifyingName = form.getSampleTypeEnglishName();
        String userId = getSysUserId(request);
        String frontendDomain = form.getDomain();
        String backendDomainCode = mapFrontendDomainToBackendCode(frontendDomain);

        // Domain mapping completed for sample type creation

        Localization localization = createLocalization(form.getSampleTypeFrenchName(), identifyingName, userId);

        TypeOfSample typeOfSample = createTypeOfSample(identifyingName, userId, backendDomainCode);

        SystemModule workplanModule = createSystemModule("Workplan", identifyingName, userId);
        SystemModule resultModule = createSystemModule("LogbookResults", identifyingName, userId);
        SystemModule validationModule = createSystemModule("ResultValidation", identifyingName, userId);

        Role resultsEntryRole = roleService.getRoleByName(Constants.ROLE_RESULTS);
        Role validationRole = roleService.getRoleByName(Constants.ROLE_VALIDATION);

        RoleModule workplanResultModule = createRoleModule(userId, workplanModule, resultsEntryRole);
        RoleModule resultResultModule = createRoleModule(userId, resultModule, resultsEntryRole);
        RoleModule validationValidationModule = createRoleModule(userId, validationModule, validationRole);

        try {
            sampleTypeCreateService.createAndInsertSampleType(localization, typeOfSample, workplanModule, resultModule,
                    validationModule, workplanResultModule, resultResultModule, validationValidationModule);

            LogEvent.logInfo(this.getClass().getSimpleName(), "postSampleTypeCreate",
                "Sample Type '" + identifyingName + "' successfully saved to database with domain: " + backendDomainCode);

            // Additional verification: Query the database to confirm the domain was saved correctly
            try {
                // Wait a moment for transaction to commit
                Thread.sleep(500);

                // Verify the saved domain by querying recent sample types
                List<TypeOfSample> allTypes = typeOfSampleService.getAllTypeOfSamples();
                TypeOfSample savedType = allTypes.stream()
                    .filter(type -> identifyingName.equals(type.getDescription()))
                    .findFirst()
                    .orElse(null);

                if (savedType != null) {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "postSampleTypeCreate",
                        "DATABASE VERIFICATION: Found saved sample type with domain '" + savedType.getDomain() + "'");
                    if (backendDomainCode.equals(savedType.getDomain())) {
                        LogEvent.logInfo(this.getClass().getSimpleName(), "postSampleTypeCreate",
                            "DOMAIN MATCH: Expected '" + backendDomainCode + "', Got '" + savedType.getDomain() + "' - SUCCESS!");
                    } else {
                        LogEvent.logError(this.getClass().getSimpleName(), "postSampleTypeCreate",
                            "DOMAIN MISMATCH: Expected '" + backendDomainCode + "', Got '" + savedType.getDomain() + "' - ERROR!");
                    }
                } else {
                    LogEvent.logError(this.getClass().getSimpleName(), "postSampleTypeCreate",
                        "DATABASE VERIFICATION: Could not find saved sample type - may not be committed yet");
                }
            } catch (Exception verificationException) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "postSampleTypeCreate",
                    "Verification check failed: " + verificationException.getMessage());
            }

        } catch (LIMSRuntimeException e) {
            LogEvent.logError("Failed to save Sample Type '" + identifyingName + "' to database: " + e.getMessage(), e);
            throw e;
        }
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);

        // return findForward(FWD_SUCCESS_INSERT, form);
        return form;
    }

    private Localization createLocalization(String french, String english, String currentUserId) {
        Localization localization = new Localization();
        localization.setEnglish(english);
        localization.setFrench(french);
        localization.setDescription("type of sample name");
        localization.setSysUserId(currentUserId);
        return localization;
    }

    private RoleModule createRoleModule(String userId, SystemModule workplanModule, Role role) {
        RoleModule roleModule = new RoleModule();
        roleModule.setRole(role);
        roleModule.setSystemModule(workplanModule);
        roleModule.setSysUserId(userId);
        roleModule.setHasAdd("Y");
        roleModule.setHasDelete("Y");
        roleModule.setHasSelect("Y");
        roleModule.setHasUpdate("Y");
        return roleModule;
    }

    private TypeOfSample createTypeOfSample(String identifyingName, String userId, String backendDomainCode) {
        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setDescription(identifyingName);
        typeOfSample.setDomain(backendDomainCode); // Use the already-mapped backend domain code
        typeOfSample.setLocalAbbreviation(
                identifyingName.length() > 10 ? identifyingName.substring(0, 10) : identifyingName);
        typeOfSample.setIsActive(true);
        typeOfSample.setSortOrder(Integer.MAX_VALUE);
        typeOfSample.setSysUserId(userId);
        String identifyingNameKey = identifyingName.replaceAll(" ", "_");
        typeOfSample.setNameKey("Sample.type." + identifyingNameKey);

        LogEvent.logInfo(this.getClass().getSimpleName(), "createTypeOfSample",
            "TypeOfSample created with backend domain code: " + backendDomainCode);

        return typeOfSample;
    }

    /**
     * Maps frontend domain values to backend domain codes.
     * Frontend: CLINICAL, ENVIRONMENTAL, VECTOR
     * Backend: H (Human/Clinical), E (Environmental), V (Vector)
     */
    private String mapFrontendDomainToBackendCode(String frontendDomain) {
        if (frontendDomain == null || frontendDomain.trim().isEmpty()) {
            return "H"; // Default to Human/Clinical
        }

        switch (frontendDomain.toUpperCase()) {
            case "CLINICAL":
                return "H"; // Human/Clinical
            case "ENVIRONMENTAL":
                return "E"; // Environmental
            case "VECTOR":
                return "V"; // Vector surveillance
            default:
                return "H"; // Default to Human/Clinical for unknown domains
        }
    }

    private SystemModule createSystemModule(String menuItem, String identifyingName, String userId) {
        SystemModule module = new SystemModule();
        module.setSystemModuleName(menuItem + ":" + identifyingName);
        module.setDescription(menuItem + "=>" + identifyingName);
        module.setSysUserId(userId);
        module.setHasAddFlag("Y");
        module.setHasDeleteFlag("Y");
        module.setHasSelectFlag("Y");
        module.setHasUpdateFlag("Y");
        return module;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "sampleTypeCreateDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/SampleTypeCreate";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "sampleTypeCreateDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        return null;
    }
}
