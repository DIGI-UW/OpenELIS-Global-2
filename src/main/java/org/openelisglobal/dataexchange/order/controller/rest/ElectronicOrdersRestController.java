package org.openelisglobal.dataexchange.order.controller.rest;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.order.ElectronicOrderSortOrderCategoryConvertor;
import org.openelisglobal.dataexchange.order.form.ElectronicOrderViewForm;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderDisplayItem;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class ElectronicOrdersRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "searchType", "searchValue", "startDate", "endDate",
            "testIds", "statusId", "useAllInfo" };

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

    @InitBinder
    public void initBinder(final WebDataBinder webdataBinder) {
        webdataBinder.registerCustomEditor(ElectronicOrder.SortOrder.class,
                new ElectronicOrderSortOrderCategoryConvertor());
        webdataBinder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/rest/ElectronicOrders/getOrders", method = RequestMethod.GET)
    public ElectronicOrderViewForm showElectronicOrders(HttpServletRequest request,
            @ModelAttribute("form") @Valid ElectronicOrderViewForm form, BindingResult result) {
        initializeFormLists(form);
        
        if (form.getSearchType() != null) {
            processSearch(form);
        }

        return form;
    }

    private void initializeFormLists(ElectronicOrderViewForm form) {
        DisplayListService displayListService = DisplayListService.getInstance();
        form.setReferralFacilitySelectionList(displayListService.getList(ListType.REFERRAL_ORGANIZATIONS));
        form.setTestSelectionList(displayListService.getList(ListType.ORDERABLE_TESTS));
        form.setStatusSelectionList(displayListService.getList(ListType.ELECTRONIC_ORDER_STATUSES));
    }

    private void processSearch(ElectronicOrderViewForm form) {
        List<ElectronicOrder> electronicOrders = electronicOrderService.searchForElectronicOrders(form);
        List<ElectronicOrderDisplayItem> displayItems = convertToDisplayItem(electronicOrders, form.getUseAllInfo());
        form.setSearchFinished(true);
        form.setEOrders(displayItems);
    }

    private List<ElectronicOrderDisplayItem> convertToDisplayItem(List<ElectronicOrder> electronicOrders,
            boolean useAllInfo) {
        return electronicOrders.stream()
                .map(e -> convertToDisplayItem(e, useAllInfo))
                .collect(Collectors.toList());
    }

    private ElectronicOrderDisplayItem convertToDisplayItem(ElectronicOrder electronicOrder, boolean useAllInfo) {
        ElectronicOrderDisplayItem displayItem = new ElectronicOrderDisplayItem();
        try {
            populateBasicInfo(displayItem, electronicOrder);
            populatePatientInfo(displayItem, electronicOrder.getPatient());
            populateTaskInfo(displayItem, electronicOrder);
            populateOrganizationInfo(displayItem, electronicOrder);
            populateSampleInfo(displayItem, electronicOrder);

            if (useAllInfo) {
                populateExtendedInfo(displayItem, electronicOrder);
            }
        } catch (ResourceNotFoundException e) {
            handleError(displayItem, "FHIR resource not found", e);
        } catch (NullPointerException e) {
            handleError(displayItem, "null data", e);
        } catch (RuntimeException e) {
            handleError(displayItem, "unknown exception", e);
        }
        return displayItem;
    }

    private void populateBasicInfo(ElectronicOrderDisplayItem displayItem, ElectronicOrder order) {
        displayItem.setStatus(statusOfSampleService.get(order.getStatusId()).getDefaultLocalizedName());
        displayItem.setElectronicOrderId(order.getId());
        displayItem.setExternalOrderId(order.getExternalId());
        displayItem.setPriority(order.getPriority());
    }

    private void populatePatientInfo(ElectronicOrderDisplayItem displayItem, Patient patient) {
        if (patient != null) {
            displayItem.setSubjectNumber(patientService.getSubjectNumber(patient));
            displayItem.setPatientLastName(patient.getPerson().getLastName());
            displayItem.setPatientFirstName(patient.getPerson().getFirstName());
            displayItem.setPatientNationalId(patient.getNationalId());
        } else {
            handleError(displayItem, "Patient was a null resource", null);
        }
    }

    private void populateTaskInfo(ElectronicOrderDisplayItem displayItem, ElectronicOrder order) {
        Task task = fhirUtil.getFhirParser().parseResource(Task.class, order.getData());
        displayItem.setRequestDateDisplay(DateUtil.formatDateAsText(task.getAuthoredOn()));
    }

    private void populateOrganizationInfo(ElectronicOrderDisplayItem displayItem, ElectronicOrder order) {
        Task task = fhirUtil.getFhirParser().parseResource(Task.class, order.getData());
        Organization org = getOrganizationFromTask(task);
        if (org != null) {
            displayItem.setRequestingFacility(org.getOrganizationName());
        }
    }

    private Organization getOrganizationFromTask(Task task) {
        String orgId = task.getRestriction().getRecipientFirstRep().getReferenceElement().getIdPart();
        Organization org = organizationService.getOrganizationByFhirId(orgId);
        
        if (org == null && !task.getLocation().isEmpty()) {
            orgId = task.getLocation().getReferenceElement().getIdPart();
            org = organizationService.getOrganizationByFhirId(orgId);
        }
        return org;
    }

    private void populateSampleInfo(ElectronicOrderDisplayItem displayItem, ElectronicOrder order) {
        Sample sample = sampleService.getSampleByReferringId(order.getExternalId());
        if (sample != null) {
            displayItem.setLabNumber(sample.getAccessionNumber());
        }
    }

    private void populateExtendedInfo(ElectronicOrderDisplayItem displayItem, ElectronicOrder order) {
        IGenericClient fhirClient = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());
        ServiceRequest serviceRequest = fhirClient.read().resource(ServiceRequest.class)
                .withId(order.getExternalId()).execute();
        
        populateRequisitionInfo(displayItem, serviceRequest);
        populateTestInfo(displayItem, serviceRequest);
        populatePatientExtendedInfo(displayItem, serviceRequest, fhirClient);
    }

    private void populateRequisitionInfo(ElectronicOrderDisplayItem displayItem, ServiceRequest serviceRequest) {
        if (serviceRequest.getRequisition() != null) {
            displayItem.setReferringLabNumber(serviceRequest.getRequisition().getValue());
        }
    }

    private void populateTestInfo(ElectronicOrderDisplayItem displayItem, ServiceRequest serviceRequest) {
        serviceRequest.getCode().getCoding().stream()
                .filter(coding -> coding.hasSystem() && 
                        coding.getSystem().equalsIgnoreCase("http://loinc.org"))
                .findFirst()
                .ifPresent(coding -> {
                    List<Test> tests = testService.getActiveTestsByLoinc(coding.getCode());
                    if (!tests.isEmpty()) {
                        displayItem.setTestName(tests.get(0).getLocalizedTestName().getLocalizedValue());
                    }
                });
    }

    private void populatePatientExtendedInfo(ElectronicOrderDisplayItem displayItem, 
            ServiceRequest serviceRequest, IGenericClient fhirClient) {
        String patientUuid = serviceRequest.getSubject().getReferenceElement().getIdPart();
        org.hl7.fhir.r4.model.Patient fhirPatient = fhirClient.read()
                .resource(org.hl7.fhir.r4.model.Patient.class)
                .withId(patientUuid)
                .execute();

        fhirPatient.getIdentifier().forEach(identifier -> {
            if ("passport".equals(identifier.getSystem())) {
                displayItem.setPassportNumber(identifier.getId());
            }
            if ((fhirConfig.getOeFhirSystem() + "/pat_subjectNumber").equals(identifier.getSystem())) {
                displayItem.setSubjectNumber(identifier.getId());
            }
        });
    }

    private void handleError(ElectronicOrderDisplayItem displayItem, String errorMessage, Exception e) {
        displayItem.setWarnings(Arrays.asList("error in data collection - " + errorMessage));
        if (e != null) {
            LogEvent.logError(e);
        }
    }

    @Override
    protected String findLocalForward(String forward) {
        return FWD_SUCCESS.equals(forward) ? "electronicOrderViewDefinition" : "PageNotFound";
    }

    @Override
    protected String getPageTitleKey() {
        return "eorder.browse.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "eorder.browse.title";
    }
}