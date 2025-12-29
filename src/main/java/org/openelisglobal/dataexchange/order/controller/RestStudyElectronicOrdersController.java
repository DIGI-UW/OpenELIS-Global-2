package org.openelisglobal.dataexchange.order.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.order.ElectronicOrderSortOrderCategoryConvertor;
import org.openelisglobal.dataexchange.order.form.ElectronicOrderPaging;
import org.openelisglobal.dataexchange.order.form.ElectronicOrderViewForm;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderDisplayItem;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.util.OrganizationTypeList;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestStudyElectronicOrdersController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "searchType", "searchValue", "startDate", "endDate",
            "testIds", "statusId", "useAllInfo", "organizationId", "organizationList", "qaEventId", "qaAuthorizer",
            "qaNote", "electronicOrderId", };

    @Autowired
    private StatusOfSampleService statusOfSampleService;

    @Autowired
    private ElectronicOrderService electronicOrderService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private TestService testService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private FhirConfig fhirConfig;

    @Value("${org.openelisglobal.fhir.subscriber}")
    private String defaultRemoteServer;

    @InitBinder
    public void initBinder(final WebDataBinder webdataBinder) {
        webdataBinder.registerCustomEditor(ElectronicOrder.SortOrder.class,
                new ElectronicOrderSortOrderCategoryConvertor());
        webdataBinder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/rest/StudyElectronicOrders", method = RequestMethod.GET)
    public ElectronicOrderViewForm showStudyElectronicOrders(HttpServletRequest request,
            @ModelAttribute("form") @Valid ElectronicOrderViewForm form, BindingResult result)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        LogEvent.logInfo(this.getClass().getSimpleName(), "showStudyElectronicOrders",
                "Received request with parameters: " + "searchType=" + request.getParameter("searchType")
                        + ", searchValue=" + request.getParameter("searchValue"));

        try {
            form.setReferralFacilitySelectionList(
                    DisplayListService.getInstance().getList(ListType.REFERRAL_ORGANIZATIONS));
            form.setTestSelectionList(DisplayListService.getInstance().getList(ListType.ORDERABLE_TESTS));
            form.setStatusSelectionList(DisplayListService.getInstance().getList(ListType.ELECTRONIC_ORDER_STATUSES));
            form.setOrganizationList(OrganizationTypeList.ARV_ORGS.getList());
            form.setQaEvents(DisplayListService.getInstance().getList(ListType.QA_EVENTS));

            // Explicitly bind request parameters to form
            String searchType = request.getParameter("searchType");
            if (searchType != null) {
                try {
                    form.setSearchType(ElectronicOrderViewForm.SearchType.valueOf(searchType));
                    LogEvent.logInfo(this.getClass().getSimpleName(), "showStudyElectronicOrders",
                            "Successfully set searchType to: " + searchType);
                } catch (IllegalArgumentException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "showStudyElectronicOrders",
                            "Invalid searchType parameter: " + searchType);
                }
            }
            String searchValue = request.getParameter("searchValue");
            if (searchValue != null) {
                form.setSearchValue(searchValue);
            }
            String startDate = request.getParameter("startDate");
            if (startDate != null) {
                form.setStartDate(startDate);
            }
            String endDate = request.getParameter("endDate");
            if (endDate != null) {
                form.setEndDate(endDate);
            }
            String statusId = request.getParameter("statusId");
            if (statusId != null) {
                form.setStatusId(statusId);
            }

            List<ElectronicOrder> electronicOrders;
            List<ElectronicOrderDisplayItem> eOrderDisplayItems = new ArrayList<>();
            ElectronicOrderPaging paging = new ElectronicOrderPaging();
            String requestedPage = request.getParameter("page");
            if (GenericValidator.isBlankOrNull(requestedPage)) {
                if (form.getSearchType() != null) {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "showStudyElectronicOrders",
                            "Searching for electronic orders with searchType: " + form.getSearchType());
                    electronicOrders = electronicOrderService.searchForElectronicOrders(form);
                    LogEvent.logInfo(this.getClass().getSimpleName(), "showStudyElectronicOrders",
                            "Found " + electronicOrders.size() + " electronic orders");
                    eOrderDisplayItems = convertToDisplayItem(electronicOrders);
                    paging.setDatabaseResults(request, form, eOrderDisplayItems);
                }
            } else {
                int requestedPageNumber = Integer.parseInt(requestedPage);
                // Sets the requested page in the response.
                paging.page(request, form, requestedPageNumber);
            }
            form.setSearchFinished(true);

            LogEvent.logInfo(this.getClass().getSimpleName(), "showStudyElectronicOrders",
                    "Successfully returning form with " + (form.geteOrders() != null ? form.geteOrders().size() : 0)
                            + " orders");

            return form;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "showStudyElectronicOrders",
                    "Error processing request: " + e.getMessage());
            LogEvent.logError(e);
            throw e;
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        LogEvent.logError(this.getClass().getSimpleName(), "handleException", "Unhandled exception: " + e.getMessage());
        LogEvent.logError(e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
    }

    private List<ElectronicOrderDisplayItem> convertToDisplayItem(List<ElectronicOrder> electronicOrders) {
        return electronicOrders.stream().map(e -> convertToDisplayItem(e)).collect(Collectors.toList());
    }

    private ElectronicOrderDisplayItem convertToDisplayItem(ElectronicOrder electronicOrder) {
        ElectronicOrderDisplayItem displayItem = new ElectronicOrderDisplayItem();

        try {
            displayItem.setStatus(statusOfSampleService.get(electronicOrder.getStatusId()).getDefaultLocalizedName());
            displayItem.setElectronicOrderId(electronicOrder.getId());
            displayItem.setExternalOrderId(electronicOrder.getExternalId());
            displayItem.setPriority(electronicOrder.getPriority());
            displayItem.setQaEventId(electronicOrder.getRejectReasonId());
            Patient patient = electronicOrder.getPatient();
            if (patient != null) {
                displayItem.setSubjectNumber(patientService.getSubjectNumber(patient));
                displayItem.setPatientNationalId(patient.getNationalId());
                displayItem.setBirthDate(patient.getBirthDateForDisplay());
                displayItem.setGender(patient.getGender());
                displayItem.setPatientUpid(patient.getUpidCode());
            } else {
                String errorMsg = "error in data collection - Patient was a null resource";
                displayItem.setWarnings(Arrays.asList(errorMsg));
            }
            Task task = fhirUtil.getFhirParser().parseResource(Task.class, electronicOrder.getData());
            displayItem.setRequestDateDisplay(DateUtil.formatDateAsText(task.getAuthoredOn()));
            for (ParameterComponent parameter : task.getInput()) {
                if (parameter.getType().getCodingFirstRep().getCode().equals("CI0050005AAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    // VL
                    // demand date
                    if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                        if (parameter.getValue() instanceof DateTimeType) {
                            DateTimeType dateValue = (DateTimeType) parameter.getValue();
                            if (ObjectUtils.isNotEmpty(dateValue))
                                displayItem.setRequestDateDisplay(DateUtil.formatDateAsText(dateValue.getValue()));
                        }
                    }
                }
            }
            Organization organization = organizationService.getOrganizationByFhirId(
                    task.getRestriction().getRecipientFirstRep().getReferenceElement().getIdPart());
            if (organization != null) {
                displayItem.setRequestingFacility(organization.getOrganizationName());
            }

            Sample sample = sampleService.getSampleByReferringId(electronicOrder.getExternalId());
            if (sample != null) {
                displayItem.setLabNumber(sample.getAccessionNumber());
            }
            IGenericClient fhirClient = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());

            ServiceRequest serviceRequest = fhirClient.read().resource(ServiceRequest.class)
                    .withId(electronicOrder.getExternalId()).execute();
            if (serviceRequest.getRequisition() != null) {
                displayItem.setReferringLabNumber(serviceRequest.getRequisition().getValue());
            }
            org.hl7.fhir.r4.model.Patient fhirPatient = fhirClient.read() //
                    .resource(org.hl7.fhir.r4.model.Patient.class) //
                    .withId(serviceRequest.getSubject().getReferenceElement().getIdPart()) //
                    .execute();
            if (fhirPatient != null) {
                for (Identifier identifier : fhirPatient.getIdentifier()) {
                    // get patient UPID
                    if (("https://openmrs.org/UPI").equals(identifier.getSystem())) {
                        displayItem.setPatientUpid(identifier.getValue());
                        break;
                    }
                    // get location name
                    if (("http://fhir.openmrs.org/ext/patient/identifier#location")
                            .equals(identifier.getExtensionFirstRep().getUrl())) {
                        Extension extension = identifier.getExtensionFirstRep();
                        Reference locationReference = (Reference) extension.getValue();
                        String display = locationReference.getDisplay();
                        displayItem.setRequestingFacility(display);
                    }
                }
            }

            Encounter encounter = fhirUtil.getFhirClient(defaultRemoteServer).read().resource(Encounter.class)
                    .withId(serviceRequest.getEncounter().getReferenceElement().getIdPart()).execute();
            if (ObjectUtils.isNotEmpty(encounter)) {
                // get Collection Date
                Period period = encounter.getPeriod();
                if (ObjectUtils.isNotEmpty(period)) {
                    Date collectionDate = encounter.getPeriod().getStart();
                    if (ObjectUtils.isNotEmpty(collectionDate)) {
                        displayItem.setCollectionDateDisplay(DateUtil.formatDateAsText(collectionDate));
                    }
                }
            }
            Test test = null;
            for (Coding coding : serviceRequest.getCode().getCoding()) {
                if (coding.hasSystem()) {
                    if (coding.getSystem().equalsIgnoreCase("http://loinc.org")) {
                        List<Test> tests = testService.getActiveTestsByLoinc(coding.getCode());
                        if (tests.size() != 0) {
                            test = tests.get(0);
                            break;
                        }
                    }
                }
            }
            if (test != null) {
                displayItem.setTestName(test.getLocalizedTestName().getLocalizedValue());
            }
        } catch (ResourceNotFoundException e) {
            String errorMsg = "error in data collection - FHIR resource not found";
            displayItem.setWarnings(Arrays.asList(errorMsg));
            LogEvent.logError(e);
        } catch (NullPointerException e) {
            String errorMsg = "error in data collection - null data";
            displayItem.setWarnings(Arrays.asList(errorMsg));
            LogEvent.logError(e);
        } catch (RuntimeException e) {
            String errorMsg = "error in data collection - unknown exception";
            displayItem.setWarnings(Arrays.asList(errorMsg));
            LogEvent.logError(e);
        }

        return displayItem;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "studyElectronicOrderViewDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "eorder.study.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "eorder.study.title";
    }

    @RequestMapping(value = "/rest/rejectStudyElectronicOrder", method = RequestMethod.POST)
    public ResponseEntity<?> rejectStudyElectronicOrder(HttpServletRequest request,
            @ModelAttribute("form") @Valid ElectronicOrderViewForm form, BindingResult result) {
        LogEvent.logInfo(this.getClass().getSimpleName(), "rejectStudyElectronicOrder", "Rejecting electronic order: "
                + form.getQaEventId() + " for order ID: " + request.getParameter("electronicOrderId"));

        try {
            String electronicOrderId = request.getParameter("electronicOrderId");
            String qaEventId = request.getParameter("qaEventId");
            String qaNote = request.getParameter("qaNote");
            String qaAuthorizer = request.getParameter("qaAuthorizer");

            if (GenericValidator.isBlankOrNull(electronicOrderId)) {
                return ResponseEntity.badRequest().body("Electronic order ID is required");
            }

            if (GenericValidator.isBlankOrNull(qaEventId)) {
                return ResponseEntity.badRequest().body("Rejection reason is required");
            }

            ElectronicOrder electronicOrder = electronicOrderService.get(electronicOrderId);
            if (electronicOrder == null) {
                return ResponseEntity.badRequest().body("Electronic order not found");
            }

            // Set rejection details
            electronicOrder.setRejectReasonId(qaEventId);
            electronicOrder.setRejectComment(qaNote);
            electronicOrder.setQaAuthorizer(qaAuthorizer);

            // Set status to Cancelled (status_id = 22 for EXTERNAL_ORDER Cancelled)
            electronicOrder.setStatusId("22");

            // Update the electronic order
            electronicOrderService.update(electronicOrder);

            LogEvent.logInfo(this.getClass().getSimpleName(), "rejectStudyElectronicOrder",
                    "Successfully rejected electronic order: " + electronicOrderId);

            return ResponseEntity.ok().body(java.util.Collections.singletonMap("success", true));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "rejectStudyElectronicOrder",
                    "Error rejecting electronic order: " + e.getMessage());
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error rejecting order: " + e.getMessage());
        }
    }
}
