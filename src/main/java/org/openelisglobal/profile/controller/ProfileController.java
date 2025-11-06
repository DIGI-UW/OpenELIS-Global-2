package org.openelisglobal.profile.controller;

import java.io.IOException;
import java.util.Arrays;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.profile.form.ProfileForm;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "file" };

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @Autowired
    private TestService testService;
    @Autowired
    private OrganizationService organizationService;

    private static final String TEST_PROFILE_SUCCESS = "testSuccess";
    private static final String ORGANIZATION_PROFILE_SUCCESS = "organizationSuccess";

    @GetMapping("/TestProfile")
    public ModelAndView viewTestPage() {
        ProfileForm form = new ProfileForm();
        form.setFormAction("TestProfile");
        return findForward(FWD_SUCCESS, form);
    }

    @PostMapping("/TestProfile")
    public ModelAndView applyTestProfile(@ModelAttribute("form") ProfileForm form,
            RedirectAttributes redirectAttributes) throws IOException {
        String testNamesString = new String(form.getFile().getBytes());
        String[] testNames = testNamesString.split("\\R|,");
        testService.activateTestsAndDeactivateOthers(Arrays.asList(testNames));

        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        return findForward(TEST_PROFILE_SUCCESS, form);
    }

    @GetMapping("/OrganizationProfile")
    public ModelAndView viewOrganizationPage() {
        ProfileForm form = new ProfileForm();
        form.setFormAction("OrganizationProfile");
        return findForward(FWD_SUCCESS, form);
    }

    @PostMapping("/OrganizationProfile")
    public ModelAndView applyOrganizationProfile(@ModelAttribute("form") ProfileForm form,
            RedirectAttributes redirectAttributes) throws IOException {
        String organizationNamesString = new String(form.getFile().getBytes());
        String[] organizationNames = organizationNamesString.split("\\R|,");

        organizationService.activateOrganizationsAndDeactivateOthers(Arrays.asList(organizationNames));

        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        return findForward(ORGANIZATION_PROFILE_SUCCESS, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        switch (forward) {
        case FWD_SUCCESS:
            return "profileDefinition";
        case TEST_PROFILE_SUCCESS:
            return "redirect:/TestProfile";
        case ORGANIZATION_PROFILE_SUCCESS:
            return "redirect:/OrganizationProfile";
        }
        return "PageNotFound";
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
