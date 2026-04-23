package org.openelisglobal.dataexchange.service.order;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.validator.GenericValidator;
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
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.order.dao.ElectronicOrderDAO;
import org.openelisglobal.dataexchange.order.form.ElectronicOrderViewForm;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder.SortOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderDisplayItem;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ElectronicOrderServiceImpl extends AuditableBaseObjectServiceImpl<ElectronicOrder, String>
        implements ElectronicOrderService {
    @Autowired
    protected ElectronicOrderDAO baseObjectDAO;
    @Autowired
    protected IStatusService statusService;
    @Autowired
    protected OrganizationService organizationService;
    @Autowired
    protected TestService testService;
    @Autowired
    protected FhirUtil fhirUtil;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private PatientService patientService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private StatusOfSampleService statusOfSampleService;
    @Value("${org.openelisglobal.fhir.subscriber}")
    private String defaultRemoteServer;

    ElectronicOrderServiceImpl() {
        super(ElectronicOrder.class);
    }

    @Override
    protected ElectronicOrderDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicOrder> getAllElectronicOrdersOrderedBy(SortOrder order) {
        return getBaseObjectDAO().getAllElectronicOrdersOrderedBy(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicOrder> getElectronicOrdersByExternalId(String id) {
        return getBaseObjectDAO().getElectronicOrdersByExternalId(id);
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersContainingValueOrderedBy(String searchValue, SortOrder order) {

        List<ElectronicOrder> searchResult = getBaseObjectDAO()
                .getAllElectronicOrdersContainingValueOrderedBy(searchValue, order);

        if (searchResult != null && searchResult.size() > 0) {
            return searchResult;
        }
        // this is done in case sample lab number was used to search instead of the
        // order lab number
        if (searchValue != null && searchValue.contains(".")) {
            searchValue = searchValue.substring(0, searchValue.indexOf('.'));
        }
        return getBaseObjectDAO().getAllElectronicOrdersContainingValueOrderedBy(searchValue, order);
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersContainingValuesOrderedBy(String accessionNumber,
            String patientLastName, String patientFirstName, String gender, SortOrder order) {
        return getBaseObjectDAO().getAllElectronicOrdersContainingValuesOrderedBy(accessionNumber, patientLastName,
                patientFirstName, gender, order);
    }

    @Override
    public List<ElectronicOrder> getElectronicOrdersContainingValueExludedByOrderedBy(String searchValue,
            List<ExternalOrderStatus> excludedStatuses, SortOrder sortOrder) {
        List<String> exludedStatusIds = new ArrayList<>();
        for (ExternalOrderStatus status : excludedStatuses) {
            String statusId = statusService.getStatusID(status);
            if (!GenericValidator.isBlankOrNull(statusId)) {
                exludedStatusIds.add(statusId);
            }
        }

        return getBaseObjectDAO().getElectronicOrdersContainingValueExludedByOrderedBy(searchValue, exludedStatusIds,
                sortOrder);
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersByDateAndStatus(Date startDate, Date endDate, String statusId,
            SortOrder sortOrder) {
        return getBaseObjectDAO().getAllElectronicOrdersByDateAndStatus(startDate, endDate, statusId, sortOrder);
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersByTimestampAndStatus(Timestamp startTimestamp,
            Timestamp endTimestamp, String statusId, SortOrder sortOrder) {
        return getBaseObjectDAO().getAllElectronicOrdersByTimestampAndStatus(startTimestamp, endTimestamp, statusId,
                sortOrder);
    }

    @Override
    public int getCountOfElectronicOrdersByTimestampAndStatus(Timestamp startTimestamp, Timestamp endTimestamp,
            String statusId) {
        return getBaseObjectDAO().getCountOfElectronicOrdersByTimestampAndStatus(startTimestamp, endTimestamp,
                statusId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicOrder> searchForElectronicOrders(ElectronicOrderViewForm form) {
        switch (form.getSearchType()) {
        case IDENTIFIER:
            IGenericClient fhirClient = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());
            Bundle searchBundle = fhirClient.search().forResource(ServiceRequest.class)
                    .where(ServiceRequest.IDENTIFIER.exactly().code(form.getSearchValue())).returnBundle(Bundle.class)
                    .execute();

            List<String> identifierValues = new ArrayList<>(searchBundle.getEntry().size() + 1);
            identifierValues.add(form.getSearchValue());
            for (BundleEntryComponent bundleEntry : searchBundle.getEntry()) {
                if (bundleEntry.hasResource()
                        && ResourceType.ServiceRequest.equals(bundleEntry.getResource().getResourceType())) {
                    identifierValues.add(bundleEntry.getResource().getIdElement().getIdPart());
                }
            }
            String nameValue = form.getSearchValue();

            List<ElectronicOrder> eOrders = baseObjectDAO.getAllElectronicOrdersMatchingAnyValue(identifierValues,
                    nameValue, SortOrder.LAST_UPDATED_ASC);

            return eOrders;
        case DATE_STATUS:
            String startDate = form.getStartDate();
            String endDate = form.getEndDate();
            if (GenericValidator.isBlankOrNull(startDate) && !GenericValidator.isBlankOrNull(endDate)) {
                startDate = endDate;
            }
            if (GenericValidator.isBlankOrNull(endDate) && !GenericValidator.isBlankOrNull(startDate)) {
                endDate = startDate;
            }
            java.sql.Timestamp startTimestamp = GenericValidator.isBlankOrNull(startDate) ? null
                    : DateUtil.convertStringDateStringTimeToTimestamp(startDate, "00:00:00.0");
            java.sql.Timestamp endTimestamp = GenericValidator.isBlankOrNull(endDate) ? null
                    : DateUtil.convertStringDateStringTimeToTimestamp(endDate, "23:59:59");
            return getAllElectronicOrdersByTimestampAndStatus(startTimestamp, endTimestamp, form.getStatusId(),
                    SortOrder.STATUS_ID);
        default:
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicOrder> searchForStudyElectronicOrders(ElectronicOrderViewForm form) {
        switch (form.getSearchType()) {
        case IDENTIFIER:
            IGenericClient fhirClient = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());
            Bundle searchBundle = fhirClient.search().forResource(ServiceRequest.class)
                    .where(ServiceRequest.IDENTIFIER.exactly().code(form.getSearchValue())).returnBundle(Bundle.class)
                    .execute();

            List<String> identifierValues = new ArrayList<>(searchBundle.getEntry().size() + 1);
            identifierValues.add(form.getSearchValue());
            for (BundleEntryComponent bundleEntry : searchBundle.getEntry()) {
                if (bundleEntry.hasResource()
                        && ResourceType.ServiceRequest.equals(bundleEntry.getResource().getResourceType())) {
                    identifierValues.add(bundleEntry.getResource().getIdElement().getIdPart());
                }
            }
            String nameValue = form.getSearchValue();

            List<ElectronicOrder> eOrders = baseObjectDAO.getAllElectronicOrdersMatchingAnyValue(identifierValues,
                    nameValue, SortOrder.LAST_UPDATED_ASC);

            return eOrders;
        case DATE_STATUS:
            String startDate = form.getStartDate();
            String endDate = form.getEndDate();
            if (GenericValidator.isBlankOrNull(startDate) && !GenericValidator.isBlankOrNull(endDate)) {
                startDate = endDate;
            }
            if (GenericValidator.isBlankOrNull(endDate) && !GenericValidator.isBlankOrNull(startDate)) {
                endDate = startDate;
            }
            java.sql.Timestamp startTimestamp = GenericValidator.isBlankOrNull(startDate) ? null
                    : DateUtil.convertStringDateStringTimeToTimestamp(startDate, "00:00:00.0");
            java.sql.Timestamp endTimestamp = GenericValidator.isBlankOrNull(endDate) ? null
                    : DateUtil.convertStringDateStringTimeToTimestamp(endDate, "23:59:59");
            return getAllElectronicOrdersByTimestampAndStatus(startTimestamp, endTimestamp, form.getStatusId(),
                    SortOrder.STATUS_ID);
        default:
            return null;
        }
    }

    @Override
    public int getCountOfAllElectronicOrdersByDateAndStatus(Date startDate, Date endDate, String statusId) {
        return getBaseObjectDAO().getCountOfAllElectronicOrdersByDateAndStatus(startDate, endDate, statusId);
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersByStatusList(List<String> statusIds, SortOrder sortOrder) {
        return getBaseObjectDAO().getAllElectronicOrdersByStatusList(statusIds, sortOrder);
    }

    @Override
    public int getCountOfElectronicOrdersByStatusList(List<String> statusIds) {
        return getBaseObjectDAO().getCountOfElectronicOrdersByStatusList(statusIds);
    }

    @Override
    @Transactional(readOnly = true)
    public ElectronicOrderDisplayItem buildStudyElectronicOrderDisplayItem(ElectronicOrder electronicOrder) {
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
                displayItem.setWarnings(Arrays.asList("error in data collection - Patient was a null resource"));
            }
            Task task = fhirUtil.getFhirParser().parseResource(Task.class, electronicOrder.getData());
            displayItem.setRequestDateDisplay(DateUtil.formatDateAsText(task.getAuthoredOn()));
            for (ParameterComponent parameter : task.getInput()) {
                if (parameter.getType().getCodingFirstRep().getCode().equals("CI0050005AAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    if (ObjectUtils.isNotEmpty(parameter.getValue()) && parameter.getValue() instanceof DateTimeType) {
                        DateTimeType dateValue = (DateTimeType) parameter.getValue();
                        if (ObjectUtils.isNotEmpty(dateValue))
                            displayItem.setRequestDateDisplay(DateUtil.formatDateAsText(dateValue.getValue()));
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
            org.hl7.fhir.r4.model.Patient fhirPatient = fhirClient.read().resource(org.hl7.fhir.r4.model.Patient.class)
                    .withId(serviceRequest.getSubject().getReferenceElement().getIdPart()).execute();
            if (fhirPatient != null) {
                for (Identifier identifier : fhirPatient.getIdentifier()) {
                    if ("https://openmrs.org/UPI".equals(identifier.getSystem())) {
                        displayItem.setPatientUpid(identifier.getValue());
                        break;
                    }
                    if ("http://fhir.openmrs.org/ext/patient/identifier#location"
                            .equals(identifier.getExtensionFirstRep().getUrl())) {
                        Extension extension = identifier.getExtensionFirstRep();
                        Reference locationReference = (Reference) extension.getValue();
                        displayItem.setRequestingFacility(locationReference.getDisplay());
                    }
                }
            }
            Encounter encounter = fhirUtil.getFhirClient(defaultRemoteServer).read().resource(Encounter.class)
                    .withId(serviceRequest.getEncounter().getReferenceElement().getIdPart()).execute();
            if (ObjectUtils.isNotEmpty(encounter)) {
                Period period = encounter.getPeriod();
                if (ObjectUtils.isNotEmpty(period)) {
                    java.util.Date collectionDate = period.getStart();
                    if (ObjectUtils.isNotEmpty(collectionDate))
                        displayItem.setCollectionDateDisplay(DateUtil.formatDateAsText(collectionDate));
                }
            }
            Test test = null;
            for (Coding coding : serviceRequest.getCode().getCoding()) {
                if (coding.hasSystem() && coding.getSystem().equalsIgnoreCase("http://loinc.org")) {
                    List<Test> tests = testService.getActiveTestsByLoinc(coding.getCode());
                    if (!tests.isEmpty()) {
                        test = tests.get(0);
                        break;
                    }
                }
            }
            if (test != null) {
                displayItem.setTestName(test.getLocalizedTestName().getLocalizedValue());
            }
        } catch (ResourceNotFoundException e) {
            displayItem.setWarnings(Arrays.asList("error in data collection - FHIR resource not found"));
            LogEvent.logError(e);
        } catch (NullPointerException e) {
            displayItem.setWarnings(Arrays.asList("error in data collection - null data"));
            LogEvent.logError(e);
        } catch (RuntimeException e) {
            displayItem.setWarnings(Arrays.asList("error in data collection - unknown exception"));
            LogEvent.logError(e);
        }
        return displayItem;
    }
}
