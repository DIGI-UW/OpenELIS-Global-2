package org.openelisglobal.testconfiguration.controller.rest;

import javax.servlet.http.HttpServletRequest;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testconfiguration.form.ResultSelectListForm;
import org.openelisglobal.testconfiguration.service.ResultSelectListService;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ResultSelectListAddRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "nameEnglish", "nameFrench", "testSelectListJson" };

    @Autowired
    private TestService testService;
    @Autowired
    private ResultSelectListService resultSelectListService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/ResultSelectListAdd")
    public ResultSelectListForm showCreateResultSelectList(HttpServletRequest request) {
        ResultSelectListForm form = new ResultSelectListForm();
        form.setPage("1");
        addFlashMsgsToRequest(request);
        // return findForward(FWD_SUCCESS, form);
        return form;
    }

    @PostMapping(value = "/ResultSelectListAdd")
    public ResultSelectListForm showResultSelectListAddToTest(HttpServletRequest request,
            @RequestBody ResultSelectListForm form) {
        form.setPage("2");
        if ("".equalsIgnoreCase(form.getNameEnglish())) {
            form.setNameEnglish(form.getNameFrench());
        } else if ("".equalsIgnoreCase(form.getNameFrench())) {
            form.setNameFrench(form.getNameEnglish());
        }
        form.setTests(testService.getAllTestsByDictionaryResult());
        form.setTestDictionary(resultSelectListService.getTestSelectDictionary());
        addFlashMsgsToRequest(request);
        // return findForward(FWD_SUCCESS, form);
        return form;
    }

    @PostMapping(value = "/SaveResultSelectList")
    public ResultSelectListForm SaveResultSelectList(HttpServletRequest request, @RequestBody ResultSelectListForm form,
            RedirectAttributes redirectAttributes) {

        addFlashMsgsToRequest(request);
        String currentUserId = getSysUserId(request);
        boolean saved = resultSelectListService.addResultSelectList(form, currentUserId);
        if (saved) {
            redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
            DisplayListService.getInstance().refreshList(ListType.DICTIONARY_TEST_RESULTS);
            // return findForward(FWD_SUCCESS_INSERT, form);
            return form;
        }
        // return findForward(FWD_FAIL_INSERT, form);
        return form;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "resultSelectListDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/ResultSelectListAdd";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "resultSelectListDefinition";
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
