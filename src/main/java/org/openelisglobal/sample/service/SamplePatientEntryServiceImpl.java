package org.openelisglobal.sample.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.address.service.OrganizationAddressService;
import org.openelisglobal.address.valueholder.OrganizationAddress;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
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
        persistRequesterData(updateData);
        if (useInitialSampleCondition) {
            persistInitialSampleConditions(updateData);
        }
        if (useSampleNature) {
            persistSampleNature(updateData);
        }

        persistObservations(updateData);

        request.getSession().setAttribute("lastAccessionNumber", updateData.getAccessionNumber());
        request.getSession().setAttribute("lastPatientId", updateData.getPatientId());
    }

    private void persistObservations(SamplePatientUpdateData updateData) {

        for (ObservationHistory observation : updateData.getObservations()) {
            observation.setSampleId(updateData.getSample().getId());
            observation.setPatientId(updateData.getPatientId());
            observationHistoryService.insert(observation);
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

        updateData.getSample().setFhirUuid(UUID.randomUUID());
        sampleService.insertDataWithAccessionNumber(updateData.getSample());
        updateData.getSample().setPriority(updateData.getPriority());

        for (SampleAdditionalField field : updateData.getSampleFields()) {
            field.setSample(updateData.getSample());
            sampleService.saveSampleAdditionalField(field);
        }

        if (updateData.getProgramSample() != null) {
            if (updateData.getProgramQuestionnaireResponse() != null) {
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

        for (SampleTestCollection sampleTestCollection : updateData.getSampleItemsTests()) {
            if (GenericValidator.isBlankOrNull(sampleTestCollection.item.getFhirUuidAsString())) {
                sampleTestCollection.item.setFhirUuid(UUID.randomUUID());
            }
            String sampleId = sampleItemService.insert(sampleTestCollection.item);
            SampleItem savedItem = sampleItemService.get(sampleId);
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

        updateData.buildSampleHuman();

        sampleHumanService.insert(updateData.getSampleHuman());

        if (updateData.getElectronicOrder() != null) {
            electronicOrderService.update(updateData.getElectronicOrder());
        }
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

        for (SampleTestCollection sampleTestCollection : updateData.getSampleItemsTests()) {
            List<ObservationHistory> initialConditions = sampleTestCollection.initialSampleConditionIdList;

            if (initialConditions != null) {
                for (ObservationHistory observation : initialConditions) {
                    observation.setSampleId(sampleTestCollection.item.getSample().getId());
                    observation.setSampleItemId(sampleTestCollection.item.getId());
                    observation.setPatientId(updateData.getPatientId());
                    observation.setSysUserId(updateData.getCurrentUserId());
                    observationHistoryService.insert(observation);
                }
            }
        }
    }

    private void persistSampleNature(SamplePatientUpdateData updateData) {

        for (SampleTestCollection sampleTestCollection : updateData.getSampleItemsTests()) {
            ObservationHistory sampleNature = sampleTestCollection.sampleNature;

            if (sampleNature != null) {
                sampleNature.setSampleId(sampleTestCollection.item.getSample().getId());
                sampleNature.setSampleItemId(sampleTestCollection.item.getId());
                sampleNature.setPatientId(updateData.getPatientId());
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
        analysis.setStartedDate(collectionDateTime == null ? DateUtil.getNowAsSqlDate() : collectionDateTime);
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
