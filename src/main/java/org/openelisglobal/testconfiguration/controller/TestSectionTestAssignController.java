package org.openelisglobal.testconfiguration.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.hibernate.HibernateException;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testconfiguration.form.TestSectionTestAssignForm;
import org.openelisglobal.testconfiguration.service.TestSectionTestAssignService;
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
public class TestSectionTestAssignController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "testId", "testSectionId",
            "deactivateTestSectionId" };

    @Autowired
    private TestSectionTestAssignService testSectionTestAssignService;
    @Autowired
    private TestSectionService testSectionService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/TestSectionTestAssign", method = RequestMethod.GET)
    public ModelAndView showTestSectionTestAssign(HttpServletRequest request) {
        TestSectionTestAssignForm form = new TestSectionTestAssignForm();

        setupDisplayItems(form);

        return findForward(FWD_SUCCESS, form);
    }

    private void setupDisplayItems(TestSectionTestAssignForm form) {
        List<IdValuePair> testSections = DisplayListService.getInstance()
                .getListWithLeadingBlank(DisplayListService.ListType.TEST_SECTION_ACTIVE);
        LinkedHashMap<IdValuePair, List<IdValuePair>> testSectionTestsMap = new LinkedHashMap<>(testSections.size());

        for (IdValuePair sectionPair : testSections) {
            List<IdValuePair> tests = new ArrayList<>();
            testSectionTestsMap.put(sectionPair, tests);
            List<Test> testList = testSectionService.getTestsInSection(sectionPair.getId());

            for (Test test : testList) {
                if (test.isActive()) {
                    tests.add(new IdValuePair(test.getId(), TestServiceImpl.getLocalizedTestNameWithType(test)));
                }
            }
        }

        // we can't just append the original list because that list is in the cache
        List<IdValuePair> joinedList = new ArrayList<>(testSections);
        joinedList.addAll(DisplayListService.getInstance().getList(DisplayListService.ListType.TEST_SECTION_INACTIVE));

        form.setTestSectionList(joinedList);
        form.setSectionTestList(testSectionTestsMap);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "testSectionAssignDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/TestSectionTestAssign";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "testSectionAssignDefinition";
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

    @RequestMapping(value = "/TestSectionTestAssign", method = RequestMethod.POST)
    public ModelAndView postTestSectionTestAssign(HttpServletRequest request,
            @ModelAttribute("form") @Valid TestSectionTestAssignForm form, BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            setupDisplayItems(form);
            return findForward(FWD_FAIL_INSERT, form);
        }

        String testId = form.getTestId();
        String testSectionId = form.getTestSectionId();
        String deactivateTestSectionId = form.getDeactivateTestSectionId();
        boolean updateTestSection = false;
        String currentUser = getSysUserId(request);
        Test test = SpringContext.getBean(TestService.class).get(testId);
        TestSection testSection = testSectionService.get(testSectionId);
        TestSection deActivateTestSection = null;
        test.setTestSection(testSection);
        test.setSysUserId(currentUser);

        // This covers the case that they are moving the test to the same test section
        // they are moving it from
        if (testSectionId.equals(deactivateTestSectionId)) {
            return findForward(FWD_SUCCESS_INSERT, form);
        }

        if ("N".equals(testSection.getIsActive())) {
            testSection.setIsActive("Y");
            testSection.setSysUserId(currentUser);
            updateTestSection = true;
        }

        if (!org.apache.commons.validator.GenericValidator.isBlankOrNull(deactivateTestSectionId)) {
            deActivateTestSection = testSectionService.get(deactivateTestSectionId);
            deActivateTestSection.setIsActive("N");
            deActivateTestSection.setSysUserId(currentUser);
        }

        try {
            testSectionTestAssignService.updateTestAndTestSections(test, testSection, deActivateTestSection,
                    updateTestSection);
        } catch (HibernateException e) {
            LogEvent.logDebug(e);
        }

        DisplayListService.getInstance().refreshList(DisplayListService.ListType.TEST_SECTION_ACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.TEST_SECTION_INACTIVE);

        return findForward(FWD_SUCCESS_INSERT, form);
    }
}
