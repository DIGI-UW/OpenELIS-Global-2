package org.openelisglobal.referral.controller.rest;

import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.referral.form.ReferredOutTestsForm;
import org.openelisglobal.referral.service.ReferralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/")
public class ReferredOutTestsRestController {

    private static final String[] ALLOWED_FIELDS = new String[] { "labNumber", "testIds", "testUnitIds", "endDate",
            "startDate", "dateType", "searchType", "selPatient" };

    @Autowired
    private ReferralService referralService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "ReferredOutTests")
    public ReferredOutTestsForm showReferredOutTests(@Valid ReferredOutTestsForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        setupPageForDisplay(form);
        return form;
    }

    private void setupPageForDisplay(ReferredOutTestsForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (form.getSearchType() != null) {
            form.setReferralDisplayItems(referralService.getReferralItems(form));
            form.setSearchFinished(true);
        }
        form.setTestSelectionList(DisplayListService.getInstance().getList(DisplayListService.ListType.ALL_TESTS));
        form.setTestUnitSelectionList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.TEST_SECTION_BY_NAME));
    }

    public class NonNumericTests {
        public String testId;
        public String testType;
        public List<IdValuePair> dictionaryValues;
    }
}
