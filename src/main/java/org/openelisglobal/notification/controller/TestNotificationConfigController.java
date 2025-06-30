package org.openelisglobal.notification.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.notification.form.TestNotificationConfigForm;
import org.openelisglobal.notification.service.NotificationPayloadTemplateService;
import org.openelisglobal.notification.service.TestNotificationConfigService;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;
import org.openelisglobal.notification.valueholder.TestNotificationConfig;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TestNotificationConfigController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "config*", "editSystemDefaultPayloadTemplate",
            "systemDefaultPayloadTemplate*" };

    @Autowired
    private TestNotificationConfigService testNotificationConfigService;

    @Autowired
    private NotificationPayloadTemplateService payloadTemplateService;
    @Autowired
    private TestService testService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping("/TestNotificationConfig")
    public ModelAndView displayNotificationConfig(@RequestParam(name = "testId", required = false) String testId) {
        TestNotificationConfigForm form = new TestNotificationConfigForm();

        form.setFormName("TestNotificationConfigForm");
        form.setSystemDefaultPayloadTemplate(
                payloadTemplateService.getSystemDefaultPayloadTemplateForType(NotificationPayloadType.TEST_RESULT));
        form.setConfig(testNotificationConfigService.getTestNotificationConfigForTestId(testId)
                .orElse(new TestNotificationConfig()));
        if (form.getConfig().getTest() == null || GenericValidator.isBlankOrNull(form.getConfig().getTest().getId())) {
            form.getConfig().setTest(testService.get(testId));
        }
        return findForward(FWD_SUCCESS, form);
    }

    @GetMapping(value = "/TestNotificationConfig/raw/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<TestNotificationConfig> getNotificationConfigs(
            @RequestParam(name = "testIds", required = false) List<String> testIds) {
        List<TestNotificationConfig> configs = testNotificationConfigService
                .getTestNotificationConfigsForTestId(testIds);
        return configs;
    }

    @PostMapping("/TestNotificationConfig")
    public ModelAndView updateNotificationConfig(HttpServletRequest request,
            @ModelAttribute("form") @Valid TestNotificationConfigForm form, BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            saveErrors(result);
            return displayNotificationConfig(form.getConfig().getTest().getId());
        }
        String sysUserId = this.getSysUserId(request);

        testNotificationConfigService.saveStatusAndMessages(form.getConfig(), sysUserId);
        if (form.getEditSystemDefaultPayloadTemplate()) {
            payloadTemplateService.updatePayloadTemplateMessagesAndSubject(form.getSystemDefaultPayloadTemplate(),
                    sysUserId);
        }
        testNotificationConfigService.removeEmptyPayloadTemplates(form.getConfig(), sysUserId);
        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        return findForward(FWD_SUCCESS_INSERT, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "testNotificationConfigDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/TestNotificationConfigMenu";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/TestNotificationConfigMenu";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "testNotificationConfigDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "testnotificationconfig.browse.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "testnotificationconfig.browse.title";
    }
}
