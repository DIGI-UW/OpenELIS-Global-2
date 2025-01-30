package org.openelisglobal.testconfiguration.controller.rest;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testconfiguration.form.TestRenameEntryForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
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
public class TestRenameEntryRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "testId", "nameEnglish", "nameFrench",
            "reportNameEnglish", "reportNameFrench" };

    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private TestService testService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/TestRenameEntry")
    public TestRenameEntryForm showTestRenameEntry(HttpServletRequest request) {
        LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
                "Hibernate Version: " + org.hibernate.Version.getVersionString());
        // String forward = FWD_SUCCESS;
        TestRenameEntryForm form = new TestRenameEntryForm();

        form.setTestList(DisplayListService.getInstance().getList(DisplayListService.ListType.ALL_TESTS));

        // return findForward(forward, form);
        return form;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "testRenameDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/TestRenameEntry";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "testRenameDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @PostMapping(value = "/TestRenameEntry")
    public TestRenameEntryForm updateTestRenameEntry(HttpServletRequest request,
            @RequestBody @Valid TestRenameEntryForm form, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            // saveErrors(result);
            // return findForward(FWD_FAIL_INSERT, form);
            return form;
        }

        form.setCancelAction("CancelDictionary");

        String testId = form.getTestId();
        String nameEnglish = form.getNameEnglish();
        String nameFrench = form.getNameFrench();
        String reportNameEnglish = form.getReportNameEnglish();
        String reportNameFrench = form.getReportNameFrench();
        String userId = getSysUserId(request);

        updateTestNames(testId, nameEnglish, nameFrench, reportNameEnglish, reportNameFrench, userId);

        // return findForward(FWD_SUCCESS_INSERT, form);
        return form;
    }

    private void updateTestNames(String testId, String nameEnglish, String nameFrench, String reportNameEnglish,
            String reportNameFrench, String userId) {
        Test test = testService.get(testId);

        if (test != null) {

            Localization name = test.getLocalizedTestName();
            Localization reportingName = test.getLocalizedReportingName();
            name.setEnglish(nameEnglish.trim());
            name.setFrench(nameFrench.trim());
            name.setSysUserId(userId);
            reportingName.setEnglish(reportNameEnglish.trim());
            reportingName.setFrench(reportNameFrench.trim());
            reportingName.setSysUserId(userId);

            try {
                localizationService.updateTestNames(name, reportingName);
            } catch (LIMSRuntimeException e) {
                LogEvent.logError(e);
            }
        }

        // Refresh test names
        DisplayListService.getInstance().getFreshList(DisplayListService.ListType.ALL_TESTS);
        DisplayListService.getInstance().getFreshList(DisplayListService.ListType.ORDERABLE_TESTS);
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
