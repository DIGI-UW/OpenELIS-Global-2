package org.openelisglobal.testconfiguration.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import javax.validation.Valid;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testconfiguration.form.TestSectionEditForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
@PreAuthorize("hasRole('ADMIN')")
public class TestSectionEditRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "testSectionId", "domain" };

    @Autowired
    private TestSectionService testSectionService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/TestSectionEdit")
    public TestSectionEditForm getTestSectionEdit(HttpServletRequest request) {
        TestSectionEditForm form = new TestSectionEditForm();
        List<IdValuePair> sections = DisplayListService.getInstance()
                .getList(DisplayListService.ListType.TEST_SECTION_ACTIVE);
        form.setTestSectionList(sections);
        return form;
    }

    @GetMapping(value = "/TestSectionEdit/section")
    public ResponseEntity<TestSectionEditForm> getTestSection(@RequestParam String testSectionId) {
        TestSection testSection = testSectionService.getTestSectionById(testSectionId);
        if (testSection == null) {
            return ResponseEntity.notFound().build();
        }

        TestSectionEditForm form = new TestSectionEditForm();
        form.setTestSectionId(testSectionId);
        form.setDomain(testSection.getDomain());

        if (testSection.getLocalization() != null) {
            form.setNameEnglish(testSection.getLocalization().getLocalizedValue(Locale.ENGLISH));
            form.setNameFrench(testSection.getLocalization().getLocalizedValue(Locale.FRENCH));
        }

        return ResponseEntity.ok(form);
    }

    @PostMapping(value = "/TestSectionEdit")
    public ResponseEntity<TestSectionEditForm> updateTestSectionDomain(HttpServletRequest request,
            @RequestBody @Valid TestSectionEditForm form, BindingResult result) {

        if (result.hasErrors()) {
            saveErrors(result);
            return ResponseEntity.badRequest().body(form);
        }

        TestSection testSection = testSectionService.getTestSectionById(form.getTestSectionId());
        if (testSection == null) {
            return ResponseEntity.notFound().build();
        }

        String previousDomain = testSection.getDomain();
        testSection.setDomain(form.getDomain());
        testSection.setSysUserId(getSysUserId(request));

        try {
            testSectionService.update(testSection);
            LogEvent.logInfo(this.getClass().getName(), "updateTestSectionDomain",
                    "Lab unit " + testSection.getTestSectionName() + " domain changed from " + previousDomain + " to "
                            + form.getDomain() + " by " + getSysUserId(request));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.internalServerError().body(form);
        }

        DisplayListService.getInstance().refreshList(DisplayListService.ListType.TEST_SECTION_ACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.TEST_SECTION_INACTIVE);

        return ResponseEntity.ok(form);
    }

    @Override
    protected String findLocalForward(String forward) {
        return "PageNotFound";
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
