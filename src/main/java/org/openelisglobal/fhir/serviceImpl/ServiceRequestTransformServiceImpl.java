package org.openelisglobal.fhir.serviceImpl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestIntent;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestStatus;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.dataexchange.fhir.FHIRTransformUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.fhir.service.ServiceRequestTransformService;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.bean.SampleEditItem;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceRequestTransformServiceImpl implements ServiceRequestTransformService {
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FHIRTransformUtil fhirTransformUtil;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private ObservationHistoryService observationHistoryService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private OrganizationService organizationService;

    @Override
    public ServiceRequest transformToServiceRequest(Analysis analysis) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToServiceRequest",
                "transformToServiceRequest called");

        Sample sample = analysis.getSampleItem().getSample();
        Patient patient = sampleHumanService.getPatientForSample(sample);
        Provider provider = sampleHumanService.getProviderForSample(sample);

        Organization organization = sampleService.getOrganizationRequester(sample,
                TableIdService.getInstance().REFERRING_ORG_TYPE_ID);
        Organization organizationDepartment = sampleService.getOrganizationRequester(sample,
                TableIdService.getInstance().REFERRING_ORG_DEPARTMENT_TYPE_ID);

        Test test = analysis.getTest();
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(analysis.getFhirUuidAsString());
        serviceRequest.addIdentifier(fhirTransformUtil.createIdentifier(fhirConfig.getOeFhirSystem() + "/analysis_uuid",
                analysis.getFhirUuidAsString()));
        Identifier facilityId = fhirTransformUtil.createFacilityIdentifier();
        if (facilityId != null) {
            serviceRequest.addIdentifier(facilityId);
        }
        serviceRequest.setRequisition(fhirTransformUtil.createIdentifier(fhirConfig.getOeFhirSystem() + "/samp_labNo",
                analysis.getSampleItem().getSample().getAccessionNumber()));
        if (organization != null) {
            serviceRequest.addLocationReference(
                    fhirTransformUtil.createReferenceFor(ResourceType.Location, organization.getFhirUuidAsString()));
        }
        if (organizationDepartment != null) {
            serviceRequest.addLocationReference(fhirTransformUtil.createReferenceFor(ResourceType.Location,
                    organizationDepartment.getFhirUuidAsString()));
        }

        List<ElectronicOrder> eOrders = electronicOrderService.getElectronicOrdersByExternalId(sample.getReferringId());

        if (eOrders.size() <= 0) {
            serviceRequest.setIntent(ServiceRequestIntent.ORIGINALORDER);
        } else if (ElectronicOrderType.FHIR.equals(eOrders.get(eOrders.size() - 1).getType())) {
            serviceRequest.addBasedOn(
                    fhirTransformUtil.createReferenceFor(ResourceType.ServiceRequest, sample.getReferringId()));
            serviceRequest.setIntent(ServiceRequestIntent.ORDER);
        } else if (ElectronicOrderType.HL7_V2.equals(eOrders.get(eOrders.size() - 1).getType())) {
            serviceRequest.setIntent(ServiceRequestIntent.ORDER);
        }

        if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.NotStarted))) {
            serviceRequest.setStatus(ServiceRequestStatus.ACTIVE);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance))) {
            serviceRequest.setStatus(ServiceRequestStatus.ACTIVE);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalRejected))) {
            serviceRequest.setStatus(ServiceRequestStatus.REVOKED);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.Finalized))) {
            serviceRequest.setStatus(ServiceRequestStatus.COMPLETED);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.Canceled))) {
            serviceRequest.setStatus(ServiceRequestStatus.REVOKED);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.SampleRejected))) {
            serviceRequest.setStatus(ServiceRequestStatus.ENTEREDINERROR);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.BiologistRejected))) {
            serviceRequest.setStatus(ServiceRequestStatus.ACTIVE);
        } else {
            serviceRequest.setStatus(ServiceRequestStatus.UNKNOWN);
        }
        ObservationHistory program = observationHistoryService.getObservationHistoriesBySampleIdAndType(sample.getId(),
                observationHistoryService.getObservationTypeIdForType(ObservationType.PROGRAM));
        if (program != null && !GenericValidator.isBlankOrNull(program.getValue())) {
            serviceRequest.addCategory(fhirTransformUtil.transformSampleProgramToCodeableConcept(program));
        }
        serviceRequest.setPriority(fhirTransformUtil.convertToServiceRequestPriority(sample.getPriority()));
        serviceRequest.setCode(fhirTransformUtil.transformTestToCodeableConcept(test.getId()));
        serviceRequest.setAuthoredOn(new Date());
        for (Note note : noteService.getNotes(analysis)) {
            serviceRequest.addNote(fhirTransformUtil.transformNoteToAnnotation(note));
        }
        // TODO performer type?

        serviceRequest.addSpecimen(fhirTransformUtil.createReferenceFor(ResourceType.Specimen,
                analysis.getSampleItem().getFhirUuidAsString()));
        // OGC-356: Environmental samples don't have a patient
        if (patient != null) {
            serviceRequest.setSubject(
                    fhirTransformUtil.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));
        }
        if (provider != null && provider.getFhirUuid() != null) {
            serviceRequest.setRequester(
                    fhirTransformUtil.createReferenceFor(ResourceType.Practitioner, provider.getFhirUuidAsString()));
        }

        return serviceRequest;
    }

    @Override
    public List<SampleEditItem> buildSampleEditItemsListFromServiceRequest(ServiceRequest serviceRequest,
            String sysUserId) throws Exception {

        List<SampleEditItem> items = new ArrayList<>();

        if (serviceRequest == null) {
            return items;
        }

        Analysis existingAnalysis = null;
        if (serviceRequest.hasId() && serviceRequest.getIdElement() != null) {
            String analysisUuid = serviceRequest.getIdElement().getIdPart();
            try {
                List<Analysis> analyses = analysisService.getAllMatching("fhirUuid", UUID.fromString(analysisUuid));
                if (!analyses.isEmpty()) {
                    existingAnalysis = analyses.get(0);
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e);
            }
        }

        SampleItem sampleItem = null;
        if (serviceRequest.hasSpecimen() && serviceRequest.getSpecimenFirstRep().hasReference()) {
            for (Reference reference : serviceRequest.getSpecimen()) {
                String specimenUUID = reference.getReferenceElement().getIdPart();
                try {
                    sampleItem = fhirTransformUtil.getItemByFhirId(specimenUUID, sampleItemService);
                    if (sampleItem != null) {
                        break;
                    }
                } catch (Exception e) {
                    throw new Exception("Error retrieving sample item for specimen reference: " + specimenUUID, e);
                }
            }
        }

        Test requestedTest = null;
        if (serviceRequest.hasCode()) {
            List<Test> foundTests = fhirTransformUtil.resolveTestsFromServiceRequest(serviceRequest);
            requestedTest = foundTests.get(0);
        }

        // Build edit item for existing analysis if available
        if (existingAnalysis != null && existingAnalysis.getTest() != null) {
            SampleEditItem existingItem = new SampleEditItem();

            existingItem.setAnalysisId(existingAnalysis.getId());
            existingItem.setTestId(existingAnalysis.getTest().getId());
            existingItem.setTestName(existingAnalysis.getTest().getLocalizedName());
            existingItem.setId(existingAnalysis.getTest().getId());
            existingItem.setSortOrder(existingAnalysis.getTest().getSortOrder());

            if (sampleItem != null) {
                existingItem.setSampleItemId(sampleItem.getId());
                if (sampleItem.getTypeOfSample() != null) {
                    existingItem.setSampleType(sampleItem.getTypeOfSample().getLocalizedName());
                }
                if (sampleItem.getSample() != null) {
                    existingItem.setAccessionNumber(sampleItem.getSample().getAccessionNumber());
                }
            } else if (existingAnalysis.getSampleItem() != null) {
                existingItem.setSampleItemId(existingAnalysis.getSampleItem().getId());
                if (existingAnalysis.getSampleItem().getTypeOfSample() != null) {
                    existingItem.setSampleType(existingAnalysis.getSampleItem().getTypeOfSample().getLocalizedName());
                }
                if (existingAnalysis.getSampleItem().getSample() != null) {
                    existingItem.setAccessionNumber(existingAnalysis.getSampleItem().getSample().getAccessionNumber());
                }
            }

            IStatusService statusService = SpringContext.getBean(IStatusService.class);
            if (existingAnalysis.getStatusId() != null) {
                existingItem.setStatus(statusService.getStatusNameFromId(existingAnalysis.getStatusId()));
                existingItem.setHasResults(
                        !statusService.matches(existingAnalysis.getStatusId(), AnalysisStatus.NotStarted));

                boolean canCancel = !statusService.matches(existingAnalysis.getStatusId(), AnalysisStatus.Canceled)
                        && statusService.matches(existingAnalysis.getStatusId(), AnalysisStatus.NotStarted);
                existingItem.setCanCancel(canCancel);
            }

            if (requestedTest != null && !existingAnalysis.getTest().getId().equals(requestedTest.getId())) {
                existingItem.setCanceled(true);
                existingItem.setAdd(false);
            } else {
                existingItem.setCanceled(false);
                existingItem.setAdd(false);
            }

            existingItem.setRemoveSample(false);
            existingItem.setSampleItemChanged(false);

            items.add(existingItem);
        }

        if (requestedTest != null
                && (existingAnalysis == null || !existingAnalysis.getTest().getId().equals(requestedTest.getId()))) {

            SampleEditItem newItem = new SampleEditItem();
            newItem.setTestId(requestedTest.getId());
            newItem.setTestName(requestedTest.getLocalizedName());
            newItem.setId(requestedTest.getId());
            newItem.setAdd(true);
            newItem.setCanceled(false);
            newItem.setSortOrder(requestedTest.getSortOrder());

            if (sampleItem != null) {
                newItem.setSampleItemId(sampleItem.getId());
                if (sampleItem.getTypeOfSample() != null) {
                    newItem.setSampleType(sampleItem.getTypeOfSample().getLocalizedName());
                }
                if (sampleItem.getSample() != null) {
                    newItem.setAccessionNumber(sampleItem.getSample().getAccessionNumber());
                }
            }

            IStatusService statusService = SpringContext.getBean(IStatusService.class);
            newItem.setStatus(statusService.getStatusNameFromId(statusService.getStatusID(AnalysisStatus.NotStarted)));
            newItem.setHasResults(false);
            newItem.setCanCancel(true);
            newItem.setRemoveSample(false);
            newItem.setSampleItemChanged(false);

            items.add(newItem);
        }

        return items;
    }

    @Override
    public SampleOrderItem buildSampleOrderItemFromServiceRequest(ServiceRequest serviceRequest, String sysUserId)
            throws Exception {

        SampleOrderItem orderItem = new SampleOrderItem();

        if (serviceRequest == null) {
            return orderItem;
        }

        Analysis analysis = null;
        if (serviceRequest.hasId() && serviceRequest.getIdElement() != null) {
            String analysisUuid = serviceRequest.getIdElement().getIdPart();
            try {
                List<Analysis> analyses = analysisService.getAllMatching("fhirUuid", UUID.fromString(analysisUuid));
                if (!analyses.isEmpty()) {
                    analysis = analyses.get(0);
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e);
            }
        }

        Sample sample = null;
        SampleItem sampleItem = null;

        if (serviceRequest.hasSpecimen() && serviceRequest.getSpecimenFirstRep().hasReference()) {
            for (Reference reference : serviceRequest.getSpecimen()) {
                String specimenUUID = reference.getReferenceElement().getIdPart();
                try {
                    sampleItem = fhirTransformUtil.getItemByFhirId(specimenUUID, sampleItemService);
                    if (sampleItem != null && sampleItem.getSample() != null) {
                        sample = sampleItem.getSample();
                        break;
                    }
                } catch (Exception e) {
                    throw new Exception("Error retrieving sample item for specimen reference: " + specimenUUID, e);
                }
            }
        }

        if (sample == null && analysis != null && analysis.getSampleItem() != null) {
            sampleItem = analysis.getSampleItem();
            sample = sampleItem.getSample();
        }

        if (sample != null) {
            orderItem.setSampleId(sample.getId());
            orderItem.setLabNo(sample.getAccessionNumber());
        }

        // Set specimen/requester sample ID from ServiceRequest
        if (serviceRequest.hasSpecimen() && serviceRequest.getSpecimenFirstRep().hasReference()) {
            String specimenUUID = serviceRequest.getSpecimenFirstRep().getReferenceElement().getIdPart();
            orderItem.setRequesterSampleID(specimenUUID);
        }

        if (serviceRequest.hasAuthoredOn()) {
            orderItem.setRequestDate(DateUtil.formatDateAsText(serviceRequest.getAuthoredOn()));
        }

        if (sample != null && sample.getReceivedDateForDisplay() != null) {
            orderItem.setReceivedDateForDisplay(sample.getReceivedDateForDisplay());
            orderItem.setReceivedTime(sample.getReceivedTimeForDisplay());
        } else {
            orderItem.setReceivedDateForDisplay(DateUtil.getCurrentDateAsText());
            orderItem.setReceivedTime("00:00");
        }

        if (serviceRequest.hasPriority()) {
            ServiceRequest.ServiceRequestPriority fhirPriority = serviceRequest.getPriority();
            OrderPriority priority = null;

            if (ServiceRequest.ServiceRequestPriority.STAT.equals(fhirPriority)) {
                priority = OrderPriority.STAT;
            } else if (ServiceRequest.ServiceRequestPriority.URGENT.equals(fhirPriority)
                    || ServiceRequest.ServiceRequestPriority.ASAP.equals(fhirPriority)) {
                priority = OrderPriority.TIMED;
            } else {
                priority = OrderPriority.ROUTINE;
            }

            orderItem.setPriority(priority);
        } else if (sample != null && sample.getPriority() != null) {
            orderItem.setPriority(sample.getPriority());
        } else {
            orderItem.setPriority(OrderPriority.ROUTINE);
        }

        if (serviceRequest.hasRequester() && serviceRequest.getRequester().hasReference()) {
            String requesterUUID = serviceRequest.getRequester().getReferenceElement().getIdPart();
            try {
                Provider provider = providerService.getProviderByFhirId(UUID.fromString(requesterUUID));
                if (provider != null) {
                    orderItem.setProviderId(provider.getId());
                    if (provider.getPerson() != null) {
                        orderItem.setProviderPersonId(provider.getPerson().getId());
                        orderItem.setProviderFirstName(provider.getPerson().getFirstName());
                        orderItem.setProviderLastName(provider.getPerson().getLastName());
                        orderItem.setProviderWorkPhone(provider.getPerson().getWorkPhone());
                        orderItem.setProviderFax(provider.getPerson().getFax());
                        orderItem.setProviderEmail(provider.getPerson().getEmail());
                    }
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e);
            }
        } else if (analysis != null && analysis.getSampleItem() != null) {
            SampleHuman curentSampleHuman = new SampleHuman();
            curentSampleHuman.setSampleId(analysis.getSampleItem().getSample().getId());
            SampleHuman sampleHuman = sampleHumanService.getDataBySample(curentSampleHuman);
            if (sampleHuman != null && sampleHuman.getProviderId() != null) {
                Provider provider = providerService.get(sampleHuman.getProviderId());
                if (provider != null) {
                    orderItem.setProviderId(provider.getId());
                    if (provider.getPerson() != null) {
                        orderItem.setProviderPersonId(provider.getPerson().getId());
                        orderItem.setProviderFirstName(provider.getPerson().getFirstName());
                        orderItem.setProviderLastName(provider.getPerson().getLastName());
                    }
                }
            }
        }

        if (serviceRequest.hasLocationReference()) {
            Reference locationRef = serviceRequest.getLocationReferenceFirstRep();
            if (locationRef.hasReference()) {
                String locationUUID = locationRef.getReferenceElement().getIdPart();
                try {
                    Organization organization = organizationService.getOrganizationByFhirId(locationUUID);
                    if (organization != null) {
                        orderItem.setReferringSiteId(organization.getId());
                        orderItem.setReferringSiteName(organization.getOrganizationName());
                        orderItem.setReferringSiteCode(organization.getCode());
                    }
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }

        if (analysis != null && analysis.getStatusId() != null) {
            IStatusService statusService = SpringContext.getBean(IStatusService.class);
            boolean isReadOnly = !statusService.matches(analysis.getStatusId(), AnalysisStatus.NotStarted);
            orderItem.setReadOnly(isReadOnly);
        }

        orderItem.setModified(analysis != null);

        return orderItem;
    }

}
