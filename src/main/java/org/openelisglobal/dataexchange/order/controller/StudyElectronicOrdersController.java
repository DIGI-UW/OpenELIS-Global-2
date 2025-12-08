package org.openelisglobal.dataexchange.order.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.order.ElectronicOrderSortOrderCategoryConvertor;
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
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class StudyElectronicOrdersController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "searchType", "searchValue", "startDate", "endDate",
            "testIds", "statusId", "useAllInfo", "organizationId", "organizationList" };

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
    @Autowired
    private FhirPersistanceService fhirPersistanceService;
    @Autowired
    private HttpServletRequest request;

    @Value("${org.openelisglobal.fhir.subscriber}")
    private String defaultRemoteServer;

    @InitBinder
    public void initBinder(final WebDataBinder webdataBinder) {
        webdataBinder.registerCustomEditor(ElectronicOrder.SortOrder.class,
                new ElectronicOrderSortOrderCategoryConvertor());
        webdataBinder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/rejectElectronicOrders", method = RequestMethod.GET)
    public ModelAndView rejectElectronicOrders(HttpServletRequest request,
            @ModelAttribute("form") @Valid ElectronicOrderViewForm form, BindingResult result) {
        try {
            ServiceRequest serviceRequest = null;

            String externalOrderNumber = request.getParameter("externalOrderId");
            String qaEventId = request.getParameter("qaEventId");
            String qaAuthorizer = request.getParameter("qaAuthorizer");
            String qaNote = request.getParameter("qaNote");
            String searchType = request.getParameter("searchType");
            String startDate = request.getParameter("startDate");
            String endDate = request.getParameter("endDate");
            String statusId = request.getParameter("statusId");

            if (StringUtils.isNotBlank(externalOrderNumber)) {
                ElectronicOrder eOrder = null;
                List<ElectronicOrder> eOrders = electronicOrderService
                        .getElectronicOrdersByExternalId(externalOrderNumber);
                if (eOrders.size() > 0)
                    eOrder = eOrders.get(eOrders.size() - 1);
                if (eOrder != null) {

                    IGenericClient localFhirClient = fhirUtil.getLocalFhirClient();
                    // IGenericClient remoteFhirClient =
                    // fhirUtil.getFhirClient(defaultRemoteServer);
                    for (String remotePath : fhirConfig.getRemoteStorePaths()) {
                        Bundle srBundle = (Bundle) localFhirClient.search().forResource(ServiceRequest.class)
                                .where(ServiceRequest.RES_ID.exactly().code(externalOrderNumber))
                                .include(ServiceRequest.INCLUDE_SPECIMEN).execute();
                        for (BundleEntryComponent bundleComponent : srBundle.getEntry()) {
                            if (bundleComponent.hasResource() && ResourceType.ServiceRequest
                                    .equals(bundleComponent.getResource().getResourceType())) {
                                serviceRequest = (ServiceRequest) bundleComponent.getResource();
                            }
                        }
                        srBundle = (Bundle) localFhirClient
                                .search().forResource(ServiceRequest.class).where(ServiceRequest.IDENTIFIER.exactly()
                                        .systemAndIdentifier(remotePath, externalOrderNumber))
                                .include(ServiceRequest.INCLUDE_SPECIMEN).execute();
                        for (BundleEntryComponent bundleComponent : srBundle.getEntry()) {
                            if (bundleComponent.hasResource() && ResourceType.ServiceRequest
                                    .equals(bundleComponent.getResource().getResourceType())) {
                                serviceRequest = (ServiceRequest) bundleComponent.getResource();
                            }
                        }
                    }

                    eOrder.setStatusId(
                            SpringContext.getBean(IStatusService.class).getStatusID(ExternalOrderStatus.NonConforming));
                    eOrder.setRejectReasonId(qaEventId);
                    eOrder.setRejectComment(qaNote);
                    eOrder.setQaAuthorizer(qaAuthorizer);
                    electronicOrderService.update(eOrder);
                    // update Task
                    Task task = fhirPersistanceService.getTaskBasedOnServiceRequest(externalOrderNumber).orElseThrow();
                    task.setStatus(TaskStatus.REJECTED);
                    fhirPersistanceService.updateFhirResourceInFhirStore(task);
                }
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }
        return findForward(FWD_SUCCESS_INSERT, form);
    }

    @RequestMapping(value = "/StudyElectronicOrders", method = RequestMethod.GET)
    public ModelAndView showElectronicOrders(HttpServletRequest request,
            @ModelAttribute("form") @Valid ElectronicOrderViewForm form, BindingResult result) {
        form.setReferralFacilitySelectionList(
                DisplayListService.getInstance().getList(ListType.REFERRAL_ORGANIZATIONS));
        form.setTestSelectionList(DisplayListService.getInstance().getList(ListType.ORDERABLE_TESTS));
        form.setStatusSelectionList(DisplayListService.getInstance().getList(ListType.ELECTRONIC_ORDER_STATUSES));
        form.setOrganizationList(OrganizationTypeList.ARV_ORGS.getList());
        form.setQaEvents(DisplayListService.getInstance().getList(ListType.QA_EVENTS));
        if (form.getSearchType() != null) {
            List<ElectronicOrder> electronicOrders;
            List<ElectronicOrderDisplayItem> eOrderDisplayItems;

            electronicOrders = electronicOrderService.searchForElectronicOrders(form);
            eOrderDisplayItems = convertToDisplayItem(electronicOrders, form.getUseAllInfo());
            form.setSearchFinished(true);
            form.seteOrders(eOrderDisplayItems);
        }

        return findForward(FWD_SUCCESS, form);
    }

    private List<ElectronicOrderDisplayItem> convertToDisplayItem(List<ElectronicOrder> electronicOrders,
            boolean useAllInfo) {
        return electronicOrders.stream().map(e -> convertToDisplayItem(e, useAllInfo)).collect(Collectors.toList());
    }

    private ElectronicOrderDisplayItem convertToDisplayItem(ElectronicOrder electronicOrder, boolean useAllInfo) {
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
                if (parameter.getType().getCodingFirstRep().getCode().equals("CI0050005AAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // VL
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
            if (ObjectUtils.isNotEmpty(encounter)) { // get Collection Date
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
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            String externalOrderNumber = request.getParameter("externalOrderId");
            String qaEventId = request.getParameter("qaEventId");
            String qaAuthorizer = request.getParameter("qaAuthorizer");
            String qaNote = request.getParameter("qaNote");
            String searchType = request.getParameter("searchType");
            String startDate = request.getParameter("startDate");
            String endDate = request.getParameter("endDate");
            String statusId = request.getParameter("statusId");
            String searchParameters = "?searchType=" + Encode.forUriComponent(searchType) + "&startDate="
                    + Encode.forUriComponent(startDate) + "&endDate=" + Encode.forUriComponent(endDate) + "&statusId="
                    + Encode.forUriComponent(statusId);
            return "redirect:/StudyElectronicOrders" + searchParameters;
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "eorder.browse.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "study.eorder.browse.title";
    }
}
