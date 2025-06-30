package org.openelisglobal.testconfiguration.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.openelisglobal.testconfiguration.form.MethodCreateForm;
import org.openelisglobal.testconfiguration.service.MethodCreateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
public class MethodCreateRestController extends BaseController {
    private static final String[] ALLOWED_FIELDS = new String[] { "methodEnglishName", "methodFrenchName" };

    public static final String NAME_SEPARATOR = "$";

    @Autowired
    private MethodService methodService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private MethodCreateService methodCreateService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/MethodCreate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> showMethodCreate(HttpServletRequest request) {
        MethodCreateForm form = new MethodCreateForm();

        setupDisplayMethods(form);

        return ResponseEntity.ok(form);
    }

    private void setupDisplayMethods(MethodCreateForm form) {
        form.setExistingMethodList(DisplayListService.getInstance().getList(DisplayListService.ListType.METHODS));
        form.setExistingMethodList(DisplayListService.getInstance().getList(DisplayListService.ListType.METHODS));
        form.setInactiveMethodList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.METHODS_INACTIVE));

        form.setExistingEnglishNames(getExistingMethodNames(Locale.ENGLISH));

        form.setExistingFrenchNames(getExistingMethodNames(Locale.FRENCH));
    }

    private String getExistingMethodNames(Locale locale) {
        StringBuilder builder = new StringBuilder(NAME_SEPARATOR);
        List<Method> methods = methodService.getAll();

        for (Method method : methods) {
            builder.append(method.getLocalization().getLocalizedValue(locale));

            builder.append(NAME_SEPARATOR);
        }
        return builder.toString();
    }

    @PostMapping(value = "/MethodCreate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postMethodCreate(HttpServletRequest request, @RequestBody @Valid MethodCreateForm form,
            BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            setupDisplayMethods(form);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(form);
        }

        String identifyingName = form.getMethodEnglishName();
        String userId = getSysUserId(request);

        Localization localization = createLocalization(form.getMethodFrenchName(), identifyingName, userId);

        Method method = createMethod(identifyingName, userId);

        SystemModule workplanModule = createSystemModule("Workplan", identifyingName, userId);
        SystemModule resultModule = createSystemModule("LogbookResults", identifyingName, userId);
        SystemModule validationModule = createSystemModule("ResultValidation", identifyingName, userId);

        Role resultsEntryRole = roleService.getRoleByName(Constants.ROLE_RESULTS);
        Role validationRole = roleService.getRoleByName(Constants.ROLE_VALIDATION);

        RoleModule workplanResultModule = createRoleModule(userId, workplanModule, resultsEntryRole);
        RoleModule resultResultModule = createRoleModule(userId, resultModule, resultsEntryRole);
        RoleModule validationValidationModule = createRoleModule(userId, validationModule, validationRole);

        try {
            methodCreateService.insertMethod(localization, method, workplanModule, resultModule, validationModule,
                    workplanResultModule, resultResultModule, validationValidationModule);
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
        }

        DisplayListService.getInstance().refreshList(DisplayListService.ListType.METHODS);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.METHODS_INACTIVE);

        return ResponseEntity.ok(form);
    }

    private Localization createLocalization(String french, String english, String currentUserId) {
        Localization localization = new Localization();
        localization.setEnglish(english);
        localization.setFrench(french);
        localization.setDescription("method name");
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

    private Method createMethod(String identifyingName, String userId) {
        Method method = new Method();
        method.setDescription(identifyingName);
        method.setMethodName(identifyingName);
        method.setIsActive("N");
        String identifyingNameKey = identifyingName.replaceAll(" ", "_");
        method.setNameKey("method." + identifyingNameKey);

        method.setSysUserId(userId);
        return method;
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
            return "methodCreateDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/MethodCreate";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "methodCreateDefinition";
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
