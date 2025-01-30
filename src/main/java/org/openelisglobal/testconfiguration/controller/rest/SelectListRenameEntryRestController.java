package org.openelisglobal.testconfiguration.controller.rest;

import javax.servlet.http.HttpServletRequest;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.testconfiguration.form.ResultSelectListRenameForm;
import org.openelisglobal.testconfiguration.service.ResultSelectListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
@RequestMapping("/rest")
public class SelectListRenameEntryRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "nameEnglish", "nameFrench", "resultSelectOptionId" };

    @Autowired
    private ResultSelectListService resultSelectListService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/SelectListRenameEntry")
    public ResultSelectListRenameForm showUomRenameEntry(HttpServletRequest request) {
        ResultSelectListRenameForm form = new ResultSelectListRenameForm();

        form.setResultSelectOptionList(resultSelectListService.getAllSelectListOptions());
        // return findForward(FWD_SUCCESS, form);
        return form;
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

    @PostMapping(value = "/SelectListRenameEntry")
    public ResultSelectListRenameForm updateUomRenameEntry(HttpServletRequest request,
            @RequestBody ResultSelectListRenameForm form, RedirectAttributes redirectAttributes) {

        boolean renamed = resultSelectListService.renameOption(form, getSysUserId(request));
        DisplayListService.getInstance().refreshList(ListType.DICTIONARY_TEST_RESULTS);

        if (renamed) {
            redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
            form.setResultSelectOptionList(resultSelectListService.getAllSelectListOptions());
            // return findForward(FWD_SUCCESS_INSERT, form);
            return form;
        } else {
            Errors errors = new BaseErrors();
            errors.reject(MessageUtil.getMessage("alert.error"));
            saveErrors(errors);
            form.setResultSelectOptionList(resultSelectListService.getAllSelectListOptions());

            // return findForward(FWD_FAIL_INSERT, form);
            return form;
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
