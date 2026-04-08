package org.openelisglobal.sample.service;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.eqa.service.EQAProgramService;
import org.openelisglobal.eqa.service.SampleEQAService;
import org.openelisglobal.eqa.valueholder.EQAPriority;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.SampleEQA;
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
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.program.service.ImmunohistochemistrySampleService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.valueholder.SampleAdditionalField;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.dao.SampleItemDAO;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SamplePatientEntryServiceImpl implements SamplePatientEntryService {

    private static final String DEFAULT_ANALYSIS_TYPE = "MANUAL";
    private final String SAMPLE_SUBJECT = "Sample Note";
    // private String currentUserId;

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
    private SampleRequesterService sampleRequesterService;
    @Autowired
    private OrganizationService organizationService;
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
    private EQAProgramService eqaProgramService;
    @Autowired
    private BarcodeInfoService barcodeInfoService;

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
        persistSampleData(updateData);

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
        }

        request.getSession().setAttribute("lastAccessionNumber", updateData.getAccessionNumber());
        request.getSession().setAttribute("lastPatientId", updateData.getPatientId());
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
            updateData.getSample().setFhirUuid(UUID.randomUUID());
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
        }

        // Process sample items and tests (may be empty in decoupled workflow)
        Map<SampleItem, Integer> specimenLabelQuantities = new LinkedHashMap<>();
        Integer orderLabelQuantity = null;
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
                    savedItem.setReceivedDate(sampleTestCollection.item.getReceivedDate());
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

                // Check if analysis already exists for this sample item + test (for updates)
                Analysis existingAnalysis = analysisService.getAnalysisBySampleItemAndTest(savedItem.getId(),
                        test.getId());
                if (existingAnalysis != null) {
                    sampleTestCollection.analysises.add(existingAnalysis);
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

    private void persistSampleEQAData(SamplePatientUpdateData updateData) {
        SampleEQA sampleEQA = new SampleEQA();
        sampleEQA.setSampleId(Long.parseLong(updateData.getSample().getId()));
        sampleEQA.setIsEqaSample(true);
        sampleEQA.setSysUserId(updateData.getCurrentUserId());

        if (!GenericValidator.isBlankOrNull(updateData.getEqaProgramId())) {
            EQAProgram eqaProgram = eqaProgramService.get(Long.parseLong(updateData.getEqaProgramId()));
            sampleEQA.setEqaProgram(eqaProgram);
        }
        if (!GenericValidator.isBlankOrNull(updateData.getEqaProviderOrganizationId())) {
            sampleEQA.setEqaProviderOrganizationId(Long.parseLong(updateData.getEqaProviderOrganizationId()));
        }
        sampleEQA.setEqaProviderSampleId(updateData.getEqaProviderSampleId());
        sampleEQA.setEqaParticipantId(updateData.getEqaParticipantId());

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

    private void persistRequesterData(SamplePatientUpdateData updateData) {
        if (updateData.getProviderPerson() != null && !org.apache.commons.validator.GenericValidator
                .isBlankOrNull(updateData.getProviderPerson().getId())) {
            SampleRequester sampleRequester = new SampleRequester();
            sampleRequester.setRequesterId(updateData.getProviderPerson().getId());
            sampleRequester.setRequesterTypeId(TableIdService.getInstance().PROVIDER_REQUESTER_TYPE_ID);
            sampleRequester.setSampleId(Long.parseLong(updateData.getSample().getId()));
            sampleRequester.setSysUserId(updateData.getCurrentUserId());
            sampleRequesterService.insert(sampleRequester);
        }

        if (updateData.getRequesterSite() != null) {
            updateData.getRequesterSite().setSampleId(Long.parseLong(updateData.getSample().getId()));
            if (updateData.getNewOrganization() != null) {
                updateData.getRequesterSite().setRequesterId(updateData.getNewOrganization().getId());
            }
            sampleRequesterService.insert(updateData.getRequesterSite());
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
            sampleRequesterService.insert(updateData.getRequesterSiteDepartment());
        }
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

        Panel panel = updateData.getSampleAddService().getPanelForTest(test);

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
}
