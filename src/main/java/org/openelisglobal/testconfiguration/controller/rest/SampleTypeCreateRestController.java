package org.openelisglobal.testconfiguration.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.validation.Valid;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.domain.Domain;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private static final String[] ALLOWED_FIELDS = new String[] { "sampleTypeEnglishName", "sampleTypeFrenchName",
            "domain", "whonetCode", "active" };

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
    public ResponseEntity<?> postSampleTypeCreate(HttpServletRequest request,
            @RequestBody @Valid SampleTypeCreateForm form, BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            setupDisplayItems(form);
            return ResponseEntity.ok(form);
        }
        String identifyingName = form.getSampleTypeEnglishName();
        String userId = getSysUserId(request);
        String backendDomainCode = mapFrontendDomainToBackendCode(form.getDomain());

        Localization localization = createLocalization(form.getSampleTypeFrenchName(), identifyingName, userId);

        TypeOfSample typeOfSample = createTypeOfSample(identifyingName, userId, backendDomainCode, form.getWhonetCode(),
                Boolean.TRUE.equals(form.getActive()));

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
        } catch (LIMSDuplicateRecordException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "postSampleTypeCreate", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(buildDuplicateBody(typeOfSample, identifyingName, backendDomainCode));
        } catch (LIMSRuntimeException e) {
            LogEvent.logError("Failed to save Sample Type '" + identifyingName + "' to database: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "insertFailed", "message", "The sample type could not be created."));
        }
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);

        return ResponseEntity.ok(form);
    }

    /**
     * The DAO's uniqueness check triggers if EITHER (description, domain) OR
     * (localAbbrev, domain) already exists. When the auto-derived abbreviation
     * (first 10 chars of the name) is the culprit, the admin's only path forward is
     * a name with a different beginning, so name that specifically instead of a
     * generic "duplicate."
     */
    private Map<String, Object> buildDuplicateBody(TypeOfSample typeOfSample, String identifyingName,
            String backendDomainCode) {
        TypeOfSample descriptionProbe = new TypeOfSample();
        descriptionProbe.setDescription(identifyingName);
        descriptionProbe.setDomain(backendDomainCode);
        TypeOfSample nameClash = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(descriptionProbe, true);
        if (nameClash != null) {
            return Map.of("error", "duplicate", "field", "name", "message",
                    "A sample type named '" + identifyingName + "' already exists in this domain.");
        }
        String abbrev = typeOfSample.getLocalAbbreviation();
        TypeOfSample abbrevClash = typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(abbrev, backendDomainCode);
        if (abbrevClash != null) {
            return Map.of("error", "duplicate", "field", "name", "message",
                    "This name shares its first 10 characters ('" + abbrev
                            + "') with an existing sample type in this domain. Pick a name with a different start.");
        }
        return Map.of("error", "duplicate", "field", "name", "message", "A sample type with this name already exists.");
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

    private TypeOfSample createTypeOfSample(String identifyingName, String userId, String backendDomainCode,
            String whonetCode, boolean active) {
        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setDescription(identifyingName);
        typeOfSample.setDomain(backendDomainCode); // Use the already-mapped backend domain code
        typeOfSample.setLocalAbbreviation(
                identifyingName.length() > 10 ? identifyingName.substring(0, 10) : identifyingName);
        if (whonetCode != null) {
            String trimmed = whonetCode.trim();
            if (!trimmed.isEmpty() && trimmed.length() <= 5) {
                typeOfSample.setWhonetCode(trimmed);
            }
        }
        // Inactive-until-configured by default; the admin may create the type
        // active explicitly (so it is immediately orderable).
        typeOfSample.setIsActive(active);
        typeOfSample.setSortOrder(Integer.MAX_VALUE);
        typeOfSample.setSysUserId(userId);
        String identifyingNameKey = identifyingName.replaceAll(" ", "_");
        typeOfSample.setNameKey("Sample.type." + identifyingNameKey);

        return typeOfSample;
    }

    /**
     * Canonicalizes the Clinical/Environmental/Vector choice; the column stores the
     * enum value since the OGC-296 Dependency-4 migration.
     */
    private String mapFrontendDomainToBackendCode(String frontendDomain) {
        return Domain.normalize(frontendDomain);
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
