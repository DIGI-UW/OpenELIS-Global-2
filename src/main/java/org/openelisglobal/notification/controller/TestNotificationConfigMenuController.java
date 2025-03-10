package org.openelisglobal.notification.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.controller.BaseMenuController;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.notification.form.TestNotificationConfigMenuForm;
import org.openelisglobal.notification.service.TestNotificationConfigService;
import org.openelisglobal.notification.valueholder.TestNotificationConfig;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TestNotificationConfigMenuController extends BaseMenuController<TestNotificationConfig> {

    private static final String[] ALLOWED_FIELDS = new String[] { "menuList*.id", "menuList*.test.id",
            "menuList*.providerSMS.active", "menuList*.providerEmail.active", "menuList*.patientEmail.active",
            "menuList*.patientSMS.active", "menuList*.defaultPayloadTemplate.id" };

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @Autowired
    private TestService testService;
    @Autowired
    private TestNotificationConfigService testNotificationConfigService;

    @Override
    protected int getPageSize() {
        return -1;
    }

    @GetMapping("/TestNotificationConfigMenu")
    public ModelAndView displayNotificationConfig()
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        TestNotificationConfigMenuForm form = new TestNotificationConfigMenuForm();
        request.setAttribute("menuDefinition", "TestNotificationMenuDefinition");
        String forward = performMenuAction(form, request);
        return findForward(forward, form);
    }

    @PostMapping("/TestNotificationConfigMenu")
    public ModelAndView updateNotificationConfig(HttpServletRequest request,
            @ModelAttribute("form") @Valid TestNotificationConfigMenuForm form, BindingResult result,
            RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (result.hasErrors()) {
            saveErrors(result);
            return displayNotificationConfig();
        }
        try {
            testNotificationConfigService.saveTestNotificationConfigsActiveStatuses(form.getMenuList(),
                    this.getSysUserId(request));
        } catch (RuntimeException e) {
            LogEvent.logError("could not save result notification configs", e);
            Errors errors = new BaseErrors();
            errors.reject("alert.error", "An error occured while saving");
            saveErrors(errors);
            return displayNotificationConfig();
        }

        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        return findForward(FWD_SUCCESS_INSERT, form);
    }

    @Override
    protected List<TestNotificationConfig> createMenuList(AdminOptionMenuForm<TestNotificationConfig> form,
            HttpServletRequest request) {
        List<Test> allOrderableTests = testService.getAllActiveOrderableTests();

        List<TestNotificationConfig> testNotificationConfigs = new ArrayList<>();
        for (Test test : allOrderableTests) {
            TestNotificationConfig testNotificationConfig = testNotificationConfigService
                    .getTestNotificationConfigForTestId(test.getId()).orElse(new TestNotificationConfig());
            testNotificationConfig.setTest(test);
            testNotificationConfigs.add(testNotificationConfig);
        }
        return testNotificationConfigs;
    }

    @Override
    protected String getDeactivateDisabled() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "testNotificationMasterListsPageDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/MasterListsPage";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/TestNotificationConfigMenu";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "haitiMasterListsPageDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        // TODO Auto-generated method stub
        return null;
    }
}
