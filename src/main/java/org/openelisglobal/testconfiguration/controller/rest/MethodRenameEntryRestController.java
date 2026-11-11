package org.openelisglobal.testconfiguration.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.validation.Valid;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.testconfiguration.form.MethodRenameEntryForm;
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
public class MethodRenameEntryRestController extends BaseController {
    private static final String[] ALLOWED_FIELDS = new String[] { "methodId", "nameEnglish", "nameFrench" };

    @Autowired
    LocalizationService localizationService;
    @Autowired
    MethodService methodService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/MethodRenameEntry")
    public MethodRenameEntryForm showMethodRenameEntry(HttpServletRequest request) {
        MethodRenameEntryForm form = new MethodRenameEntryForm();

        form.setMethodList(DisplayListService.getInstance().getList(DisplayListService.ListType.METHODS));

        // return findForward(FWD_SUCCESS, form);
        return form;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "methodRenameDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/MethodRenameEntry";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "methodRenameDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @PostMapping(value = "/MethodRenameEntry")
    public ResponseEntity<?> updateMethodRenameEntry(HttpServletRequest request,
            @RequestBody @Valid MethodRenameEntryForm form, BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            form.setMethodList(DisplayListService.getInstance().getList(DisplayListService.ListType.METHODS));
            // return findForward(FWD_FAIL_INSERT, form);
            return validationRefusal(result);
        }

        String methodId = form.getMethodId();
        String nameEnglish = form.getNameEnglish().trim();
        String nameFrench = form.getNameFrench().trim();

        Method method;
        try {
            method = methodService.get(methodId);
        } catch (ObjectNotFoundException e) {
            return refusal(HttpStatus.NOT_FOUND, "notFound", "No method with this id.");
        }
        if (nameTakenByAnotherMethod(methodId, nameEnglish, nameFrench)) {
            return refusal(HttpStatus.CONFLICT, "duplicate", "A method with this name already exists.");
        }

        Localization name = method.getLocalization();
        name.setEnglish(nameEnglish);
        name.setFrench(nameFrench);
        name.setSysUserId(getSysUserId(request));
        try {
            localizationService.update(name);
        } catch (LIMSRuntimeException | HibernateException e) {
            return saveFailure(e);
        }

        DisplayListService.getInstance().getFreshList(DisplayListService.ListType.METHODS);
        DisplayListService.getInstance().getFreshList(DisplayListService.ListType.METHODS_INACTIVE);
        return ResponseEntity.ok(form);
    }

    /**
     * A rename may not give a method a name another method already shows, in either
     * language; method names are compared trimmed and case-insensitively.
     */
    private boolean nameTakenByAnotherMethod(String methodId, String nameEnglish, String nameFrench) {
        for (Method other : methodService.getAll()) {
            if (other.getId().equals(methodId)) {
                continue;
            }
            Localization otherName = other.getLocalization();
            if (sameName(other.getMethodName(), nameEnglish)
                    || (otherName != null && (sameName(otherName.getLocalizedValue(Locale.ENGLISH), nameEnglish)
                            || sameName(otherName.getLocalizedValue(Locale.FRENCH), nameFrench)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameName(String existing, String candidate) {
        return existing != null && existing.trim().equalsIgnoreCase(candidate);
    }

    private static ResponseEntity<Map<String, Object>> refusal(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
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
