package org.openelisglobal.testconfiguration.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.testconfiguration.form.ResultSelectListRenameForm;
import org.openelisglobal.testconfiguration.service.ResultSelectListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SelectListRenameEntryController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "nameEnglish", "nameFrench", "resultSelectOptionId" };

    @Autowired
    private ResultSelectListService resultSelectListService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/SelectListRenameEntry", method = RequestMethod.GET)
    public ModelAndView showUomRenameEntry(HttpServletRequest request) {
        ResultSelectListRenameForm form = new ResultSelectListRenameForm();

        form.setResultSelectOptionList(resultSelectListService.getAllSelectListOptions());
        return findForward(FWD_SUCCESS, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "resultSelectListRenameDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/SelectListRenameEntry";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "resultSelectListRenameDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @RequestMapping(value = "/SelectListRenameEntry", method = RequestMethod.POST)
    public ModelAndView updateUomRenameEntry(HttpServletRequest request,
            @ModelAttribute("form") ResultSelectListRenameForm form, RedirectAttributes redirectAttributes) {

        boolean renamed = resultSelectListService.renameOption(form, getSysUserId(request));
        DisplayListService.getInstance().refreshList(ListType.DICTIONARY_TEST_RESULTS);

        if (renamed) {
            redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
            form.setResultSelectOptionList(resultSelectListService.getAllSelectListOptions());
            return findForward(FWD_SUCCESS_INSERT, form);
        } else {
            Errors errors = new BaseErrors();
            errors.reject(MessageUtil.getMessage("alert.error"));
            saveErrors(errors);
            form.setResultSelectOptionList(resultSelectListService.getAllSelectListOptions());

            return findForward(FWD_FAIL_INSERT, form);
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
