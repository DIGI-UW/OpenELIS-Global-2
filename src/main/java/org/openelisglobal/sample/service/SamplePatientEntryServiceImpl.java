package org.openelisglobal.sample.service;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.address.service.OrganizationAddressService;
import org.openelisglobal.address.valueholder.OrganizationAddress;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.barcode.service.BarcodeInfoService;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.compliance.service.ComplianceStandardService;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.eqa.service.EQAPanelReceiptService;
import org.openelisglobal.eqa.service.SampleEQAService;
import org.openelisglobal.eqa.valueholder.EQAPriority;
import org.openelisglobal.eqa.valueholder.SampleEQA;
import org.openelisglobal.labelpreset.dto.OrderLabelPersistRequest;
import org.openelisglobal.labelpreset.service.OrderLabelRequestService;
import org.openelisglobal.labelpreset.valueholder.OrderLabelRequest;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.notification.service.AnalysisNotificationConfigService;
import org.openelisglobal.notification.service.TestNotificationConfigService;
import org.openelisglobal.notification.valueholder.AnalysisNotificationConfig;
import org.openelisglobal.notification.valueholder.NotificationConfigOption;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationMethod;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationPersonType;
import org.openelisglobal.notification.valueholder.TestNotificationConfig;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.organization.service.OrganizationContactService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationContact;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.program.service.ImmunohistochemistrySampleService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sample.valueholder.SampleAdditionalField;
import org.openelisglobal.sample.valueholder.SampleComplianceStandard;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.dao.SampleItemDAO;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampletyperequest.dto.SampleTypeRequestDTO;
import org.openelisglobal.sampletyperequest.service.SampleTypeRequestService;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.EffectiveTestStatusService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SamplePatientEntryServiceImpl implements SamplePatientEntryService {

    private static final String DEFAULT_ANALYSIS_TYPE = "MANUAL";
    private final String SAMPLE_SUBJECT = "Sample Note";
    // private String currentUserId;

    @Autowired
    private org.openelisglobal.microbiology.service.MicroOrderRoutingService microOrderRoutingService;

    @Autowired
    private OrganizationAddressService organizationAddressService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private ObservationHistoryService observationHistoryService;
    @Autowired
    private PersonService personService;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private OrderProgressService orderProgressService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private SampleItemDAO sampleItemDAO;
    @Autowired
    private NoteService noteService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestService testService;
    @Autowired
    private EffectiveTestStatusService effectiveTestStatusService;
    @Autowired
    private SampleTypeRequestService sampleTypeRequestService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private UnitOfMeasureService unitOfMeasureService;
    @Autowired
    private SampleRequesterService sampleRequesterService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OrganizationContactService organizationContactService;
    @Autowired
    private TestNotificationConfigService testNotificationConfigService;
    @Autowired
    private AnalysisNotificationConfigService analysisNotificationConfigService;
    @Autowired
    private PathologySampleService pathologySampleService;
    @Autowired
    private ImmunohistochemistrySampleService immunohistochemistrySampleService;
    @Autowired
    private ProgramSampleService programSampleService;
    @Autowired
    private SampleEQAService sampleEQAService;
    @Autowired
    private EQAPanelReceiptService eqaPanelReceiptService;
    @Autowired
    private BarcodeInfoService barcodeInfoService;
    @Autowired
    private org.openelisglobal.qc.dao.SampleItemQcProfileDAO sampleItemQcProfileDAO;
    @Autowired
    private SampleComplianceStandardService sampleComplianceStandardService;
    @Autowired
    private ComplianceStandardService complianceStandardService;
    @Autowired
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Autowired
    private org.openelisglobal.vector.service.VectorPoolFanOutService vectorPoolFanOutService;
    @Autowired
    private org.openelisglobal.referral.service.ReferralSetService referralSetService;
    @Autowired
    private OrderLabelRequestService orderLabelRequestService;
    @Autowired
    private org.openelisglobal.questionnaire.service.QuestionnaireStorageService questionnaireStorageService;
    @Lazy
    @Autowired
    private ResultLimitService resultLimitService;
    @Lazy
    @Autowired
    private PanelItemService panelItemService;

    @Transactional(readOnly = true)
    @Override
    public List<String> getTestNamesWithRangeNotApplied(org.openelisglobal.sample.valueholder.Sample sample) {
        Patient patient = sample == null ? null : sampleHumanService.getPatientForSample(sample);
        if (patient == null) {
            return new ArrayList<>();
        }
        List<ResultLimitService.OrderedTest> ordered = new ArrayList<>();
        for (Analysis analysis : analysisService.getAnalysesBySampleId(sample.getId())) {
            if (analysis.getTest() != null) {
                ordered.add(new ResultLimitService.OrderedTest(analysis.getTest().getId(),
                        analysis.getSampleItem() == null ? null : analysis.getSampleItem().getTypeOfSampleId()));
            }
        }
        for (SampleTypeRequest request : sampleTypeRequestService.getPendingRequestsBySampleId(sample.getId())) {
            String sampleTypeId = request.getTypeOfSample() == null ? null : request.getTypeOfSample().getId();
            for (String testId : splitIds(request.getRequestedTests())) {
                ordered.add(new ResultLimitService.OrderedTest(testId, sampleTypeId));
            }
            for (String panelId : splitIds(request.getRequestedPanels())) {
                for (PanelItem item : panelItemService.getPanelItemsForPanel(panelId)) {
                    if (item.getTest() != null) {
                        ordered.add(new ResultLimitService.OrderedTest(item.getTest().getId(), sampleTypeId));
                    }
                }
            }
        }
        return resultLimitService.getTestNamesWithRangeNotApplied(ordered, patient);
    }

    private static List<String> splitIds(String ids) {
        List<String> split = new ArrayList<>();
        if (ids == null) {
            return split;
        }
        for (String id : ids.split(",")) {
            if (!id.trim().isEmpty()) {
                split.add(id.trim());
            }
        }
        return split;
    }

    @Transactional
    @Override
    public void persistData(SamplePatientUpdateData updateData, PatientManagementUpdate patientUpdate,
            PatientManagementInfo patientInfo, SamplePatientEntryForm form, HttpServletRequest request) {

        boolean useInitialSampleCondition = FormFields.getInstance().useField(Field.InitialSampleCondition);
        boolean useSampleNature = FormFields.getInstance().useField(Field.SampleNature);

        persistOrganizationData(updateData);

        if (updateData.isSavePatient()) {
            patientUpdate.persistPatientData(patientInfo);
        }

        updateData.setPatientId(patientUpdate.getPatientId(form));

        persistProviderData(updateData);
        persistRequestorContactData(updateData);
        persistSampleData(updateData);
        persistRequestedSampleTypes(updateData.getSample(), form.getRequestedSampleTypes(),
                updateData.getCurrentUserId());
        fulfillRequestedSampleTypes(updateData);

        // Only persist requester data and observations if sample was successfully
        // created
        if (updateData.getSample() != null && updateData.getSample().getId() != null) {
            if (updateData.isEqaSample()) {
                persistSampleEQAData(updateData);
            }
            persistRequesterData(updateData);

            if (useInitialSampleCondition) {
                persistInitialSampleConditions(updateData);
            }
            if (useSampleNature) {
                persistSampleNature(updateData);
            }

            persistObservations(updateData);
            persistComplianceStandards(updateData);
        }

        persistOrderEntryReferrals(updateData, form);
        recordStepProgress(updateData.getSample(), form.getSampleOrderItems());

        request.getSession().setAttribute("lastAccessionNumber", updateData.getAccessionNumber());
        request.getSession().setAttribute("lastPatientId", updateData.getPatientId());

        // Publish post-persist event INSIDE this @Transactional method so any
        // listener (e.g. SampleStorageAssignmentListener) runs while this tx
        // is still open. If a listener throws, Spring stamps the tx as
        // rollback-only and the entire persist (sample, patient, etc.) gets
        // rolled back — same mechanism Edit Order uses. Previously this was
        // published from the controller after persistData returned, so the
        // tx had already committed by the time listeners ran.
        eventPublisher.publishEvent(new org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent(this,
                updateData, patientInfo, form));
    }

    /**
     * The storage decision and the step's completion travel with the step's save
     * (OGC-1266 FR-A5): the order-level "storage skipped" flag is applied here
     * instead of by a separate call, and the order's progress status advances in
     * the same transaction as everything else the step saved.
     */
    private void recordStepProgress(Sample sample, SampleOrderItem sampleOrder) {
        if (sample == null || sample.getId() == null || sampleOrder == null) {
            return;
        }
        orderProgressService.recordStepSave(sample, sampleOrder.getProgressStep(), sampleOrder.getStorageSkipped());
    }

    /**
     * Persists order-entry "Refer Out / Subcontract" rows in the same transaction
     * as the sample save so referral rows are not lost when the async FHIR
     * transform listener throws. Sets {@code referralsPersistedSynchronously=true}
     * on success so the legacy async leg in
     * {@code FhirTransformServiceImpl.transformPersistOrderEntryFhirObjects}
     * short-circuits and does not double-write.
     */
    void persistOrderEntryReferrals(SamplePatientUpdateData updateData, SamplePatientEntryForm form) {
        if (!form.getUseReferral()) {
            return;
        }
        if (form.getReferralItems() == null || form.getReferralItems().isEmpty()) {
            return;
        }
        referralSetService.createDraftReferralSetsForOrderEntry(form.getReferralItems(), updateData);
        updateData.setReferralsPersistedSynchronously(true);
    }

    private void persistObservations(SamplePatientUpdateData updateData) {
        String patientId = updateData.getPatientId();
        String sampleId = updateData.getSample() != null ? updateData.getSample().getId() : null;

        // OGC-356: For environmental samples, sampleId is required but patientId can be
        // null
        // Only skip if sampleId is missing (required for observation association)
        if (GenericValidator.isBlankOrNull(sampleId)) {
            return;
        }

        for (String clearedTypeId : updateData.getClearedObservationTypeIds()) {
            ObservationHistory cleared = observationHistoryService.getObservationHistoriesBySampleIdAndType(sampleId,
                    clearedTypeId);
            if (cleared != null) {
                observationHistoryService.delete(cleared.getId(), updateData.getCurrentUserId());
            }
        }

        if (updateData.getObservations() == null || updateData.getObservations().isEmpty()) {
            return;
        }

        for (ObservationHistory observation : updateData.getObservations()) {
            observation.setSampleId(sampleId);
            // OGC-356: patientId can be null for environmental samples
            if (!GenericValidator.isBlankOrNull(patientId)) {
                observation.setPatientId(patientId);
            }

            // For updates, check if observation already exists for this sample/type
            // If so, update it instead of inserting a duplicate
            ObservationHistory existing = observationHistoryService.getObservationHistoriesBySampleIdAndType(sampleId,
                    observation.getObservationHistoryTypeId());

            if (existing != null) {
                existing.setValue(observation.getValue());
                existing.setSysUserId(observation.getSysUserId());
                observationHistoryService.update(existing);
            } else {
                observationHistoryService.insert(observation);
            }
        }
    }

    private void persistComplianceStandards(SamplePatientUpdateData updateData) {
        List<String> ids = updateData.getPendingComplianceStandardIds();
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String sampleId = updateData.getSample() != null ? updateData.getSample().getId() : null;
        if (GenericValidator.isBlankOrNull(sampleId)) {
            return;
        }
        List<SampleComplianceStandard> links = new ArrayList<>();
        int priority = 0;
        for (String standardId : ids) {
            ComplianceStandard standard = complianceStandardService.get(standardId);
            if (standard == null) {
                LogEvent.logWarn("SamplePatientEntryServiceImpl", "persistComplianceStandards",
                        "Compliance standard not found for id=" + standardId + ", skipping");
                continue;
            }
            SampleComplianceStandard link = new SampleComplianceStandard();
            link.setSample(updateData.getSample());
            link.setComplianceStandard(standard);
            link.setPriority(priority++);
            link.setSysUserId(updateData.getCurrentUserId());
            links.add(link);
        }
        sampleComplianceStandardService.replaceAllForSample(sampleId, links);
    }

    private void persistOrganizationData(SamplePatientUpdateData updateData) {
        Organization newOrganization = updateData.getNewOrganization();
        if (newOrganization != null) {
            organizationService.insert(newOrganization);
            organizationService.linkOrganizationAndType(newOrganization,
                    TableIdService.getInstance().REFERRING_ORG_TYPE_ID);
            if (updateData.getRequesterSite() != null) {
                updateData.getRequesterSite().setRequesterId(newOrganization.getId());
            }

            for (OrganizationAddress address : updateData.getOrgAddressExtra()) {
                address.setOrganizationId(newOrganization.getId());
                organizationAddressService.insert(address);
            }
        }

        if (updateData.getCurrentOrganization() != null) {
            organizationService.update(updateData.getCurrentOrganization());
        }
        // newOrganization = updateData.getNewOrganizationDepartment();
        // if (newOrganization != null) {
        // organizationService.insert(newOrganization);
        // organizationService.linkOrganizationAndType(newOrganization,
        // TableIdService.getInstance().REFERRING_ORG_TYPE_ID);
        // if (updateData.getRequesterSite() != null) {
        // updateData.getRequesterSite().setRequesterId(newOrganization.getId());
        // }
        //
        // for (OrganizationAddress address : updateData.getOrgAddressExtra()) {
        // address.setOrganizationId(newOrganization.getId());
        // organizationAddressService.insert(address);
        // }
        // }
        //
        // if (updateData.getCurrentOrganizationDepartment() != null) {
        // organizationService.update(updateData.getCurrentOrganizationDepartment());
        // }

    }

    private void persistProviderData(SamplePatientUpdateData updateData) {
        if (updateData.getProviderPerson() != null && updateData.getProvider() != null) {

            personService.save(updateData.getProviderPerson());
            updateData.getProvider().setPerson(updateData.getProviderPerson());

            providerService.save(updateData.getProvider());
        }
    }

    /**
     * Saves the standalone Requestor contact Person for Environmental/Vector orders
     * (built by {@code SamplePatientUpdateData.initRequestorContact}). Unlike the
     * Provider path, there is no wrapper entity — Requestor is persisted as a bare
     * Person and linked to the sample via a {@code requestor_contact}-typed
     * SampleRequester row in {@link #persistRequesterData}.
     */
    private void persistRequestorContactData(SamplePatientUpdateData updateData) {
        if (updateData.getRequestorPerson() != null) {
            personService.save(updateData.getRequestorPerson());
        }
    }

    private void persistSampleData(SamplePatientUpdateData updateData) {
        String analysisRevision = ConfigurationProperties.getInstance().getPropertyValue("analysis.default.revision");

        if (updateData.getSample() == null) {
            return;
        }

        // Set GPS data from first sample test collection if available
        if (updateData.getSampleItemsTests() != null && !updateData.getSampleItemsTests().isEmpty()) {
            SampleTestCollection firstSampleTest = updateData.getSampleItemsTests().getFirst();
            if (!GenericValidator.isBlankOrNull(firstSampleTest.gpsLatitude)) {
                try {
                    updateData.getSample().setGpsLatitude(Double.valueOf(firstSampleTest.gpsLatitude));
                } catch (NumberFormatException e) {
                    // ignore invalid GPS data
                }
            }
            if (!GenericValidator.isBlankOrNull(firstSampleTest.gpsLongitude)) {
                try {
                    updateData.getSample().setGpsLongitude(Double.valueOf(firstSampleTest.gpsLongitude));
                } catch (NumberFormatException e) {
                    // ignore invalid GPS data
                }
            }
            if (!GenericValidator.isBlankOrNull(firstSampleTest.gpsAccuracy)) {
                try {
                    updateData.getSample().setGpsAccuracyMeters(Integer.valueOf(firstSampleTest.gpsAccuracy));
                } catch (NumberFormatException e) {
                    // ignore invalid GPS data
                }
            }
            if (!GenericValidator.isBlankOrNull(firstSampleTest.gpsCaptureMethod)) {
                updateData.getSample().setGpsCaptureMethod(firstSampleTest.gpsCaptureMethod);
                updateData.getSample().setGpsCaptureTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
            }
        }

        // Check if this is an existing sample (edit) or new sample (insert)
        if (updateData.getSample().getId() != null) {
            // Update existing sample
            updateData.getSample().setPriority(updateData.getPriority());
            sampleService.update(updateData.getSample());
        } else {
            // Insert new sample - set priority BEFORE insert so it gets persisted
            if (updateData.getSample().getFhirUuid() == null) {
                updateData.getSample().setFhirUuid(UUID.randomUUID());
            }
            updateData.getSample().setPriority(updateData.getPriority());
            sampleService.insertDataWithAccessionNumber(updateData.getSample());
        }

        for (SampleAdditionalField field : updateData.getSampleFields()) {
            field.setSample(updateData.getSample());
            sampleService.saveSampleAdditionalField(field);
        }

        if (updateData.getProgramSample() != null) {
            // Only set new UUID if this is a new ProgramSample (no existing UUID)
            if (updateData.getProgramQuestionnaireResponse() != null
                    && updateData.getProgramSample().getQuestionnaireResponseUuid() == null) {
                updateData.getProgramSample().setQuestionnaireResponseUuid(UUID.randomUUID());
            }
            updateData.getProgramSample().setSample(updateData.getSample());

            if (updateData.getProgramSample() instanceof PathologySample) {
                pathologySampleService.save((PathologySample) updateData.getProgramSample());
            } else if (updateData.getProgramSample() instanceof ImmunohistochemistrySample) {
                immunohistochemistrySampleService.save((ImmunohistochemistrySample) updateData.getProgramSample());
            } else {
                programSampleService.save(updateData.getProgramSample());
            }
            if (updateData.getProgramQuestionnaireResponse() != null) {
                updateData.getProgramQuestionnaireResponse()
                        .setId(updateData.getProgramSample().getQuestionnaireResponseUuid().toString());
                questionnaireStorageService
                        .storeQuestionnaireResponseLocally(updateData.getProgramQuestionnaireResponse());
            }
        }

        // Process sample items and tests (may be empty in decoupled workflow)
        Map<SampleItem, Integer> specimenLabelQuantities = new LinkedHashMap<>();
        Integer orderLabelQuantity = null;
        matchRetriedSampleItems(updateData);
        prepareRequestedCollectionDetails(updateData);
        for (SampleTestCollection sampleTestCollection : updateData.getSampleItemsTests()) {
            SampleItem savedItem = null;
            String sampleItemId = null;

            // For updates, use the existingSampleItemId from the XML to find the correct
            // sample_item
            if (!GenericValidator.isBlankOrNull(sampleTestCollection.existingSampleItemId)) {
                savedItem = sampleItemService.get(sampleTestCollection.existingSampleItemId);
                if (savedItem != null) {
                    sampleItemId = savedItem.getId();
                    // Update existing sample item with new collection data
                    savedItem.setSysUserId(sampleTestCollection.item.getSysUserId());
                    // Copy collection details from the incoming item
                    savedItem.setCollectionDate(sampleTestCollection.item.getCollectionDate());
                    savedItem.setCollector(sampleTestCollection.item.getCollector());
                    savedItem.setQuantity(sampleTestCollection.item.getQuantity());
                    savedItem.setUnitOfMeasure(sampleTestCollection.item.getUnitOfMeasure());
                    savedItem.setCollectionConditions(sampleTestCollection.item.getCollectionConditions());
                    String storedContainer = savedItem.getContainer();
                    copyHandlingDetails(sampleTestCollection.item, savedItem);
                    if (!sampleTestCollection.suppliedCollectionFields.contains("container")) {
                        savedItem.setContainer(storedContainer);
                    }
                    savedItem.setReceivedDate(sampleTestCollection.item.getReceivedDate());
                    if (sampleTestCollection.suppliedCollectionFields.contains("cultureSetNumber")) {
                        savedItem.setCultureSetNumber(sampleTestCollection.item.getCultureSetNumber());
                    }
                    if (sampleTestCollection.suppliedCollectionFields.contains("bodySite")) {
                        savedItem.setBodySite(sampleTestCollection.item.getBodySite());
                    }
                    savedItem.setLabPerformedSampling(sampleTestCollection.item.isLabPerformedSampling());
                    // Keep existing typeOfSample if incoming is null (don't change sample type
                    // during collection)
                    if (sampleTestCollection.item.getTypeOfSample() != null) {
                        savedItem.setTypeOfSample(sampleTestCollection.item.getTypeOfSample());
                    }
                    // Use DAO directly to bypass the service layer's audit trail evict/merge
                    // which can cause state loss when the same entity instance is fetched twice
                    savedItem = sampleItemDAO.update(savedItem);
                } else {
                    LogEvent.logWarn(this.getClass().getName(), "persistSampleData",
                            "Could not find existing sample item with ID: "
                                    + sampleTestCollection.existingSampleItemId);
                }
            }

            // If no existing sample item, create new one
            if (savedItem == null) {
                if (GenericValidator.isBlankOrNull(sampleTestCollection.item.getFhirUuidAsString())) {
                    sampleTestCollection.item.setFhirUuid(UUID.randomUUID());
                }
                sampleItemId = sampleItemService.insert(sampleTestCollection.item);
                savedItem = sampleItemService.get(sampleItemId);
            }

            // Track label quantities
            specimenLabelQuantities.put(savedItem, sampleTestCollection.numSpecimenLabels);
            if (orderLabelQuantity == null) {
                orderLabelQuantity = sampleTestCollection.numOrderLabels;
            }

            // IMPORTANT: Update the sampleTestCollection.item reference to the managed
            // entity
            // This prevents "transient instance" errors when creating Analysis objects
            sampleTestCollection.item = savedItem;
            if (!"V".equals(updateData.getSample().getDomain())) {
                bindRequestedCollection(sampleTestCollection, updateData);
            }

            // Create QC profile if this sample item is a QC sample (OGC-554)
            if (!GenericValidator.isBlankOrNull(sampleTestCollection.qcType)) {
                createQcProfile(sampleTestCollection, updateData, savedItem);
            }

            if (savedItem.isRejected()) {
                String rejectReasonId = savedItem.getRejectReasonId();
                String currentUserId = savedItem.getSysUserId();
                for (IdValuePair rejectReason : DisplayListService.getInstance().getList(ListType.REJECTION_REASONS)) {
                    if (rejectReasonId.equals(rejectReason.getId())) {
                        Note note = noteService.createSavableNote(savedItem, NoteType.REJECTION_REASON,
                                rejectReason.getValue(), SAMPLE_SUBJECT, currentUserId);
                        noteService.insert(note);
                        break;
                    }
                }
            }
            sampleTestCollection.analysises = new ArrayList<>();
            for (Test test : sampleTestCollection.tests) {
                test = testService.get(test.getId());

                if (test.isOpensMicrobiologyCase() && "CASE".equals(test.getMicrobiologyCaseRole())) {
                    microOrderRoutingService.routeCaseTest(savedItem, test, updateData.getCurrentUserId());
                    continue;
                }

                // Check if analysis already exists for this sample item + test (for updates)
                Analysis existingAnalysis = analysisService.getAnalysisBySampleItemAndTest(savedItem.getId(),
                        test.getId());
                if (existingAnalysis != null) {
                    sampleTestCollection.analysises.add(existingAnalysis);
                    continue;
                }

                // OGC-189 (M4): a deactivated lab unit takes no new work. The
                // manual picker already filters these out, so reaching here
                // means a stale form, a saved draft, or an API caller — the
                // server is the authority (D1), not the UI. Existing analyses
                // are handled above and never blocked (D3).
                if (!effectiveTestStatusService.isEffectivelyActive(test)) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "persistAnalyses",
                            "Order skipped: no analysis created for test id " + test.getId()
                                    + " because its lab unit is inactive (OGC-189).");
                    continue;
                }

                Analysis analysis = populateAnalysis(analysisRevision, sampleTestCollection, test,
                        sampleTestCollection.testIdToUserSectionMap.get(test.getId()),
                        sampleTestCollection.testIdToUserSampleTypeMap.get(test.getId()), updateData);
                analysisService.insert(analysis);
                sampleTestCollection.analysises.add(analysis);

                if (updateData.getCustomNotificationLogic()) {
                    persistAnalysisNotificationConfigs(analysis, updateData);
                }
            }
        }

        org.openelisglobal.sample.valueholder.Sample submittedSample = updateData.getSample();
        boolean isVectorOrder = submittedSample != null && "V".equals(submittedSample.getDomain());
        if (isVectorOrder) {
            String sysUserId = updateData.getCurrentUserId();
            for (SampleTestCollection stc : updateData.getSampleItemsTests()) {
                if (stc.item == null || stc.item.getId() == null) {
                    continue;
                }
                Double itemQty = stc.item.getQuantity();
                int poolCount = itemQty == null ? 0 : itemQty.intValue();
                if (poolCount <= 1) {
                    bindRequestedCollection(stc, updateData);
                    continue;
                }
                List<SampleItem> siblings = vectorPoolFanOutService.fanOut(stc.item, stc.analysises, poolCount,
                        sysUserId);
                if (siblings.isEmpty()) {
                    bindRequestedCollection(stc, updateData);
                    continue;
                }
                // Parent is hard-deleted by fanOut; siblings get the labels instead.
                specimenLabelQuantities.remove(stc.item);
                for (SampleItem sibling : siblings) {
                    specimenLabelQuantities.put(sibling, 1);
                }
                stc.item = siblings.getFirst();
                bindRequestedCollection(stc, updateData);
            }
        }

        persistOrderSpecimenBarcodeCounts(updateData.getSample(), orderLabelQuantity, specimenLabelQuantities);
        updateData.buildSampleHuman();

        // Check if SampleHuman already exists for this sample (edit case)
        SampleHuman lookupSampleHuman = new SampleHuman();
        lookupSampleHuman.setSampleId(updateData.getSample().getId());
        SampleHuman existingSampleHuman = sampleHumanService.getDataBySample(lookupSampleHuman);

        if (existingSampleHuman != null && existingSampleHuman.getId() != null) {
            // Update existing SampleHuman
            existingSampleHuman.setPatientId(updateData.getSampleHuman().getPatientId());
            existingSampleHuman.setProviderId(updateData.getSampleHuman().getProviderId());
            existingSampleHuman.setSysUserId(updateData.getSampleHuman().getSysUserId());
            sampleHumanService.update(existingSampleHuman);
        } else {
            // Insert new SampleHuman
            sampleHumanService.insert(updateData.getSampleHuman());
        }

        if (updateData.getElectronicOrder() != null) {
            electronicOrderService.update(updateData.getElectronicOrder());
        }
    }

    // Package-private so the EQA order/receipt wiring can be exercised directly
    // (same reason as persistOrderSpecimenBarcodeCounts below).
    void persistSampleEQAData(SamplePatientUpdateData updateData) {
        SampleEQA sampleEQA = new SampleEQA();
        sampleEQA.setSampleId(Long.parseLong(updateData.getSample().getId()));
        sampleEQA.setIsEqaSample(true);
        sampleEQA.setSysUserId(updateData.getCurrentUserId());

        Long enrollmentId = null;
        if (!GenericValidator.isBlankOrNull(updateData.getEqaProgramId())) {
            enrollmentId = Long.parseLong(updateData.getEqaProgramId());
            sampleEQA.setEqaEnrollmentId(enrollmentId);
        }
        sampleEQA.setEqaProviderSampleId(updateData.getEqaProviderSampleId());

        // The cycle link is optional: an order without one stays
        // visible in the uncycled bucket rather than being rejected outright.
        Long cycleId = null;
        if (!GenericValidator.isBlankOrNull(updateData.getEqaCycleId())) {
            cycleId = Long.parseLong(updateData.getEqaCycleId());
            sampleEQA.setCycleId(cycleId);
        }

        if (!GenericValidator.isBlankOrNull(updateData.getEqaDeadline())) {
            java.sql.Date deadlineDate = DateUtil.convertStringDateToSqlDate(updateData.getEqaDeadline());
            if (deadlineDate != null) {
                sampleEQA.setEqaDeadline(new Timestamp(deadlineDate.getTime()));
            }
        }
        if (!GenericValidator.isBlankOrNull(updateData.getEqaPriority())) {
            sampleEQA.setEqaPriority(EQAPriority.valueOf(updateData.getEqaPriority()));
        }

        sampleEQAService.insert(sampleEQA);

        // The panel receipt rides this order's transaction on purpose —
        // a receipt that cannot be recorded must take the whole order down with it.
        // recordReceipt is idempotent, so re-saving the same cycle is a read.
        if (cycleId != null && enrollmentId != null) {
            BigDecimal receivedTempC = GenericValidator.isBlankOrNull(updateData.getEqaReceivedTempC()) ? null
                    : new BigDecimal(updateData.getEqaReceivedTempC());
            Integer shippingBoxId = GenericValidator.isBlankOrNull(updateData.getEqaShippingBoxId()) ? null
                    : Integer.valueOf(updateData.getEqaShippingBoxId());
            eqaPanelReceiptService.recordReceipt(cycleId, enrollmentId, null, shippingBoxId, receivedTempC,
                    updateData.getEqaIntegrityOk(), updateData.getEqaIntegrityNotes(),
                    Long.valueOf(updateData.getCurrentUserId()), updateData.getCurrentUserId());
        }
    }

    void persistOrderSpecimenBarcodeCounts(org.openelisglobal.sample.valueholder.Sample sample, Integer numOrderLabels,
            Map<SampleItem, Integer> specimenLabelQuantities) {
        if (sample == null) {
            return;
        }

        int normalizedOrderLabels = normalizeLabelQuantity(numOrderLabels);
        Map<SampleItem, Integer> normalizedSpecimenLabelQuantities = new LinkedHashMap<>();
        if (specimenLabelQuantities != null) {
            for (Map.Entry<SampleItem, Integer> entry : specimenLabelQuantities.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                normalizedSpecimenLabelQuantities.put(entry.getKey(), normalizeLabelQuantity(entry.getValue()));
            }
        }
        barcodeInfoService.saveBarcodeInfoForSampleAndSampleItems(sample, normalizedOrderLabels,
                normalizedSpecimenLabelQuantities);
    }

    private int normalizeLabelQuantity(Integer quantity) {
        return quantity != null && quantity > 0 ? quantity : 1;
    }

    /**
     * Saves the specimens requested at order entry alongside the order itself, in
     * the same transaction. Re-saving reuses the pending rows in order, so
     * repeating a save updates them in place, and specimens removed from the order
     * are cancelled rather than left behind. A save that does not carry the list at
     * all, such as one made from a later stage, leaves the specimens untouched.
     */
    private void persistRequestedSampleTypes(Sample sample, List<SampleTypeRequestDTO> requestedSampleTypes,
            String currentUserId) {
        if (sample == null || sample.getId() == null || requestedSampleTypes == null) {
            return;
        }

        // A specimen already collected still satisfies its entry in the list, so it
        // is paired and left alone rather than requested a second time.
        List<SampleTypeRequest> reusable = new ArrayList<>();
        for (SampleTypeRequest existing : sampleTypeRequestService.getRequestsBySampleId(sample.getId())) {
            if (existing.getStatus() != SampleTypeRequest.Status.CANCELLED) {
                reusable.add(existing);
            }
        }

        int sortOrder = 0;
        for (SampleTypeRequestDTO requested : requestedSampleTypes) {
            if (requested == null || GenericValidator.isBlankOrNull(requested.getTypeOfSampleId())) {
                continue;
            }
            TypeOfSample typeOfSample = typeOfSampleService.getTypeOfSampleById(requested.getTypeOfSampleId());
            if (typeOfSample == null) {
                throw new IllegalArgumentException("Unknown requested sample type: " + requested.getTypeOfSampleId());
            }

            // Matching the specimen rather than the position keeps a cancellation
            // pointing at the specimen that was actually taken off the order.
            SampleTypeRequest request;
            if (!GenericValidator.isBlankOrNull(requested.getId())) {
                Integer requestId = Integer.valueOf(requested.getId());
                request = reusable.stream().filter(row -> requestId.equals(row.getId())).findFirst().orElseThrow(
                        () -> new IllegalArgumentException("Requested specimen does not belong to this order"));
                if (!typeOfSample.getId().equals(request.getTypeOfSample().getId())) {
                    throw new IllegalArgumentException("A requested specimen cannot change sample type");
                }
                reusable.remove(request);
            } else if (!GenericValidator.isBlankOrNull(requested.getSampleItemId())) {
                request = reusable.stream()
                        .filter(row -> row.getSampleItem() != null
                                && requested.getSampleItemId().equals(row.getSampleItem().getId()))
                        .findFirst().orElse(null);
                if (request != null)
                    reusable.remove(request);
            } else {
                request = takePendingRequestFor(reusable, typeOfSample);
            }
            if (request != null && request.getStatus() == SampleTypeRequest.Status.COLLECTED) {
                sortOrder++;
                continue;
            }
            boolean isNew = request == null;
            if (isNew) {
                request = new SampleTypeRequest();
                request.setSample(sample);
                request.setCreatedDate(new Timestamp(System.currentTimeMillis()));
            }
            request.setTypeOfSample(typeOfSample);
            request.setSortOrder(sortOrder);
            request.setRequestedQuantity(requested.getRequestedQuantity());
            request.setCultureSetNumber(requested.getCultureSetNumber());
            request.setContainer(requested.getContainer());
            request.setBodySite(requested.getBodySite());
            request.setCollectionDate(GenericValidator.isBlankOrNull(requested.getCollectionDate()) ? null
                    : java.sql.Date.valueOf(java.time.LocalDate.parse(requested.getCollectionDate())));
            request.setCollectionTime(GenericValidator.isBlankOrNull(requested.getCollectionTime()) ? null
                    : java.time.LocalTime.parse(requested.getCollectionTime()).toString());
            request.setRequestedTests(requested.getRequestedTests());
            request.setRequestedPanels(requested.getRequestedPanels());
            request.setStatus(SampleTypeRequest.Status.REQUESTED);
            request.setSysUserId(currentUserId);
            request.setUnitOfMeasure(GenericValidator.isBlankOrNull(requested.getUnitOfMeasureId()) ? null
                    : unitOfMeasureService.get(requested.getUnitOfMeasureId()));

            if (isNew) {
                sampleTypeRequestService.insert(request);
            } else {
                sampleTypeRequestService.update(request);
            }
            sortOrder++;
        }

        for (SampleTypeRequest removed : reusable) {
            if (removed.getStatus() == SampleTypeRequest.Status.REQUESTED) {
                sampleTypeRequestService.cancelRequest(removed.getId());
            }
        }
    }

    /**
     * A sample row carries a client key that becomes its sample item's FHIR UUID.
     * When a save that created sample items is retried after its reply was lost,
     * the rows still have no sample item id; matching them to the items the first
     * attempt created by that key turns the retry into an update instead of a
     * second set of samples.
     */
    void matchRetriedSampleItems(SamplePatientUpdateData updateData) {
        Sample sample = updateData.getSample();
        if (sample == null || sample.getId() == null || updateData.getSampleItemsTests() == null) {
            return;
        }
        Set<String> claimedItemIds = new HashSet<>();
        boolean hasUnmatchedKey = false;
        for (SampleTestCollection collection : updateData.getSampleItemsTests()) {
            if (!GenericValidator.isBlankOrNull(collection.existingSampleItemId)) {
                claimedItemIds.add(collection.existingSampleItemId);
            } else if (collection.item != null && collection.item.getFhirUuid() != null) {
                hasUnmatchedKey = true;
            }
        }
        if (!hasUnmatchedKey) {
            return;
        }
        List<SampleItem> existingItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
        if (existingItems == null || existingItems.isEmpty()) {
            return;
        }
        for (SampleTestCollection collection : updateData.getSampleItemsTests()) {
            if (!GenericValidator.isBlankOrNull(collection.existingSampleItemId) || collection.item == null
                    || collection.item.getFhirUuid() == null) {
                continue;
            }
            for (SampleItem existing : existingItems) {
                if (collection.item.getFhirUuid().equals(existing.getFhirUuid())
                        && claimedItemIds.add(existing.getId())) {
                    collection.existingSampleItemId = existing.getId();
                    break;
                }
            }
        }
    }

    /**
     * Preserve a pending specimen's identity and recorded details before creating
     * its sample.
     */
    private void prepareRequestedCollectionDetails(SamplePatientUpdateData updateData) {
        List<SampleTypeRequest> requests = sampleTypeRequestService
                .getRequestsBySampleId(updateData.getSample().getId());
        Set<Integer> claimed = new HashSet<>();
        for (SampleTestCollection collection : updateData.getSampleItemsTests()) {
            if (collection.item == null)
                continue;
            SampleTypeRequest request = null;
            if (collection.sampleTypeRequestId != null) {
                request = requests.stream().filter(row -> collection.sampleTypeRequestId.equals(row.getId()))
                        .findFirst().orElseThrow(
                                () -> new IllegalArgumentException("Requested specimen does not belong to this order"));
                if (!claimed.add(request.getId()))
                    throw new IllegalArgumentException("Requested specimen is repeated");
                if (request.getStatus() == SampleTypeRequest.Status.CANCELLED
                        || !request.getTypeOfSample().getId().equals(collection.item.getTypeOfSampleId())
                        || (request.getSampleItem() != null
                                && !request.getSampleItem().getId().equals(collection.existingSampleItemId))) {
                    throw new IllegalArgumentException("Requested specimen cannot be attached to this sample");
                }
            } else if (GenericValidator.isBlankOrNull(collection.existingSampleItemId)) {
                List<SampleTypeRequest> matching = requests.stream()
                        .filter(row -> row.getStatus() == SampleTypeRequest.Status.REQUESTED
                                && !claimed.contains(row.getId())
                                && row.getTypeOfSample().getId().equals(collection.item.getTypeOfSampleId()))
                        .toList();
                if (matching.size() == 1) {
                    request = matching.getFirst();
                    claimed.add(request.getId());
                    collection.sampleTypeRequestId = request.getId();
                }
            }
            if (request == null)
                continue;
            if (!collection.suppliedCollectionFields.contains("cultureSetNumber")) {
                collection.item.setCultureSetNumber(request.getCultureSetNumber());
            }
            if (!collection.suppliedCollectionFields.contains("container"))
                collection.item.setContainer(request.getContainer());
            if (!collection.suppliedCollectionFields.contains("bodySite"))
                collection.item.setBodySite(request.getBodySite());
            // A timestamp is available only when both its date and time were recorded.
            if (!collection.suppliedCollectionFields.contains("date")
                    && !collection.suppliedCollectionFields.contains("time") && request.getCollectionDate() != null
                    && request.getCollectionTime() != null) {
                collection.item.setCollectionDate(Timestamp.valueOf(request.getCollectionDate().toLocalDate()
                        .atTime(java.time.LocalTime.parse(request.getCollectionTime()))));
            }
        }
    }

    private void bindRequestedCollection(SampleTestCollection collection, SamplePatientUpdateData updateData) {
        if (collection.sampleTypeRequestId == null || collection.item.isRejected())
            return;
        SampleTypeRequest request = sampleTypeRequestService.get(collection.sampleTypeRequestId);
        if (request.getStatus() == SampleTypeRequest.Status.COLLECTED)
            return;
        request.setStatus(SampleTypeRequest.Status.COLLECTED);
        request.setSampleItem(collection.item);
        request.setSysUserId(updateData.getCurrentUserId());
        sampleTypeRequestService.update(request);
    }

    /**
     * Marks a requested specimen as collected once a sample of its type is saved on
     * the order, and links it to that sample. The samples are read back from the
     * order rather than taken from the save, because a save can replace the sample
     * it created (a vector pool is fanned out into one sample per organism). Each
     * sample fulfils at most one request, and a sample already linked to a request
     * is not counted again, so repeating a save does not fulfil a second request of
     * the same type.
     */
    void fulfillRequestedSampleTypes(SamplePatientUpdateData updateData) {
        Sample sample = updateData.getSample();
        if (sample == null || sample.getId() == null) {
            return;
        }
        List<SampleTypeRequest> requests = sampleTypeRequestService.getRequestsBySampleId(sample.getId());
        if (requests == null || requests.isEmpty()) {
            return;
        }
        Set<String> linkedItemIds = new HashSet<>();
        List<SampleTypeRequest> pending = new ArrayList<>();
        for (SampleTypeRequest request : requests) {
            if (request.getSampleItem() != null && request.getSampleItem().getId() != null) {
                linkedItemIds.add(request.getSampleItem().getId());
            }
            if (request.getStatus() == SampleTypeRequest.Status.REQUESTED && request.getTypeOfSample() != null) {
                pending.add(request);
            }
        }
        if (pending.isEmpty()) {
            return;
        }
        pending.sort(
                Comparator.comparing(SampleTypeRequest::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())));

        List<SampleItem> items = new ArrayList<>(sampleItemService.getSampleItemsBySampleId(sample.getId()));
        items.sort(Comparator.comparing(item -> Integer.valueOf(item.getId())));
        for (SampleItem item : items) {
            if (item.isRejected() || item.getTypeOfSample() == null || linkedItemIds.contains(item.getId())) {
                continue;
            }
            SampleTypeRequest request = takePendingRequestFor(pending, item.getTypeOfSample());
            if (request == null) {
                continue;
            }
            request.setStatus(SampleTypeRequest.Status.COLLECTED);
            request.setSampleItem(item);
            request.setSysUserId(updateData.getCurrentUserId());
            sampleTypeRequestService.update(request);
            linkedItemIds.add(item.getId());
        }
    }

    private SampleTypeRequest takePendingRequestFor(List<SampleTypeRequest> reusable, TypeOfSample typeOfSample) {
        for (Iterator<SampleTypeRequest> pending = reusable.iterator(); pending.hasNext();) {
            SampleTypeRequest request = pending.next();
            if (request.getTypeOfSample() != null && typeOfSample.getId().equals(request.getTypeOfSample().getId())) {
                pending.remove();
                return request;
            }
        }
        return null;
    }

    /**
     * Carries the collection and handling details a step save sends for a sample
     * that already exists. Before OGC-1424 only some of them were copied, so an
     * edited collection method, GPS position or legacy temperature on a saved
     * sample was silently dropped. A chosen receiver is stored; a receiver that
     * only defaulted to the saving user is stored when the receipt is recorded in
     * this save, never on a sample received earlier. Called before the receipt date
     * is copied. The arrival keeps its original recorder and time while the
     * condition and temperature are unchanged, and a rejected temperature never
     * clears the stored one.
     */
    static void copyHandlingDetails(SampleItem incoming, SampleItem saved) {
        saved.setCollectionMethod(incoming.getCollectionMethod());
        saved.setSampleTemperature(incoming.getSampleTemperature());
        saved.setSpecimenOrigin(incoming.getSpecimenOrigin());
        saved.setContainer(incoming.getContainer());
        saved.setLocationDetails(incoming.getLocationDetails());
        saved.setGpsLatitude(incoming.getGpsLatitude());
        saved.setGpsLongitude(incoming.getGpsLongitude());
        boolean receiptRecordedNow = saved.getReceivedDate() == null && incoming.getReceivedDate() != null;
        if (incoming.getReceivedById() != null && (!incoming.isReceivedByDefaulted() || receiptRecordedNow)) {
            saved.setReceivedById(incoming.getReceivedById());
        }
        java.math.BigDecimal temperature = incoming.isArrivalTemperatureRejected() ? saved.getArrivalTemperature()
                : incoming.getArrivalTemperature();
        boolean arrivalChanged = !java.util.Objects.equals(incoming.getArrivalCondition(), saved.getArrivalCondition())
                || !sameTemperature(temperature, saved.getArrivalTemperature());
        if (arrivalChanged) {
            saved.setArrivalCondition(incoming.getArrivalCondition());
            saved.setArrivalTemperature(temperature);
            saved.setArrivalRecordedById(incoming.getArrivalRecordedById());
            saved.setArrivalRecordedAt(incoming.getArrivalRecordedAt());
        }
    }

    private static boolean sameTemperature(java.math.BigDecimal a, java.math.BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    /*
     * private void persistSampleProject() throws LIMSRuntimeException {
     * SampleProjectDAO sampleProjectDAO = new SampleProjectDAOImpl(); ProjectDAO
     * projectDAO = new ProjectDAOImpl(); Project project = new Project(); //
     * project.setId(projectId); projectDAO.getData(project);
     *
     * SampleProject sampleProject = new SampleProject();
     * sampleProject.setProject(project); sampleProject.setSample(sample);
     * sampleProject.setSysUserId(getSysUserId(request));
     * sampleProjectDAO.insertData(sampleProject); }
     */

    private void persistAnalysisNotificationConfigs(Analysis analysis, SamplePatientUpdateData updateData) {
        Optional<TestNotificationConfig> testNotificationConfig = testNotificationConfigService
                .getTestNotificationConfigForTestId(analysis.getTest().getId());
        AnalysisNotificationConfig analysisNotificationConfig = new AnalysisNotificationConfig();
        analysisNotificationConfig.setAnalysis(analysis);
        if (testNotificationConfig.isPresent()) {
            analysisNotificationConfig
                    .setDefaultPayloadTemplate(testNotificationConfig.get().getDefaultPayloadTemplate());
        }

        this.persistAnalysisNotificationConfig(analysis, updateData.getPatientEmailNotificationTestIds(),
                analysisNotificationConfig, testNotificationConfig, NotificationMethod.EMAIL,
                NotificationPersonType.PATIENT);
        this.persistAnalysisNotificationConfig(analysis, updateData.getPatientSMSNotificationTestIds(),
                analysisNotificationConfig, testNotificationConfig, NotificationMethod.SMS,
                NotificationPersonType.PATIENT);
        this.persistAnalysisNotificationConfig(analysis, updateData.getProviderEmailNotificationTestIds(),
                analysisNotificationConfig, testNotificationConfig, NotificationMethod.EMAIL,
                NotificationPersonType.PROVIDER);
        this.persistAnalysisNotificationConfig(analysis, updateData.getProviderSMSNotificationTestIds(),
                analysisNotificationConfig, testNotificationConfig, NotificationMethod.SMS,
                NotificationPersonType.PROVIDER);
        analysisNotificationConfigService.save(analysisNotificationConfig);
    }

    private void persistAnalysisNotificationConfig(Analysis analysis, List<String> testIds,
            AnalysisNotificationConfig analysisNotificationConfig,
            Optional<TestNotificationConfig> testNotificationConfig, NotificationMethod method,
            NotificationPersonType personType) {
        NotificationNature notificationNature = NotificationNature.RESULT_VALIDATION;
        NotificationConfigOption nto = analysisNotificationConfig.getOptionFor(notificationNature, method, personType);
        nto.setNotificationMethod(method);
        nto.setNotificationNature(notificationNature);
        nto.setNotificationPersonType(personType);
        if (testIds.contains(analysis.getTest().getId())) {
            nto.setActive(true);
        } else {
            nto.setActive(false);
        }

        if (testNotificationConfig.isPresent()) {
            NotificationConfigOption nto2 = testNotificationConfig.get().getOptionFor(notificationNature, method,
                    personType);
            nto.setPayloadTemplate(nto2.getPayloadTemplate());
            nto.setAdditionalContacts(new ArrayList<>());
            nto.getAdditionalContacts().addAll(nto2.getAdditionalContacts());
        }
    }

    /**
     * The site and department rows already linking this order to organizations. A
     * later step saves the same site again, and inserting it every time left one
     * duplicate row per save.
     */
    private List<SampleRequester> organizationRequestersOf(SamplePatientUpdateData updateData) {
        if (updateData.getSample() == null || updateData.getSample().getId() == null) {
            return new ArrayList<>();
        }
        long organizationTypeId = TableIdService.getInstance().ORGANIZATION_REQUESTER_TYPE_ID;
        List<SampleRequester> rows = new ArrayList<>();
        for (SampleRequester requester : sampleRequesterService
                .getRequestersForSampleId(updateData.getSample().getId())) {
            if (requester.getRequesterTypeId() == organizationTypeId) {
                rows.add(requester);
            }
        }
        return rows;
    }

    private boolean isLinked(List<SampleRequester> requesters, long organizationId) {
        return requesters.stream().anyMatch(requester -> requester.getRequesterId() == organizationId);
    }

    void persistRequesterData(SamplePatientUpdateData updateData) {
        if (updateData.getProviderPerson() != null && !org.apache.commons.validator.GenericValidator
                .isBlankOrNull(updateData.getProviderPerson().getId())) {
            // Bug (2026-07-06, same class as the Requestor fix): this used to
            // unconditionally insert() a new sample_requester row on every
            // save, including edits to an order that already had a
            // provider-typed row — duplicating the link on every re-save.
            // Reuse the existing row for this sample if one exists; only
            // insert when there truly isn't one yet.
            long providerTypeId = TableIdService.getInstance().PROVIDER_REQUESTER_TYPE_ID;
            SampleRequester existingProviderRequester = sampleRequesterService
                    .getRequestersForSampleId(updateData.getSample().getId()).stream()
                    .filter(r -> r.getRequesterTypeId() == providerTypeId).findFirst().orElse(null);

            if (existingProviderRequester != null) {
                existingProviderRequester.setRequesterId(updateData.getProviderPerson().getId());
                existingProviderRequester.setSysUserId(updateData.getCurrentUserId());
                sampleRequesterService.update(existingProviderRequester);
            } else {
                SampleRequester sampleRequester = new SampleRequester();
                sampleRequester.setRequesterId(updateData.getProviderPerson().getId());
                sampleRequester.setRequesterTypeId(providerTypeId);
                sampleRequester.setSampleId(Long.parseLong(updateData.getSample().getId()));
                sampleRequester.setSysUserId(updateData.getCurrentUserId());
                sampleRequesterService.insert(sampleRequester);
            }
        }

        List<SampleRequester> existingOrganizationRequesters = organizationRequestersOf(updateData);
        if (updateData.getRequesterSite() != null) {
            updateData.getRequesterSite().setSampleId(Long.parseLong(updateData.getSample().getId()));
            if (updateData.getNewOrganization() != null) {
                updateData.getRequesterSite().setRequesterId(updateData.getNewOrganization().getId());
            }
            Set<Long> linkedOrganizations = new HashSet<>();
            linkedOrganizations.add(updateData.getRequesterSite().getRequesterId());
            if (updateData.getRequesterSiteDepartment() != null) {
                linkedOrganizations.add(updateData.getRequesterSiteDepartment().getRequesterId());
            }
            for (Iterator<SampleRequester> stale = existingOrganizationRequesters.iterator(); stale.hasNext();) {
                SampleRequester existing = stale.next();
                if (!linkedOrganizations.contains(existing.getRequesterId())) {
                    sampleRequesterService.delete(existing);
                    stale.remove();
                }
            }
            if (!isLinked(existingOrganizationRequesters, updateData.getRequesterSite().getRequesterId())) {
                sampleRequesterService.insert(updateData.getRequesterSite());
            }
        }

        if (updateData.getRequesterSiteDepartment() != null) {
            Organization siteDepartment = organizationService
                    .get(String.valueOf(updateData.getRequesterSiteDepartment().getRequesterId()));
            boolean orgHasType = false;
            for (OrganizationType orgType : siteDepartment.getOrganizationTypes()) {
                if (orgType.getId().equals(TableIdService.getInstance().REFERRING_ORG_DEPARTMENT_TYPE_ID)) {
                    orgHasType = true;
                }
            }
            if (!orgHasType) {
                organizationService.linkOrganizationAndType(siteDepartment,
                        TableIdService.getInstance().REFERRING_ORG_DEPARTMENT_TYPE_ID);
            }
            updateData.getRequesterSiteDepartment().setSampleId(Long.parseLong(updateData.getSample().getId()));
            // if (updateData.getNewOrganizationDepartment() != null) {
            //
            // updateData.getRequesterSite().setRequesterId(updateData.getNewOrganizationDepartment().getId());
            // }
            if (!isLinked(existingOrganizationRequesters, updateData.getRequesterSiteDepartment().getRequesterId())) {
                sampleRequesterService.insert(updateData.getRequesterSiteDepartment());
            }
        }

        // Standalone Requestor contact (Environmental/Vector) — independent
        // of the Requesting Organization/Provider handled above.
        String requestorOrganizationId = null;
        if (updateData.getRequestorPerson() != null && updateData.getRequesterContact() != null
                && !GenericValidator.isBlankOrNull(updateData.getRequestorPerson().getId())) {
            // Bug (2026-07-06): this used to unconditionally insert() a new
            // sample_requester row on every save, including edits to an
            // order that already had a requestor_contact row — so every
            // re-save duplicated the link (6 duplicate rows observed for one
            // sample). Reuse the existing row for this sample if one exists;
            // only insert when there truly isn't one yet.
            long requestorContactTypeId = TableIdService.getInstance().REQUESTOR_CONTACT_REQUESTER_TYPE_ID;
            SampleRequester existingRequesterContact = sampleRequesterService
                    .getRequestersForSampleId(updateData.getSample().getId()).stream()
                    .filter(r -> r.getRequesterTypeId() == requestorContactTypeId).findFirst().orElse(null);

            if (existingRequesterContact != null) {
                existingRequesterContact.setRequesterId(updateData.getRequestorPerson().getId());
                existingRequesterContact.setSysUserId(updateData.getCurrentUserId());
                sampleRequesterService.update(existingRequesterContact);
            } else {
                updateData.getRequesterContact().setRequesterId(updateData.getRequestorPerson().getId());
                updateData.getRequesterContact().setSampleId(Long.parseLong(updateData.getSample().getId()));
                sampleRequesterService.insert(updateData.getRequesterContact());
            }
        }

        if (updateData.getRequesterSite() != null) {
            requestorOrganizationId = updateData.getNewOrganization() != null ? updateData.getNewOrganization().getId()
                    : String.valueOf(updateData.getRequesterSite().getRequesterId());
        }

        // When both a Requesting Organization and a Requestor contact are
        // bound on the same order, link them via OrganizationContact for future
        // "suggested contacts for this org" reuse. Pure side-effect: does not gate
        // save and does not replace the two independent SampleRequester rows above.
        if (updateData.getRequestorPerson() != null && !GenericValidator.isBlankOrNull(requestorOrganizationId)
                && !GenericValidator.isBlankOrNull(updateData.getRequestorPerson().getId())) {
            linkOrganizationContactIfNeeded(requestorOrganizationId, updateData.getRequestorPerson().getId(),
                    updateData.getCurrentUserId());
        }
    }

    private void linkOrganizationContactIfNeeded(String organizationId, String personId, String currentUserId) {
        List<OrganizationContact> existingContacts = organizationContactService
                .getListForOrganizationId(organizationId);
        boolean alreadyLinked = existingContacts.stream()
                .anyMatch(contact -> contact.getPerson() != null && personId.equals(contact.getPerson().getId()));
        if (alreadyLinked) {
            return;
        }

        // A bare `new Person()` with only an id set is a transient POJO to
        // Hibernate, not a managed entity — cascading the many-to-one save
        // throws TransientPropertyValueException. Load the already-persisted
        // Person (persistRequestorContactData/persistProviderData run before
        // this in persistData, so personId is guaranteed saved by now).
        Person person = personService.get(personId);
        OrganizationContact organizationContact = new OrganizationContact();
        organizationContact.setOrganizationId(organizationId);
        organizationContact.setPerson(person);
        organizationContact.setSysUserId(currentUserId);
        organizationContactService.insert(organizationContact);
    }

    private void persistInitialSampleConditions(SamplePatientUpdateData updateData) {
        String patientId = updateData.getPatientId();

        if (GenericValidator.isBlankOrNull(patientId)) {
            return;
        }

        for (SampleTestCollection sampleTestCollection : updateData.getSampleItemsTests()) {
            List<ObservationHistory> initialConditions = sampleTestCollection.initialSampleConditionIdList;

            if (initialConditions != null) {
                for (ObservationHistory observation : initialConditions) {
                    observation.setSampleId(sampleTestCollection.item.getSample().getId());
                    observation.setSampleItemId(sampleTestCollection.item.getId());
                    observation.setPatientId(patientId);
                    observation.setSysUserId(updateData.getCurrentUserId());
                    observationHistoryService.insert(observation);
                }
            }
        }
    }

    private void persistSampleNature(SamplePatientUpdateData updateData) {
        String patientId = updateData.getPatientId();

        if (GenericValidator.isBlankOrNull(patientId)) {
            return;
        }

        for (SampleTestCollection sampleTestCollection : updateData.getSampleItemsTests()) {
            ObservationHistory sampleNature = sampleTestCollection.sampleNature;

            if (sampleNature != null) {
                sampleNature.setSampleId(sampleTestCollection.item.getSample().getId());
                sampleNature.setSampleItemId(sampleTestCollection.item.getId());
                sampleNature.setPatientId(patientId);
                sampleNature.setSysUserId(updateData.getCurrentUserId());
                observationHistoryService.insert(sampleNature);
            }
        }
    }

    private Analysis populateAnalysis(String analysisRevision, SampleTestCollection sampleTestCollection, Test test,
            String userSelectedTestSection, String sampleTypeName, SamplePatientUpdateData updateData) {
        java.sql.Date collectionDateTime = DateUtil.convertStringDateTimeToSqlDate(sampleTestCollection.collectionDate);
        TestSection testSection = test.getTestSection();
        if (!org.apache.commons.validator.GenericValidator.isBlankOrNull(userSelectedTestSection)) {
            testSection = testSectionService.get(userSelectedTestSection);
        }

        Panel panel = updateData.getSampleAddService().getPanelForTest(sampleTestCollection, test);

        Analysis analysis = new Analysis();
        analysis.setTest(test);
        analysis.setPanel(panel);
        analysis.setIsReportable(test.getIsReportable());
        analysis.setAnalysisType(DEFAULT_ANALYSIS_TYPE);
        analysis.setSampleItem(sampleTestCollection.item);
        analysis.setSysUserId(sampleTestCollection.item.getSysUserId());
        analysis.setRevision(analysisRevision);
        analysis.setStartedDate(collectionDateTime == null ? DateUtil.getNowAsTimestamp()
                : new java.sql.Timestamp(collectionDateTime.getTime()));
        if (sampleTestCollection.item.isRejected()) {
            analysis.setStatusId(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.SampleRejected));
        } else {
            analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted));
        }
        if (!org.apache.commons.validator.GenericValidator.isBlankOrNull(sampleTypeName)) {
            analysis.setSampleTypeName(sampleTypeName);
        }
        analysis.setTestSection(testSection);
        // this will be used as an identifier for the service request as well
        analysis.setFhirUuid(UUID.randomUUID());
        return analysis;
    }

    private void createQcProfile(SampleTestCollection stc, SamplePatientUpdateData updateData, SampleItem savedItem) {
        org.openelisglobal.qc.valueholder.SampleItemQcProfile qcProfile = new org.openelisglobal.qc.valueholder.SampleItemQcProfile();
        qcProfile.setId(UUID.randomUUID().toString());
        qcProfile.setSampleItemId(Integer.valueOf(savedItem.getId()));
        qcProfile.setQcType(stc.qcType);
        qcProfile.setSystemUserId(Integer.valueOf(savedItem.getSysUserId()));
        qcProfile.setSysUserId(savedItem.getSysUserId());

        if (!GenericValidator.isBlankOrNull(stc.qcParentSampleIndex)) {
            int parentIdx = Integer.parseInt(stc.qcParentSampleIndex);
            List<SampleTestCollection> allStcs = updateData.getSampleItemsTests();
            if (parentIdx >= 0 && parentIdx < allStcs.size()) {
                SampleItem parentItem = allStcs.get(parentIdx).item;
                if (parentItem != null && parentItem.getId() != null) {
                    qcProfile.setParentSampleItemId(Integer.valueOf(parentItem.getId()));
                }
            }
        }

        if ("CONTROL".equals(stc.qcType) && !GenericValidator.isBlankOrNull(stc.qcExpectedValue)) {
            qcProfile.setExpectedValue(new java.math.BigDecimal(stc.qcExpectedValue));
        }

        sampleItemQcProfileDAO.insert(qcProfile);
    }

    /**
     * OGC-285 M5b — see {@link SamplePatientEntryService#persistLabelRequests}.
     * Lives in this scanned service (not the scan-excluded REST controller) so the
     * positional correlation logic is reachable by a real-DB integration test that
     * drives the same code the save hook calls.
     */
    @Transactional
    @Override
    public List<OrderLabelRequest> persistLabelRequests(SamplePatientUpdateData updateData,
            OrderLabelPersistRequest payload, String sysUserId) {
        if (payload == null || updateData == null || updateData.getSample() == null) {
            return new ArrayList<>();
        }
        String orderId = updateData.getSample().getId();
        if (orderId == null) {
            return new ArrayList<>();
        }

        // Correlate by list position over the in-memory, ordered, ID-bearing
        // sampleItemsTests (populated by persistData) — NOT a re-fetch + sortOrder
        // sort (sortOrder is a String, lexically ordered: "10" < "2").
        Map<String, String> sampleIdMap = new LinkedHashMap<>();
        Map<String, List<String>> testIdsBySampleLocal = new LinkedHashMap<>();
        List<SampleTestCollection> sampleItemsTests = updateData.getSampleItemsTests();
        if (sampleItemsTests != null) {
            for (int i = 0; i < sampleItemsTests.size(); i++) {
                SampleTestCollection stc = sampleItemsTests.get(i);
                String local = String.valueOf(i);
                if (stc.item != null && stc.item.getId() != null) {
                    sampleIdMap.put(local, stc.item.getId());
                } else if (stc.existingSampleItemId != null && !stc.existingSampleItemId.isBlank()) {
                    sampleIdMap.put(local, stc.existingSampleItemId);
                }
                List<String> testIds = new ArrayList<>();
                if (stc.tests != null) {
                    for (Test test : stc.tests) {
                        if (test != null && test.getId() != null) {
                            testIds.add(test.getId());
                        }
                    }
                }
                testIdsBySampleLocal.put(local, testIds);
                String itemId = sampleIdMap.get(local);
                if (itemId != null) {
                    testIdsBySampleLocal.put("item-" + itemId, testIds);
                }
            }
        }

        return orderLabelRequestService.persistRequest(orderId, sampleIdMap, payload, sysUserId, testIdsBySampleLocal);
    }
}
