package org.openelisglobal.samplebatchentry.controller.rest;

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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class SampleBatchEntrySetupRestController extends BaseSampleEntryController {

    @Autowired
    SiteInformationService siteInformationService;

    @GetMapping(value = "/SampleBatchEntrySetup", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public SampleBatchEntryForm showSampleBatchEntrySetup(HttpServletRequest request)
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

        return form;
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
