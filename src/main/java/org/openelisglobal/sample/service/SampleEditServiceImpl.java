package org.openelisglobal.sample.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.RequesterService;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.common.services.SampleOrderService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.services.registration.ResultUpdateRegister;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dataexchange.orderresult.OrderResponseWorker.Event;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.bean.SampleEditItem;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleEditServiceImpl implements SampleEditService {

    private static final String DEFAULT_ANALYSIS_TYPE = "MANUAL";
    private static final String CANCELED_TEST_STATUS_ID;
    private static final String CANCELED_SAMPLE_STATUS_ID;
    private final String SAMPLE_SUBJECT = "Sample Note";

    static {
        CANCELED_TEST_STATUS_ID = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled);
        CANCELED_SAMPLE_STATUS_ID = SpringContext.getBean(IStatusService.class).getStatusID(SampleStatus.Canceled);
    }

    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private TestService testService;
    @Autowired
    private ObservationHistoryService observationService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private PersonService personService;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private SampleRequesterService sampleRequesterService;
    @Autowired
    private OrganizationService organizationService;
    // @Autowired
    // private OrganizationOrganizationTypeService orgOrgTypeService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    TypeOfSampleTestService typeOfSampleTestService;
    @Autowired
    ResultService resultService;
    @Autowired
    SampleHumanService sampleHumanService;
    @Autowired
    UserRoleService userRoleService;
    @Autowired
    NoteService noteService;

    @Transactional
    @Override
    public void editSample(SampleEditForm form, HttpServletRequest request, Sample updatedSample, boolean sampleChanged,
            String sysUserId) {

        List<SampleEditItem> existingTests = form.getExistingTests() != null ? form.getExistingTests()
                : new ArrayList<>();
        List<Analysis> cancelAnalysisList = createRemoveList(existingTests, sysUserId);
        List<SampleItem> updateSampleItemList = createSampleItemUpdateList(existingTests, sysUserId);
        List<SampleItem> cancelSampleItemList = createCancelSampleList(existingTests, cancelAnalysisList, sysUserId);
        List<Analysis> addAnalysisList = createAddAanlysisList(
                form.getPossibleTests() != null ? form.getPossibleTests() : new ArrayList<>(), sysUserId);

        List<IResultUpdate> updaters = ResultUpdateRegister.getRegisteredUpdaters();
        ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet(sysUserId);

        if (updatedSample == null) {
            updatedSample = sampleService.getSampleByAccessionNumber(form.getAccessionNumber());
        }
        updatedSample.setPriority(form.getSampleOrderItems().getPriority());
        String receivedDateForDisplay = updatedSample.getReceivedDateForDisplay();
        String collectionDateFromRecieveDate = null;
        boolean useReceiveDateForCollectionDate = !FormFields.getInstance().useField(Field.CollectionDate);

        if (useReceiveDateForCollectionDate) {
            collectionDateFromRecieveDate = receivedDateForDisplay + " 00:00:00";
        }

        SampleAddService sampleAddService = new SampleAddService(form.getSampleXML(), sysUserId, updatedSample,
                collectionDateFromRecieveDate);
        List<SampleTestCollection> addedSamples = createAddSampleList(form, sampleAddService);

        SampleOrderService sampleOrderService = new SampleOrderService(form.getSampleOrderItems());
        sampleOrderService.setSample(updatedSample);
        SampleOrderService.SampleOrderPersistenceArtifacts orderArtifacts = sampleOrderService
                .getPersistenceArtifacts(updatedSample, sysUserId);

        if (orderArtifacts.getSample() != null) {
            sampleChanged = true;
            updatedSample = orderArtifacts.getSample();
        }
        Patient patient = sampleService.getPatient(updatedSample);
        persistProviderData(orderArtifacts);
        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setSampleId(updatedSample.getId());
        SampleHuman existingSampleHuman = sampleHumanService.getDataBySample(sampleHuman);
        existingSampleHuman.setSysUserId(sysUserId);
        existingSampleHuman.setSampleId(updatedSample.getId());
        existingSampleHuman.setPatientId(patient.getId());
        if (orderArtifacts.getProvider() != null) {
            existingSampleHuman.setProviderId(orderArtifacts.getProvider().getId());
        }
        sampleHumanService.update(existingSampleHuman);

        for (SampleItem sampleItem : updateSampleItemList) {
            sampleItemService.update(sampleItem);
        }

        for (Analysis analysis : cancelAnalysisList) {
            analysisService.update(analysis);
            addExternalResultsToDeleteList(analysis, patient, updatedSample, actionDataSet);
        }

        for (IResultUpdate updater : updaters) {
            updater.postTransactionalCommitUpdate(actionDataSet);
        }

        for (Analysis analysis : addAnalysisList) {
            if (analysis.getId() == null) {
                analysisService.insert(analysis);
            } else {
                analysisService.update(analysis);
            }
        }

        for (SampleItem sampleItem : cancelSampleItemList) {
            sampleItemService.update(sampleItem);
        }

        if (sampleChanged) {
            sampleService.update(updatedSample);
        }

        // seems like this is unused
        /*
         * if (paymentObservation != null) {
         * paymentObservation.setPatientId(patient.getId());
         * observationDAO.insertOrUpdateData(paymentObservation); }
         */

        for (SampleTestCollection sampleTestCollection : addedSamples) {
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

            for (Test test : sampleTestCollection.tests) {
                test = testService.get(test.getId());

                Analysis analysis = populateAnalysis(sampleTestCollection, test,
                        sampleTestCollection.testIdToUserSectionMap.get(test.getId()), sampleAddService);
                analysisService.insert(analysis);
            }

            if (sampleTestCollection.initialSampleConditionIdList != null) {
                for (ObservationHistory observation : sampleTestCollection.initialSampleConditionIdList) {
                    observation.setPatientId(patient.getId());
                    observation.setSampleItemId(sampleTestCollection.item.getId());
                    observation.setSampleId(sampleTestCollection.item.getSample().getId());
                    observation.setSysUserId(sysUserId);
                    observationService.insert(observation);
                }
            }

            if (sampleTestCollection.sampleNature != null) {
                sampleTestCollection.sampleNature.setPatientId(patient.getId());
                sampleTestCollection.sampleNature.setSampleItemId(sampleTestCollection.item.getId());
                sampleTestCollection.sampleNature.setSampleId(sampleTestCollection.item.getSample().getId());
                sampleTestCollection.sampleNature.setSysUserId(sysUserId);
                observationService.insert(sampleTestCollection.sampleNature);
            }
        }

        for (ObservationHistory observation : orderArtifacts.getObservations()) {
            observationService.save(observation);
        }

        if (orderArtifacts.getSamplePersonRequester() != null) {
            SampleRequester samplePersonRequester = orderArtifacts.getSamplePersonRequester();
            samplePersonRequester.setRequesterId(orderArtifacts.getProviderPerson().getId());
            sampleRequesterService.save(samplePersonRequester);
        }

        if (orderArtifacts.getProviderOrganization() != null) {
            boolean link = orderArtifacts.getProviderOrganization().getId() == null;
            organizationService.save(orderArtifacts.getProviderOrganization());
            if (link) {
                organizationService.linkOrganizationAndType(orderArtifacts.getProviderOrganization(),
                        RequesterService.REFERRAL_ORG_TYPE_ID);
            }
        }

        if (orderArtifacts.getProviderDepartmentOrganization() != null) {
            boolean link = true;
            String orgTypeId = TableIdService.getInstance().REFERRING_ORG_DEPARTMENT_TYPE_ID;
            Organization org = orderArtifacts.getProviderDepartmentOrganization();
            if (org.getOrganizationTypes() != null) {
                if (org.getOrganizationTypes().stream().anyMatch(e -> e.getId().equals(orgTypeId))) {
                    link = false;
                }
            }
            organizationService.save(orderArtifacts.getProviderDepartmentOrganization());
            if (link) {
                organizationService.linkOrganizationAndType(orderArtifacts.getProviderDepartmentOrganization(),
                        orgTypeId);
            }
        }

        if (orderArtifacts.getSampleOrganizationRequester() != null) {
            if (orderArtifacts.getProviderOrganization() != null) {
                orderArtifacts.getSampleOrganizationRequester()
                        .setRequesterId(orderArtifacts.getProviderOrganization().getId());
            }
            sampleRequesterService.save(orderArtifacts.getSampleOrganizationRequester());
        }

        if (orderArtifacts.getSampleOrganizationDepartRequester() != null) {
            if (orderArtifacts.getProviderDepartmentOrganization() != null) {
                orderArtifacts.getSampleOrganizationDepartRequester()
                        .setRequesterId(orderArtifacts.getProviderDepartmentOrganization().getId());
            }
            sampleRequesterService.save(orderArtifacts.getSampleOrganizationDepartRequester());
        }

        if (orderArtifacts.getDeletableSampleOrganizationRequester() != null) {
            sampleRequesterService.delete(orderArtifacts.getDeletableSampleOrganizationRequester());
        }

        request.getSession().setAttribute("lastAccessionNumber", updatedSample.getAccessionNumber());
        request.getSession().setAttribute("lastPatientId", patient.getId());
    }

    private void addExternalResultsToDeleteList(Analysis analysis, Patient patient, Sample updatedSample,
            ResultsUpdateDataSet actionDataSet) {
        List<ResultSet> deletedResults = new ArrayList<>();
        if (!GenericValidator.isBlankOrNull(analysis.getSampleItem().getSample().getReferringId())) {
            List<Result> results = resultService.getResultsByAnalysis(analysis);
            if (results.size() == 0) {
                Result result = createCancelResult(analysis);
                results.add(result);
            }
            for (Result result : results) {
                result.setResultEvent(Event.TESTING_NOT_DONE);

                deletedResults.add(new ResultSet(result, null, null, patient, updatedSample, null, false));
            }
        }
        actionDataSet.setModifiedResults(deletedResults);
    }

    private Result createCancelResult(Analysis analysis) {
        Result result = new Result();
        result.setAnalysis(analysis);
        result.setMinNormal((double) 0);
        result.setMaxNormal((double) 0);
        result.setValue("cancel");
        return result;
    }

    private List<SampleItem> createSampleItemUpdateList(List<SampleEditItem> existingTests, String sysUserId) {
        List<SampleItem> modifyList = new ArrayList<>();

        for (SampleEditItem editItem : existingTests) {
            if (editItem.isSampleItemChanged()) {
                SampleItem sampleItem = sampleItemService.get(editItem.getSampleItemId());
                if (sampleItem != null) {
                    String collectionTime = editItem.getCollectionDate();
                    if (GenericValidator.isBlankOrNull(collectionTime)) {
                        sampleItem.setCollectionDate(null);
                    } else {
                        collectionTime += " " + (GenericValidator.isBlankOrNull(editItem.getCollectionTime()) ? "00:00"
                                : editItem.getCollectionTime());
                        sampleItem.setCollectionDate(DateUtil.convertStringDateToTimestamp(collectionTime));
                    }
                    sampleItem.setSysUserId(sysUserId);
                    modifyList.add(sampleItem);
                }
            }
        }

        return modifyList;
    }

    private Analysis populateAnalysis(SampleTestCollection sampleTestCollection, Test test,
            String userSelectedTestSection, SampleAddService sampleAddService) {
        java.sql.Date collectionDateTime = DateUtil.convertStringDateTimeToSqlDate(sampleTestCollection.collectionDate);
        TestSection testSection = test.getTestSection();
        if (!GenericValidator.isBlankOrNull(userSelectedTestSection)) {
            testSection = testSectionService.get(userSelectedTestSection); // change
        }

        Panel panel = sampleAddService.getPanelForTest(test);

        Analysis analysis = new Analysis();
        analysis.setTest(test);
        analysis.setIsReportable(test.getIsReportable());
        analysis.setAnalysisType(DEFAULT_ANALYSIS_TYPE);
        analysis.setSampleItem(sampleTestCollection.item);
        analysis.setSysUserId(sampleTestCollection.item.getSysUserId());
        analysis.setRevision("0");
        analysis.setStartedDate(collectionDateTime == null ? DateUtil.getNowAsSqlDate() : collectionDateTime);
        if (sampleTestCollection.item.isRejected()) {
            analysis.setStatusId(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.SampleRejected));
        } else {
            analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted));
        }
        analysis.setTestSection(testSection);
        analysis.setPanel(panel);
        return analysis;
    }

    private List<SampleTestCollection> createAddSampleList(SampleEditForm form, SampleAddService sampleAddService) {

        String maxAccessionNumber = form.getMaxAccessionNumber();
        if (!GenericValidator.isBlankOrNull(maxAccessionNumber)) {
            sampleAddService.setInitialSampleItemOrderValue(Integer.parseInt(maxAccessionNumber.split("-")[1]));
        }

        return sampleAddService.createSampleTestCollection();
    }

    private List<SampleItem> createCancelSampleList(List<SampleEditItem> list, List<Analysis> cancelAnalysisList,
            String sysUserId) {
        List<SampleItem> cancelList = new ArrayList<>();

        boolean cancelTest = false;

        for (SampleEditItem editItem : list) {
            if (editItem.getAccessionNumber() != null) {
                cancelTest = false;
            }
            if (cancelTest && !cancelAnalysisListContainsId(editItem.getAnalysisId(), cancelAnalysisList)) {
                Analysis analysis = getCancelableAnalysis(editItem, sysUserId);
                cancelAnalysisList.add(analysis);
            }

            if (editItem.isRemoveSample()) {
                cancelTest = true;
                SampleItem sampleItem = getCancelableSampleItem(editItem, sysUserId);
                if (sampleItem != null) {
                    cancelList.add(sampleItem);
                }
                if (!cancelAnalysisListContainsId(editItem.getAnalysisId(), cancelAnalysisList)) {
                    Analysis analysis = getCancelableAnalysis(editItem, sysUserId);
                    cancelAnalysisList.add(analysis);
                }
            }
        }

        return cancelList;
    }

    private List<Analysis> createAddAanlysisList(List<SampleEditItem> tests, String sysUserId) {
        List<Analysis> addAnalysisList = new ArrayList<>();

        for (SampleEditItem sampleEditItem : tests) {
            if (sampleEditItem.isAdd()) {

                Analysis analysis = newOrExistingCanceledAnalysis(sampleEditItem);

                if (analysis.getId() == null) {
                    SampleItem sampleItem = sampleItemService.get(sampleEditItem.getSampleItemId());
                    analysis.setSampleItem(sampleItem);

                    Test test = testService.get(sampleEditItem.getTestId());

                    analysis.setTest(test);
                    analysis.setRevision("0");
                    analysis.setTestSection(test.getTestSection());
                    analysis.setEnteredDate(DateUtil.getNowAsTimestamp());
                    analysis.setIsReportable(test.getIsReportable());
                    analysis.setAnalysisType("MANUAL");
                    analysis.setStartedDate(DateUtil.getNowAsSqlDate());
                }

                analysis.setStatusId(
                        SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted));
                analysis.setSysUserId(sysUserId);

                addAnalysisList.add(analysis);
            }
        }

        return addAnalysisList;
    }

    private Analysis newOrExistingCanceledAnalysis(SampleEditItem sampleEditItem) {
        List<Analysis> canceledAnalysis = analysisService
                .getAnalysesBySampleItemIdAndStatusId(sampleEditItem.getSampleItemId(), CANCELED_TEST_STATUS_ID);

        for (Analysis analysis : canceledAnalysis) {
            if (sampleEditItem.getTestId().equals(analysis.getTest().getId())) {
                return analysis;
            }
        }

        return new Analysis();
    }

    private List<Analysis> createRemoveList(List<SampleEditItem> tests, String sysUserId) {
        List<Analysis> removeAnalysisList = new ArrayList<>();

        for (SampleEditItem sampleEditItem : tests) {
            if (sampleEditItem.isCanceled()) {
                Analysis analysis = getCancelableAnalysis(sampleEditItem, sysUserId);
                removeAnalysisList.add(analysis);
            }
        }

        return removeAnalysisList;
    }

    private Analysis getCancelableAnalysis(SampleEditItem sampleEditItem, String sysUserId) {
        Analysis analysis = analysisService.get(sampleEditItem.getAnalysisId());
        analysis.setSysUserId(sysUserId);
        analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled));
        return analysis;
    }

    private SampleItem getCancelableSampleItem(SampleEditItem editItem, String sysUserId) {
        String sampleItemId = editItem.getSampleItemId();
        SampleItem item = sampleItemService.get(sampleItemId);

        if (item.getId() != null) {
            item.setStatusId(CANCELED_SAMPLE_STATUS_ID);
            item.setSysUserId(sysUserId);
            return item;
        }

        return null;
    }

    private boolean cancelAnalysisListContainsId(String analysisId, List<Analysis> cancelAnalysisList) {

        for (Analysis analysis : cancelAnalysisList) {
            if (analysisId.equals(analysis.getId())) {
                return true;
            }
        }

        return false;
    }

    private void persistProviderData(SampleOrderService.SampleOrderPersistenceArtifacts orderArtifacts) {
        if (orderArtifacts.getProviderPerson() != null && orderArtifacts.getProvider() != null) {

            personService.save(orderArtifacts.getProviderPerson());
            orderArtifacts.getProvider().setPerson(orderArtifacts.getProviderPerson());

            providerService.save(orderArtifacts.getProvider());
        }
    }
}
