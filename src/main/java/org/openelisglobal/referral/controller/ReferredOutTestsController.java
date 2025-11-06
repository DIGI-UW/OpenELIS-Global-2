package org.openelisglobal.referral.controller;

import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.referral.form.ReferredOutTestsForm;
import org.openelisglobal.referral.service.ReferralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ReferredOutTestsController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "labNumber", "testIds", "testUnitIds", "endDate",
            "startDate", "dateType", "searchType", "selPatient" };

    @Autowired
    private ReferralService referralService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/ReferredOutTests", method = RequestMethod.GET)
    public ModelAndView showReferredOutTests(@Valid @ModelAttribute("form") ReferredOutTestsForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        request.getSession().setAttribute(SAVE_DISABLED, TRUE);

        setupPageForDisplay(form);

        addFlashMsgsToRequest(request);
        return findForward(FWD_SUCCESS, form);
    }

    private void setupPageForDisplay(ReferredOutTestsForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (form.getSearchType() != null) {
            form.setReferralDisplayItems(referralService.getReferralItems(form));
            form.setSearchFinished(true);
        }
        form.setTestSelectionList(DisplayListService.getInstance().getList(ListType.ALL_TESTS));
        form.setTestUnitSelectionList(DisplayListService.getInstance().getList(ListType.TEST_SECTION_BY_NAME));
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "referredOutDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "homePageDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/ReferredOutTests";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "referredOutDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageSubtitleKey() {
        return "referral.out.manage";
    }

    @Override
    protected String getPageTitleKey() {
        return "referral.out.manage";
    }

    public class NonNumericTests {
        public String testId;
        public String testType;
        public List<IdValuePair> dictionaryValues;
    }
}
