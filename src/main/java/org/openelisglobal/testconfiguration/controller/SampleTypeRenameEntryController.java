package org.openelisglobal.testconfiguration.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.hibernate.HibernateException;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.testconfiguration.form.SampleTypeRenameEntryForm;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
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
public class SampleTypeRenameEntryController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "sampleTypeId", "nameEnglish", "nameFrench" };

    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private LocalizationService localizationService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/SampleTypeRenameEntry", method = RequestMethod.GET)
    public ModelAndView showSampleTypeRenameEntry(HttpServletRequest request) {
        SampleTypeRenameEntryForm form = new SampleTypeRenameEntryForm();

        form.setSampleTypeList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE));

        return findForward(FWD_SUCCESS, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "sampleTypeRenameDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/SampleTypeRenameEntry";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "sampleTypeRenameDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @RequestMapping(value = "/SampleTypeRenameEntry", method = RequestMethod.POST)
    public ModelAndView updateSampleTypeRenameEntry(HttpServletRequest request,
            @ModelAttribute("form") @Valid SampleTypeRenameEntryForm form, BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            form.setSampleTypeList(
                    DisplayListService.getInstance().getList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE));
            return findForward(FWD_FAIL_INSERT, form);
        }

        String sampleTypeId = form.getSampleTypeId();
        String nameEnglish = form.getNameEnglish();
        String nameFrench = form.getNameFrench();
        String userId = getSysUserId(request);

        updateSampleTypeNames(sampleTypeId, nameEnglish, nameFrench, userId);

        form.setSampleTypeList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE));

        return findForward(FWD_SUCCESS_INSERT, form);
    }

    private void updateSampleTypeNames(String sampleTypeId, String nameEnglish, String nameFrench, String userId) {
        TypeOfSample typeOfSample = typeOfSampleService.getTypeOfSampleById(sampleTypeId);

        if (typeOfSample != null) {

            Localization name = typeOfSample.getLocalization();
            name.setEnglish(nameEnglish.trim());
            name.setFrench(nameFrench.trim());
            name.setSysUserId(userId);

            try {
                localizationService.update(name);
            } catch (HibernateException e) {
                LogEvent.logDebug(e);
            }
        }

        // Refresh SampleType names
        DisplayListService.getInstance().getFreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
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
