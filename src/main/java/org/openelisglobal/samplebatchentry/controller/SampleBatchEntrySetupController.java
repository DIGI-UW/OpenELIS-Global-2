package org.openelisglobal.samplebatchentry.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.SampleOrderService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.sample.controller.BaseSampleEntryController;
import org.openelisglobal.sample.form.ProjectData;
import org.openelisglobal.samplebatchentry.form.SampleBatchEntryForm;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class SampleBatchEntrySetupController extends BaseSampleEntryController {

    private static final String[] ALLOWED_FIELDS = new String[] {};

    @Autowired
    SiteInformationService siteInformationService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/SampleBatchEntrySetup", method = RequestMethod.GET)
    public ModelAndView showSampleBatchEntrySetup(HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        SampleBatchEntryForm form = new SampleBatchEntryForm();

        request.getSession().setAttribute(NEXT_DISABLED, TRUE);

        SampleOrderService sampleOrderService = new SampleOrderService();
        form.setSampleOrderItems(sampleOrderService.getSampleOrderItem());
        form.setSampleTypes(DisplayListService.getInstance().getList(ListType.SAMPLE_TYPE_ACTIVE));
        form.setTestSectionList(DisplayListService.getInstance().getList(ListType.TEST_SECTION_ACTIVE));
        form.setCurrentDate(DateUtil.getCurrentDateAsText());
        form.setCurrentTime(DateUtil.getCurrentTimeAsText());
        form.getSampleOrderItems().setReceivedTime(DateUtil.getCurrentTimeAsText());
        form.getSampleOrderItems().setReceivedDateForDisplay(DateUtil.getCurrentDateAsText());
        form.setProjectDataVL(new ProjectData());
        form.setProjectDataEID(new ProjectData());

        SiteInformation siteInfoTab = siteInformationService.getSiteInformationByName("Study Management tab");
        String siteInfo = siteInfoTab != null ? siteInfoTab.getValue() : "true";
        request.getSession().setAttribute("siteInfo", siteInfo);

        addProjectList(form);

        if (FormFields.getInstance().useField(FormFields.Field.InitialSampleCondition)) {
            form.setInitialSampleConditionList(
                    DisplayListService.getInstance().getList(ListType.INITIAL_SAMPLE_CONDITION));
        }
        if (FormFields.getInstance().useField(FormFields.Field.SampleNature)) {
            form.setSampleNatureList(DisplayListService.getInstance().getList(ListType.SAMPLE_NATURE));
        }

        return findForward(FWD_SUCCESS, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "sampleBatchEntrySetupDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "homePageDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "sample.batchentry.setup.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "sample.batchentry.setup.title";
    }
}
