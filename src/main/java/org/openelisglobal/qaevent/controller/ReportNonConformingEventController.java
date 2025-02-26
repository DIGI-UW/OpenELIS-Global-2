package org.openelisglobal.qaevent.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSInvalidConfigurationException;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.RequesterService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.qaevent.form.NonConformingEventForm;
import org.openelisglobal.qaevent.service.NceCategoryService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.worker.NonConformingEventWorker;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReportNonConformingEventController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "currentUserId", "status", "id", "reportDate", "name",
            "reporterName", "dateOfEvent", "labOrderNumber", "prescriberName", "site", "reportingUnit", "description",
            "suspectedCauses", "proposedAction", "specimenId" };

    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private RequesterService requesterService;
    @Autowired
    private NceCategoryService nceCategoryService;
    @Autowired
    private NonConformingEventWorker nonConformingEventWorker;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/ReportNonConformingEvent", method = RequestMethod.GET)
    public ModelAndView showReportNonConformingEvent(@Valid @ModelAttribute("form") NonConformingEventForm oldForm,
            BindingResult result, HttpServletRequest request) throws LIMSInvalidConfigurationException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL, oldForm);
        }

        NonConformingEventForm form = new NonConformingEventForm();
        form.setCurrentUserId(getSysUserId(request));
        form.setPatientSearch(new PatientSearch());
        String labNumber = oldForm.getLabOrderNumber();
        String specimenId = oldForm.getSpecimenId();
        if (!GenericValidator.isBlankOrNull(labNumber)) {
            initForm(labNumber, specimenId, form);
        }
        addFlashMsgsToRequest(request);
        request.getSession().setAttribute(SAVE_DISABLED, TRUE);

        return findForward(FWD_SUCCESS, form);
    }

    @RequestMapping(value = "/ReportNonConformingEvent", method = RequestMethod.POST)
    public ModelAndView showReportNonConformingEventUpdate(HttpServletRequest request,
            @ModelAttribute("form") NonConformingEventForm form, BindingResult result,
            RedirectAttributes redirectAttributes) {

        boolean updated = nonConformingEventWorker.update(form);
        if (updated) {
            redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
            return findForward(FWD_SUCCESS_INSERT, form);
        } else {
            return findForward(FWD_FAIL_INSERT, form);
        }
    }

    private void initForm(String labOrderNumber, String sampleItemIds, NonConformingEventForm form)
            throws LIMSInvalidConfigurationException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {

        SystemUser systemUser = systemUserService.getUserById(form.getCurrentUserId());
        form.setName(systemUser.getFirstName() + " " + systemUser.getLastName());
        form.setNceNumber(System.currentTimeMillis() + "");
        NcEvent event = nonConformingEventWorker.create(labOrderNumber, Arrays.asList(sampleItemIds.split(",")),
                systemUser.getId(), form.getNceNumber());
        form.setNceNumber(event.getNceNumber());
        form.setId(event.getId());

        form.setLabOrderNumber(labOrderNumber);
        Sample sample = getSampleForLabNumber(labOrderNumber);
        boolean sampleFound = !(sample == null || GenericValidator.isBlankOrNull(sample.getId()));
        if (sampleFound) {
            List<SampleItem> sampleItems = new ArrayList<>();
            String[] sampleItemIdArray = sampleItemIds.split(",");
            List<String> ids = new ArrayList<>();
            for (String s : sampleItemIdArray) {
                SampleItem si = sampleItemService.getData(s);
                sampleItems.add(si);
                ids.add(si.getId());
            }
            form.setSpecimens(sampleItems);
        }

        form.setReportingUnits(
                DisplayListService.getInstance().getList(DisplayListService.ListType.TEST_SECTION_ACTIVE));

        requesterService.setSampleId(sample == null ? null : sample.getId());
        form.setSite(requesterService.getReferringSiteName());
        form.setPrescriberName(requesterService.getRequesterLastFirstName());

        form.setNceCategories(nceCategoryService.getAllNceCategories());

        Date today = Calendar.getInstance().getTime();
        form.setReportDate(DateUtil.formatDateAsText(today));
    }

    private Sample getSampleForLabNumber(String labNumber) throws LIMSInvalidConfigurationException {
        return sampleService.getSampleByAccessionNumber(labNumber);
    }

    @Override
    protected String getPageSubtitleKey() {
        return "nonconforming.page.title";
    }

    @Override
    protected String getPageTitleKey() {
        return "nonconforming.page.title";
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "reportNonConformingEventDefiniton";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/HomePage";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/ReportNonConformingEvent";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "reportNonConformingEventDefiniton";
        } else {
            return "PageNotFound";
        }
    }
}
