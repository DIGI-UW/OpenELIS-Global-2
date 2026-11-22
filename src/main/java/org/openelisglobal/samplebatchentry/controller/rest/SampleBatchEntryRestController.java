package org.openelisglobal.samplebatchentry.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.StaleObjectStateException;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.SampleOrderService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.samplebatchentry.form.SampleBatchEntryForm;
import org.openelisglobal.samplebatchentry.form.SampleBatchEntrySaveForm;
import org.openelisglobal.samplebatchentry.service.SampleBatchEntryService;
import org.openelisglobal.samplebatchentry.validator.SampleBatchEntryFormValidator;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
@RequestMapping("/rest")
public class SampleBatchEntryRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "patientProperties.currentDate",
            "patientProperties.patientLastUpdated", "patientProperties.personLastUpdated",
            "patientProperties.patientUpdateStatus", "patientProperties.patientPK", "patientProperties.guid",
            "patientProperties.STnumber", "patientProperties.subjectNumber", "patientProperties.nationalId",
            "patientProperties.lastName", "patientProperties.firstName", "patientProperties.aka",
            "patientProperties.birthDateForDisplay", "patientProperties.age", "patientProperties.gender",
            //
            "sampleOrderItems.labNo",
            //
            "sampleOrderItems.newRequesterName", "sampleOrderItems.referringSiteId",
            "sampleOrderItems.referringSiteDepartmentId", "form.sampleOrderItems.referringSiteName",
            "patientProperties.patientUpdateStatus", "currentDate", "currentTime",
            "sampleOrderItems.receivedDateForDisplay", "sampleOrderItems.receivedTime", "sampleXML", "testSectionList",
            //
            "method", "facilityIDCheck", "facilityID", "patientInfoCheck" };

    @Autowired
    SampleBatchEntryFormValidator formValidator;

    @Autowired
    TestService testService;
    @Autowired
    TypeOfSampleService typeOfSampleService;
    @Autowired
    OrganizationService organizationService;

    @Autowired
    private SampleBatchEntryService sampleBatchEntryService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @PostMapping(value = "/SampleBatchEntry", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SampleBatchEntryForm showSampleBatchEntry(HttpServletRequest request, @RequestBody SampleBatchEntryForm form,
            BindingResult result, RedirectAttributes redirectAttributes) throws DocumentException {
        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            return (form);
        }
        String sampleXML = form.getSampleXML();
        SampleOrderService sampleOrderService = new SampleOrderService();
        SampleOrderItem soi = sampleOrderService.getSampleOrderItem();
        soi.setReceivedTime(form.getSampleOrderItems().getReceivedTime());
        soi.setReceivedDateForDisplay(form.getSampleOrderItems().getReceivedDateForDisplay());
        soi.setNewRequesterName(form.getSampleOrderItems().getNewRequesterName());
        soi.setReferringSiteId(form.getFacilityID());
        soi.setReferringSiteDepartmentId(form.getSampleOrderItems().getReferringSiteDepartmentId());

        form.setSampleOrderItems(soi);

        form.setLocalDBOnly(ConfigurationProperties.getInstance()
                .getPropertyValueLowerCase(Property.UseExternalPatientInfo).equals("false"));

        // get summary of tests selected to place in common fields section
        Document sampleDom = DocumentHelper.parseText(sampleXML);
        Element sampleItem = sampleDom.getRootElement().element("sample");
        String testIDs = sampleItem.attributeValue("tests");
        StringTokenizer tokenizer = new StringTokenizer(testIDs, ",");
        StringBuilder sBuilder = new StringBuilder();
        String separator = "";
        while (tokenizer.hasMoreTokens()) {
            sBuilder.append(separator);
            sBuilder.append(TestServiceImpl.getUserLocalizedTestName(testService.get(tokenizer.nextToken().trim())));
            separator = "<br>";
        }
        String sampleType = typeOfSampleService.get(sampleItem.attributeValue("sampleID")).getLocalAbbreviation();
        String testNames = sBuilder.toString();
        request.setAttribute("sampleType", sampleType);
        request.setAttribute("testNames", testNames);

        // get facility name from id
        String facilityName = "";
        if (!StringUtil.isNullorNill(form.getFacilityID())) {
            Organization organization = organizationService.get(form.getFacilityID());
            facilityName = organization.getOrganizationName();
        } else if (!StringUtil.isNullorNill(form.getSampleOrderItems().getNewRequesterName())) {
            facilityName = form.getSampleOrderItems().getNewRequesterName();
        }
        String departmentName = "";
        if (!StringUtil.isNullorNill(form.getSampleOrderItems().getReferringSiteDepartmentId())) {
            Organization organization = organizationService
                    .get(form.getSampleOrderItems().getReferringSiteDepartmentId());
            departmentName = organization.getOrganizationName();
        }
        request.setAttribute("facilityName", facilityName);
        request.setAttribute("departmentName", departmentName);
        form.setPatientSearch(new PatientSearch());

        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);

        return (form);
    }

    @PostMapping(value = "/SamplePatientEntryBatch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> showSamplePatientEntrySave(HttpServletRequest request,
            @RequestBody @Validated(SamplePatientEntryForm.SamplePatientEntryBatch.class) SampleBatchEntrySaveForm form,
            BindingResult result, RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        try {
            if (!sampleBatchEntryService.save(form, result, request, getSysUserId(request))) {
                saveErrors(result);
                return ResponseEntity.badRequest().body(buildErrorBody(result));
            }
        } catch (LIMSRuntimeException e) {
            if (e.getCause() instanceof StaleObjectStateException) {
                result.reject("errors.OptimisticLockException", "errors.OptimisticLockException");
            } else {
                LogEvent.logDebug(e);
                result.reject("errors.UpdateException", "errors.UpdateException");
            }
            LogEvent.logInfo(this.getClass().getSimpleName(), "showSamplePatientEntrySave", result.toString());
            saveErrors(result);
            request.setAttribute(ALLOW_EDITS_KEY, "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildErrorBody(result));
        }

        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        return ResponseEntity.ok(form);
    }

    private static Map<String, Object> buildErrorBody(BindingResult result) {
        List<Map<String, String>> fieldErrors = result.getFieldErrors().stream().map(fe -> Map.of("field",
                fe.getField(), "defaultMessage", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : ""))
                .collect(Collectors.toList());
        String message = "Validation failed";
        if (!fieldErrors.isEmpty()) {
            message = fieldErrors.get(0).get("field") + ": " + fieldErrors.get(0).get("defaultMessage");
        } else if (!result.getGlobalErrors().isEmpty() && result.getGlobalErrors().get(0).getDefaultMessage() != null) {
            message = result.getGlobalErrors().get(0).getDefaultMessage();
        }
        return Map.of("error", message, "fieldErrors", fieldErrors);
    }

    @Override
    protected String findLocalForward(String forward) {
        switch (forward) {
        case "On Demand":
            return "sampleBatchEntryOnDemandDefinition";
        case "Pre-Printed":
            return "sampleBatchEntryPrePrintedDefinition";
        case FWD_FAIL:
        case FWD_FAIL_INSERT:
            return "sampleBatchEntrySetupDefinition";
        case FWD_SUCCESS_INSERT:
            return "redirect:/SamplePatientEntry";
        default:
            return "redirect:/SampleBatchEntrySetup";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "sample.batchentry.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "sample.batchentry.title";
    }
}
