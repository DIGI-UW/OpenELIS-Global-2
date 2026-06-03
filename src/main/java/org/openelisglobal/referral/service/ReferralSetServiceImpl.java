package org.openelisglobal.referral.service;

import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.notification.service.NotificationContext;
import org.openelisglobal.notification.service.NotificationTriggerDispatcher;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.referral.dao.ReferralStatusHistoryDAO;
import org.openelisglobal.referral.fhir.service.FhirReferralService;
import org.openelisglobal.referral.fhir.service.TestNotFullyConfiguredException;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.referral.valueholder.ReferralSet;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.openelisglobal.referral.valueholder.ReferralSubcontract;
import org.openelisglobal.referral.valueholder.ReferralType;
import org.openelisglobal.referral.valueholder.SubcontractStatus;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ReferralSetServiceImpl implements ReferralSetService {

    @Autowired
    private ReferralService referralService;
    @Autowired
    private ReferralResultService referralResultService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private FhirReferralService fhirReferralService;
    @Autowired
    private TestService testService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private ReferralTypeService referralTypeService;
    @Autowired
    private ReferralStatusHistoryDAO statusHistoryDAO;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private NotificationTriggerDispatcher notificationTriggerDispatcher;

    private String REFERRAL_CONFORMATION_ID;

    @PostConstruct
    public void init() {
        ReferralType referralType = referralTypeService.getReferralTypeByName("Confirmation");
        if (referralType != null) {
            REFERRAL_CONFORMATION_ID = referralType.getId();
        } else {
            REFERRAL_CONFORMATION_ID = null;
        }
    }

    @Transactional
    @Override
    public void updateReferralSets(List<ReferralSet> referralSetList, List<Sample> modifiedSamples,
            Set<Sample> parentSamples, List<ReferralResult> removableReferralResults, String sysUserId) {
        for (ReferralSet referralSet : referralSetList) {
            referralService.update(referralSet.getReferral());

            for (ReferralResult referralResult : referralSet.getUpdatableReferralResults()) {
                Result rResult = referralResult.getResult();
                if (rResult != null) {
                    if (rResult.getId() == null) {
                        resultService.insert(rResult);
                    } else {
                        rResult.setSysUserId(sysUserId);
                        resultService.update(rResult);
                    }
                }

                if (referralResult.getId() == null) {
                    referralResultService.insert(referralResult);
                } else {
                    referralResultService.update(referralResult);
                }
            }

            if (referralSet.getNote() != null) {
                if (referralSet.getNote().getId() == null) {
                    noteService.insert(referralSet.getNote());
                } else {
                    noteService.update(referralSet.getNote());
                }
            }
        }

        for (ReferralResult referralResult : removableReferralResults) {

            referralResult.setSysUserId(sysUserId);
            referralResultService.delete(referralResult);

            if (referralResult.getResult() != null && referralResult.getResult().getId() != null) {
                referralResult.getResult().setSysUserId(sysUserId);
                resultService.delete(referralResult.getResult());
            }
        }

        setStatusOfParentSamples(modifiedSamples, parentSamples, sysUserId);

        for (Sample sample : modifiedSamples) {
            sampleService.update(sample);
        }

        for (ReferralSet referralSet : referralSetList) {
            if (referralSet.getReferral().isCanceled()) {
                // try {
                // fhirReferralService.cancelReferralToOrganization(
                // referralSet.getReferral().getOrganization().getId(),
                //
                // referralSet.getReferral().getAnalysis().getSampleItem().getSample().getId(),
                //
                // Arrays.asList(referralSet.getReferral().getAnalysis().getId()));
                // } catch (FhirLocalPersistingException e) {
                // // TODO don't catch since this is a considerable error in OE world
                // going ahead?
                // LogEvent.logError(e);
                // }
            } else {
                try {
                    fhirReferralService.referAnalysisesToOrganization(referralSet.getReferral());
                } catch (TestNotFullyConfiguredException e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "updateRefreralSets",
                            "unable to automatically refer a test that does not have a loinc code set");
                } catch (FhirLocalPersistingException e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "updateRefreralSets",
                            "had a problem saving the referral locally in fhir");
                }
            }
        }
    }

    private void setStatusOfParentSamples(List<Sample> modifiedSamples, Set<Sample> parentSamples, String sysUserId) {
        for (Sample sample : parentSamples) {
            List<Analysis> analysisList = analysisService.getAnalysesBySampleId(sample.getId());

            String finalizedId = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized);
            boolean allAnalysisFinished = true;

            if (analysisList != null) {
                for (Analysis childAnalysis : analysisList) {
                    Referral referral = referralService.getReferralByAnalysisId(childAnalysis.getId());
                    List<ReferralResult> referralResultList;

                    if (referral == null || referral.getId() == null) {
                        referralResultList = new ArrayList<>();
                    } else {
                        referralResultList = referralResultService.getReferralResultsForReferral(referral.getId());
                    }

                    if (referralResultList.isEmpty() || referral == null) {
                        if (!finalizedId.equals(childAnalysis.getStatusId())) {
                            allAnalysisFinished = false;
                            break;
                        }
                    } else {
                        for (ReferralResult referralResult : referralResultList) {
                            if (referralResult.getResult() == null
                                    || GenericValidator.isBlankOrNull(referralResult.getResult().getValue())) {
                                if (!(referral.isCanceled() && finalizedId.equals(childAnalysis.getStatusId()))) {
                                    allAnalysisFinished = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (allAnalysisFinished) {
                sample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Finished));
                sample.setSysUserId(sysUserId);
                modifiedSamples.add(sample);
            }
        }
    }

    @Override
    @Transactional
    public void createSaveReferralSetsSamplePatientEntry(List<ReferralItem> referralItems,
            SamplePatientUpdateData updateData) {
        List<ReferralSet> referralSets = createReferralRowsForItems(referralItems, updateData);
        updateReferralSets(referralSets, new ArrayList<>(), new HashSet<>(), new ArrayList<>(),
                updateData.getCurrentUserId());
    }

    @Override
    @Transactional
    public void createDraftReferralSetsForOrderEntry(List<ReferralItem> referralItems,
            SamplePatientUpdateData updateData) {
        if (referralItems == null || referralItems.isEmpty()) {
            return;
        }
        // S-14 / OGC-624: env/vector "Refer Out" save is DB-only. The FHIR push
        // that lives at the end of updateReferralSets is intentionally NOT run
        // here — receiving-lab FHIR resources are created on the DRAFT →
        // DISPATCHED transition (see ReferralServiceImpl.transition).
        createReferralRowsForItems(referralItems, updateData);
    }

    /**
     * Shared insert/update body for both the legacy clinical Step-1 entry point and
     * the new env/vector draft-only entry point. Returns the set of newly-created
     * Referrals wrapped in {@link ReferralSet}s so the caller can decide whether to
     * fan out into {@link #updateReferralSets}.
     */
    private List<ReferralSet> createReferralRowsForItems(List<ReferralItem> referralItems,
            SamplePatientUpdateData updateData) {
        List<ReferralSet> referralSets = new ArrayList<>();
        if (referralItems == null) {
            return referralSets;
        }
        for (ReferralItem referralItem : referralItems) {
            if (!GenericValidator.isBlankOrNull(referralItem.getReferralId())) {
                updateExistingReferralFromItem(referralItem, updateData.getCurrentUserId());
                continue;
            }
            Result result = new Result();
            result.setSysUserId("1");

            Referral referral = new Referral();
            referral.setFhirUuid(UUID.randomUUID());
            referral.setStatus(ReferralStatus.SENT);
            referral.setSysUserId(updateData.getCurrentUserId());
            referral.setReferralTypeId(REFERRAL_CONFORMATION_ID);

            referral.setRequestDate(new Timestamp(new Date().getTime()));
            referral.setSentDate(DateUtil.convertStringDateToTruncatedTimestamp(referralItem.getReferredSendDate()));
            referral.setRequesterName(referralItem.getReferrer());
            referral.setOrganization(organizationService.get(referralItem.getReferredInstituteId()));
            referral.setSubcontract(buildSubcontractFromItem(referralItem, updateData.getCurrentUserId()));
            for (SampleTestCollection sampleItemTest : updateData.getSampleItemsTests()) {
                for (Analysis analysis : sampleItemTest.analysises) {
                    if (referralItem.getReferredTestId().equals(analysis.getTest().getId())) {
                        referral.setAnalysis(analysis);

                        String testResultType = testService.getResultType(analysis.getTest());
                        result.setResultType(testResultType);
                        result.setAnalysis(analysis);
                    }
                }
            }
            referral.setReferralReasonId(referralItem.getReferralReasonId());

            referralService.insert(referral);
            insertInitialDraftHistory(referral.getId(), updateData.getCurrentUserId());
            resultService.insert(result);
            fireReferralOutAfterCommit(referral, updateData.getCurrentUserId());
            ReferralResult referralResult = new ReferralResult();
            referralResult.setReferralId(referral.getId());
            referralResult.setSysUserId(updateData.getCurrentUserId());
            referralResult.setTestId(referralItem.getReferredTestId());
            referralResult.setResult(result);
            referralResultService.insert(referralResult);

            ReferralSet referralSet = new ReferralSet();
            referralSet.setReferral(referral);
            referralSet.setExistingReferralResults(Arrays.asList(referralResult));
            referralSets.add(referralSet);
        }
        return referralSets;
    }

    // Refer Out edit form re-submits the existing referralId; update in place to
    // avoid duplicate rows.
    private void updateExistingReferralFromItem(ReferralItem referralItem, String currentUserId) {
        Referral existing = referralService.getReferralById(referralItem.getReferralId());
        if (existing == null) {
            return;
        }
        existing.setSysUserId(currentUserId);
        if (!GenericValidator.isBlankOrNull(referralItem.getReferredInstituteId())) {
            existing.setOrganization(organizationService.get(referralItem.getReferredInstituteId()));
        }
        existing.setReferralReasonId(referralItem.getReferralReasonId());
        existing.setRequesterName(referralItem.getReferrer());
        if (!GenericValidator.isBlankOrNull(referralItem.getReferredSendDate())) {
            existing.setSentDate(DateUtil.convertStringDateToTruncatedTimestamp(referralItem.getReferredSendDate()));
        }

        ReferralSubcontract subcontract = existing.getSubcontract();
        if (subcontract == null) {
            existing.setSubcontract(buildSubcontractFromItem(referralItem, currentUserId));
        } else {
            subcontract.setSysUserId(currentUserId);
            subcontract.setAgreementReference(referralItem.getAgreementReference());
            subcontract.setHandoffDatetimeForDisplay(referralItem.getHandoffDatetime());
            subcontract.setExpectedReturnDateForDisplay(referralItem.getExpectedReturnDate());
            subcontract.setCocContactName(referralItem.getCocContactName());
            subcontract.setCocContactPhone(referralItem.getCocContactPhone());
            subcontract.setCocContactEmail(referralItem.getCocContactEmail());
            subcontract.setSubcontractNotes(referralItem.getSubcontractNotes());
        }
        referralService.update(existing);
    }

    private ReferralSubcontract buildSubcontractFromItem(ReferralItem referralItem, String currentUserId) {
        ReferralSubcontract subcontract = new ReferralSubcontract();
        subcontract.setSubcontractStatus(SubcontractStatus.DRAFT);
        subcontract.setSysUserId(currentUserId);
        subcontract.setAgreementReference(referralItem.getAgreementReference());
        subcontract.setHandoffDatetimeForDisplay(referralItem.getHandoffDatetime());
        subcontract.setExpectedReturnDateForDisplay(referralItem.getExpectedReturnDate());
        subcontract.setCocContactName(referralItem.getCocContactName());
        subcontract.setCocContactPhone(referralItem.getCocContactPhone());
        subcontract.setCocContactEmail(referralItem.getCocContactEmail());
        subcontract.setSubcontractNotes(referralItem.getSubcontractNotes());
        return subcontract;
    }

    /**
     * Schedule a REFERRAL_OUT notification fire post-commit. Building the {@code
     * NotificationContext} inside this transaction (while entity associations are
     * still attached to the session) avoids LazyInitializationException on the
     * async thread. The async dispatcher fires only after the parent transaction
     * commits; if no transaction is active (rare), it fires synchronously.
     */
    private void fireReferralOutAfterCommit(Referral referral, String currentUserId) {
        NotificationContext context = buildReferralOutContext(referral, currentUserId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        notificationTriggerDispatcher.fire(context);
                    } catch (RuntimeException e) {
                        LogEvent.logError(ReferralSetServiceImpl.class.getSimpleName(), "fireReferralOutAfterCommit",
                                "failed to fire REFERRAL_OUT for referral " + referral.getId());
                        LogEvent.logError(e);
                    }
                }
            });
        } else {
            notificationTriggerDispatcher.fire(context);
        }
    }

    private NotificationContext buildReferralOutContext(Referral referral, String currentUserId) {
        Sample sample = null;
        if (referral.getAnalysis() != null && referral.getAnalysis().getSampleItem() != null) {
            sample = referral.getAnalysis().getSampleItem().getSample();
        }
        Organization receivingLab = referral.getOrganization();

        String accession = sample == null || sample.getAccessionNumber() == null ? "" : sample.getAccessionNumber();
        String testName = referral.getAnalysis() == null || referral.getAnalysis().getTest() == null ? ""
                : referral.getAnalysis().getTest().getLocalizedTestName().getLocalizedValue();

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("referralId", referral.getId() == null ? "" : referral.getId());
        variables.put("orderId", accession);
        variables.put("sampleAccessionNumber", accession);
        variables.put("labName", receivingLab == null || receivingLab.getOrganizationName() == null ? ""
                : receivingLab.getOrganizationName());
        variables.put("testName", testName);
        variables.put("referredTests", resolveReferredTests(sample, testName));
        variables.put("referralDate", referral.getRequestDate() == null ? ""
                : DateUtil.convertTimestampToStringDate(referral.getRequestDate()));
        variables.put("expectedReturn",
                referral.getSubcontract() == null || referral.getSubcontract().getExpectedReturnDateForDisplay() == null
                        ? ""
                        : referral.getSubcontract().getExpectedReturnDateForDisplay());
        String sendingLabName = ConfigurationProperties.getInstance().getPropertyValue(Property.configurationName);
        variables.put("sendingLabName", sendingLabName == null ? "" : sendingLabName);
        variables.put("sendingLabPhone", "");
        String patientFirst = "";
        String patientLast = "";
        if (sample != null) {
            try {
                Patient patient = sampleHumanService.getPatientForSample(sample);
                if (patient != null && patient.getPerson() != null) {
                    patientFirst = patient.getPerson().getFirstName() == null ? "" : patient.getPerson().getFirstName();
                    patientLast = patient.getPerson().getLastName() == null ? "" : patient.getPerson().getLastName();
                }
            } catch (RuntimeException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "buildReferralOutContext",
                        "could not resolve patient for sample " + sample.getId());
            }
        }
        variables.put("patientFirstName", patientFirst);
        variables.put("patientLastName", patientLast);

        return new NotificationContext("REFERRAL_OUT", referral, sample, receivingLab, currentUserId, variables);
    }

    /**
     * Builds a comma-separated list of test names referred for this sample. Today a
     * {@link Referral} attaches to a single {@link Analysis}, so the natural
     * "referred tests" set is the union of referrals on the sample. Falls back to
     * the single-test name when no sample is available.
     */
    private String resolveReferredTests(Sample sample, String fallbackTestName) {
        if (sample == null || sample.getId() == null) {
            return fallbackTestName == null ? "" : fallbackTestName;
        }
        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
        if (analyses == null || analyses.isEmpty()) {
            return fallbackTestName == null ? "" : fallbackTestName;
        }
        StringBuilder sb = new StringBuilder();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (Analysis analysis : analyses) {
            if (referralService.getReferralByAnalysisId(analysis.getId()) == null) {
                continue;
            }
            if (analysis.getTest() == null || analysis.getTest().getLocalizedTestName() == null) {
                continue;
            }
            String name = analysis.getTest().getLocalizedTestName().getLocalizedValue();
            if (!GenericValidator.isBlankOrNull(name) && seen.add(name)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(name);
            }
        }
        if (sb.length() == 0) {
            return fallbackTestName == null ? "" : fallbackTestName;
        }
        return sb.toString();
    }

    /**
     * S-14 FR-02 audit-trail seed: pair every Refer Out save with a
     * {@code null -> DRAFT} history row, giving every subcontract a complete
     * lifecycle record from inception.
     */
    private void insertInitialDraftHistory(String referralId, String actorUserId) {
        ReferralStatusHistory history = new ReferralStatusHistory();
        history.setReferralId(referralId);
        history.setFromStatus(null);
        history.setToStatus(SubcontractStatus.DRAFT);
        history.setChangedByUserId(actorUserId);
        history.setChangedAt(DateUtil.getNowAsTimestamp());
        history.setSysUserId(actorUserId);
        statusHistoryDAO.insert(history);
    }
}
