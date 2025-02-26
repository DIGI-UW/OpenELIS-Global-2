package org.openelisglobal.testconfiguration.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.hibernate.HibernateException;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.testconfiguration.form.MethodRenameEntryForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class MethodRenameEntryController extends BaseController {
    private static final String[] ALLOWED_FIELDS = new String[] { "methodId", "nameEnglish", "nameFrench" };

    @Autowired
    LocalizationService localizationService;
    @Autowired
    MethodService methodService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/MethodRenameEntry", method = RequestMethod.GET)
    public ModelAndView showMethodRenameEntry(HttpServletRequest request) {
        MethodRenameEntryForm form = new MethodRenameEntryForm();

        form.setMethodList(DisplayListService.getInstance().getList(DisplayListService.ListType.METHODS));

        return findForward(FWD_SUCCESS, form);
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

    @RequestMapping(value = "/MethodRenameEntry", method = RequestMethod.POST)
    public ModelAndView updateMethodRenameEntry(HttpServletRequest request,
            @ModelAttribute("form") @Valid MethodRenameEntryForm form, BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            form.setMethodList(DisplayListService.getInstance().getList(DisplayListService.ListType.METHODS));
            return findForward(FWD_FAIL_INSERT, form);
        }

        String methodId = form.getMethodId();
        String nameEnglish = form.getNameEnglish();
        String nameFrench = form.getNameFrench();
        String userId = getSysUserId(request);

        updateMethodNames(methodId, nameEnglish, nameFrench, userId);

        return findForward(FWD_SUCCESS_INSERT, form);
    }

    private void updateMethodNames(String methodId, String nameEnglish, String nameFrench, String userId) {
        Method method = methodService.get(methodId);

        if (method != null) {

            Localization name = method.getLocalization();
            name.setEnglish(nameEnglish.trim());
            name.setFrench(nameFrench.trim());
            name.setSysUserId(userId);

            try {
                localizationService.update(name);
            } catch (HibernateException e) {
                LogEvent.logDebug(e);
            }
        }

        // Refresh method names
        DisplayListService.getInstance().getFreshList(DisplayListService.ListType.METHODS);
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
