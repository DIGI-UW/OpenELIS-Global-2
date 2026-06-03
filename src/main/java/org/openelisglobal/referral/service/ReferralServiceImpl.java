package org.openelisglobal.referral.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.Hibernate;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.notification.service.NotificationContext;
import org.openelisglobal.notification.service.NotificationTriggerDispatcher;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.referral.action.beanitems.ReferralDisplayItem;
import org.openelisglobal.referral.dao.ReferralDAO;
import org.openelisglobal.referral.dao.ReferralStatusHistoryDAO;
import org.openelisglobal.referral.fhir.service.FhirReferralService;
import org.openelisglobal.referral.fhir.service.TestNotFullyConfiguredException;
import org.openelisglobal.referral.form.ReferredOutTestsForm;
import org.openelisglobal.referral.form.ReferredOutTestsForm.ReferDateType;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.openelisglobal.referral.valueholder.ReferralSubcontract;
import org.openelisglobal.referral.valueholder.SubcontractStatus;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ReferralServiceImpl extends AuditableBaseObjectServiceImpl<Referral, String> implements ReferralService {
    @Autowired
    protected ReferralDAO baseObjectDAO;

    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ReferralStatusHistoryDAO statusHistoryDAO;
    @Autowired
    private NotificationTriggerDispatcher notificationTriggerDispatcher;
    @Autowired
    private FhirReferralService fhirReferralService;

    ReferralServiceImpl() {
        super(Referral.class);
    }

    @Override
    protected ReferralDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Referral getReferralByAnalysisId(String id) {
        Referral referral = getMatch("analysis.id", id).orElse(null);
        if (referral != null) {
            Hibernate.initialize(referral.getOrganization());
        }
        return referral;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Referral> getUncanceledOpenReferrals() {
        return getBaseObjectDAO().getReferralsByStatus(
                Arrays.asList(ReferralStatus.CREATED, ReferralStatus.SENT, ReferralStatus.RECEIVED));
    }

    @Override
    @Transactional(readOnly = true)
    public Referral getReferralById(String referralId) {
        return getBaseObjectDAO().getReferralById(referralId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Referral> getReferralsBySampleId(String id) {
        return getBaseObjectDAO().getAllReferralsBySampleId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Referral> getReferralsByOrganization(String organizationId, Date lowDate, Date highDate) {
        return getBaseObjectDAO().getAllReferralsByOrganization(organizationId, lowDate, highDate);
    }

    @Override
    public List<Referral> getSentReferrals() {
        return getBaseObjectDAO().getReferralsByStatus(Arrays.asList(ReferralStatus.SENT));
    }

    @Override
    public List<UUID> getSentReferralUuids() {
        return getBaseObjectDAO().getReferralsByStatus(Arrays.asList(ReferralStatus.SENT)).stream()
                .map(e -> e.getFhirUuid()).filter(e -> e != null).collect(Collectors.toList());
    }

    @Override
    public List<Referral> getReferralsByTestAndDate(ReferDateType dateType, Timestamp startTimestamp,
            Timestamp endTimestamp, List<String> testUnitIds, List<String> testIds) {
        return baseObjectDAO.getReferralsByTestAndDate(dateType, startTimestamp, endTimestamp, testUnitIds, testIds);
    }

    @Override
    public List<Referral> getReferralsByAccessionNumber(String labNumber) {
        Sample sample = sampleService.getSampleByAccessionNumber(labNumber);
        if (sample != null) {
            List<Analysis> analysises = analysisService.getAnalysesBySampleId(sample.getId());
            return baseObjectDAO
                    .getReferralsByAnalysisIds(analysises.stream().map(Analysis::getId).collect(Collectors.toList()));
        }
        return new ArrayList<>();
    }

    @Override
    @Transactional
    public List<Referral> getReferralByPatientId(String selPatient) {
        List<Sample> samples = sampleHumanService.getSamplesForPatient(selPatient);
        List<Analysis> analysises = new ArrayList<>();
        for (Sample sample : samples) {
            analysises.addAll(analysisService.getAnalysesBySampleId(sample.getId()));
        }
        return baseObjectDAO
                .getReferralsByAnalysisIds(analysises.stream().map(Analysis::getId).collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralDisplayItem> getReferralItems(ReferredOutTestsForm form) {
        List<ReferralDisplayItem> referralItems = new ArrayList<>();
        List<Referral> referrals;
        switch (form.getSearchType()) {
        case TEST_AND_DATES:
            referrals = getReferralsByTestAndDate(form);
            break;
        case LAB_NUMBER:
            referrals = getReferralsByLabNumber(form);
            break;
        case PATIENT:
            referrals = getReferralsByPatient(form);
            break;
        default:
            referrals = new ArrayList<>();
        }

        for (Referral referral : referrals) {
            referralItems.add(convertToDisplayItem(referral));
        }

        return referralItems;
    }

    private List<Referral> getReferralsByTestAndDate(ReferredOutTestsForm form) {
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
        return getReferralsByTestAndDate(form.getDateType(), startTimestamp, endTimestamp, form.getTestUnitIds(),
                form.getTestIds());
    }

    private List<Referral> getReferralsByLabNumber(ReferredOutTestsForm form) {
        return getReferralsByAccessionNumber(form.getLabNumber());
    }

    private List<Referral> getReferralsByPatient(ReferredOutTestsForm form) {
        return getReferralByPatientId(form.getSelPatient());
    }

    @Override
    @Transactional(readOnly = true)
    public ReferralDisplayItem convertToDisplayItem(Referral referral) {
        ReferralDisplayItem referralItem = new ReferralDisplayItem();

        Analysis analysis = referral.getAnalysis();
        List<Result> resultList = analysisService.getResults(analysis);
        Patient patient = sampleHumanService.getPatientForSample(analysis.getSampleItem().getSample());

        referralItem.setAccessionNumber(analysis.getSampleItem().getSample().getAccessionNumber());
        referralItem.setReferredSendDate(DateUtil.convertTimestampToStringDate(referral.getSentDate()));
        referralItem.setReferralStatus(referral.getStatus());
        referralItem.setReferralStatusDisplay(referral.getStatus().toString());
        referralItem.setPatientLastName(patient.getPerson().getLastName());
        referralItem.setPatientFirstName(patient.getPerson().getFirstName());
        referralItem.setReferringTestName(analysis.getTest().getLocalizedTestName().getLocalizedValue());
        if (!resultList.isEmpty()) {
            referralItem.setReferralResultsDisplay(getAppropriateResultValue(resultList));
            referralItem.setResultDate(analysis.getCompletedDateForDisplay());
        }
        Organization organization = referral.getOrganization();
        if (organization != null) {
            referralItem.setReferenceLabDisplay(organization.getOrganizationName());
        }
        referralItem.setNotes(analysisService.getNotesAsString(analysis, true, true, "<br/>", false));
        referralItem.setAnalysisId(analysis.getId());

        return referralItem;
    }

    private String getAppropriateResultValue(List<Result> results) {
        Result result = results.get(0);
        if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.getResultType())) {
            if (!GenericValidator.isBlankOrNull(result.getValue()) && !"0".equals(result.getValue())) {
                Dictionary dictionary = dictionaryService.get(result.getValue());
                if (dictionary != null) {
                    return dictionary.getLocalizedName();
                }
            }
        } else if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(result.getResultType())) {
            StringBuilder multiResult = new StringBuilder();

            for (Result subResult : results) {
                if (!GenericValidator.isBlankOrNull(result.getValue()) && !"0".equals(result.getValue())) {
                    Dictionary dictionary = dictionaryService.get(subResult.getValue());

                    if (dictionary.getId() != null) {
                        multiResult.append(dictionary.getLocalizedName());
                        multiResult.append(", ");
                    }
                }
            }

            if (multiResult.length() > 0) {
                multiResult.setLength(multiResult.length() - 2); // remove last ", "
            }

            return multiResult.toString();
        } else {
            String resultValue = GenericValidator.isBlankOrNull(result.getValue()) ? "" : result.getValue();

            if (!GenericValidator.isBlankOrNull(resultValue)
                    && result.getAnalysis().getTest().getUnitOfMeasure() != null) {
                resultValue += " " + result.getAnalysis().getTest().getUnitOfMeasure().getName();
            }

            return resultValue;
        }

        return "";
    }

    // ---- S-14 / OGC-624 FR-02 subcontract transitions ------------------------

    @Override
    @Transactional
    public void dispatchSubcontract(String referralId, Timestamp handoffDatetime, String actorUserId, String notes) {
        if (handoffDatetime == null) {
            throw new IllegalArgumentException("handoffDatetime is required to dispatch a subcontract");
        }
        transition(referralId, SubcontractStatus.DISPATCHED, actorUserId, notes, handoffDatetime);
    }

    // REQUIRES_NEW isolates the strict-linear guard's IllegalStateException from
    // any parent transaction. Auto-trigger callers (FHIR result import, FHIR Task
    // acceptance poll) wrap this in try/catch so rejected transitions only log
    // and continue without rolling back the caller's own work.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSubcontractReceived(String referralId, String actorUserId, String notes) {
        transition(referralId, SubcontractStatus.RECEIVED, actorUserId, notes, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSubcontractResultsReturned(String referralId, String actorUserId, String notes) {
        transition(referralId, SubcontractStatus.RESULTS_RETURNED, actorUserId, notes, null);
    }

    @Override
    @Transactional
    public void closeSubcontract(String referralId, String actorUserId, String notes) {
        transition(referralId, SubcontractStatus.CLOSED, actorUserId, notes, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralStatusHistory> getSubcontractStatusHistory(String referralId) {
        return statusHistoryDAO.findByReferralIdOrderedByChangedAt(referralId);
    }

    private void transition(String referralId, SubcontractStatus target, String actorUserId, String notes,
            Timestamp handoffDatetime) {
        Referral referral = baseObjectDAO.getReferralById(referralId);
        if (referral == null) {
            throw new IllegalArgumentException("Referral not found: " + referralId);
        }
        ReferralSubcontract subcontract = referral.getSubcontract();
        if (subcontract == null) {
            // Historical pre-S-14 row — has no subcontract metadata to advance. No-op so
            // upstream auto-triggers (FHIR result import) don't break on legacy data.
            LogEvent.logDebug(this.getClass().getSimpleName(), "transition",
                    "skipping " + target + " transition: referral " + referralId + " has no subcontract row");
            return;
        }
        SubcontractStatus current = subcontract.getSubcontractStatus();
        if (!current.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Illegal subcontract transition for referral " + referralId + ": " + current + " -> " + target);
        }
        if (target == SubcontractStatus.DISPATCHED) {
            subcontract.setHandoffDatetime(handoffDatetime);
        }
        subcontract.setSubcontractStatus(target);
        subcontract.setSysUserId(actorUserId);
        referral.setSysUserId(actorUserId);
        baseObjectDAO.update(referral); // cascade flushes the subcontract update

        ReferralStatusHistory history = new ReferralStatusHistory();
        history.setReferralId(referralId);
        history.setFromStatus(current);
        history.setToStatus(target);
        history.setChangedByUserId(actorUserId);
        history.setChangedAt(DateUtil.getNowAsTimestamp());
        history.setNotes(notes);
        history.setSysUserId(actorUserId);
        statusHistoryDAO.insert(history);

        if (target == SubcontractStatus.DISPATCHED) {
            pushReferralToFhirStore(referral);
            fireSubcontractDispatchedAfterCommit(referral, actorUserId);
        }
    }

    /**
     * S-14 / OGC-624: DRAFT → DISPATCHED is the moment the receiving lab needs a
     * FHIR Task + ServiceRequest for this referral. Best-effort, mirrors the
     * try/catch shape in {@code ReferralSetServiceImpl.updateReferralSets}: a
     * FHIR-store outage logs an error but does NOT roll back the DB transition,
     * since the SUBCONTRACT_DISPATCHED notification (queued right after) and the
     * audit-history row are already committed, and rolling back would strand the
     * operator with no recovery path.
     */
    private void pushReferralToFhirStore(Referral referral) {
        try {
            fhirReferralService.referAnalysisesToOrganization(referral);
        } catch (TestNotFullyConfiguredException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "pushReferralToFhirStore",
                    "unable to automatically refer a test that does not have a loinc code set for referral "
                            + referral.getId());
        } catch (FhirLocalPersistingException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "pushReferralToFhirStore",
                    "FHIR store unavailable while dispatching referral " + referral.getId());
            LogEvent.logError(e);
        }
    }

    /**
     * Schedule a SUBCONTRACT_DISPATCHED notification fire post-commit. Building the
     * {@link NotificationContext} inside this transaction (while entity
     * associations are still attached to the session) avoids
     * LazyInitializationException on the async thread. Mirrors the proven
     * REFERRAL_OUT fire-after-commit pattern in {@code ReferralSetServiceImpl}.
     */
    private void fireSubcontractDispatchedAfterCommit(Referral referral, String actorUserId) {
        NotificationContext context = buildSubcontractDispatchedContext(referral, actorUserId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        notificationTriggerDispatcher.fire(context);
                    } catch (RuntimeException e) {
                        LogEvent.logError(ReferralServiceImpl.class.getSimpleName(),
                                "fireSubcontractDispatchedAfterCommit",
                                "failed to fire SUBCONTRACT_DISPATCHED for referral " + referral.getId());
                        LogEvent.logError(e);
                    }
                }
            });
        } else {
            notificationTriggerDispatcher.fire(context);
        }
    }

    private NotificationContext buildSubcontractDispatchedContext(Referral referral, String actorUserId) {
        Sample sample = null;
        if (referral.getAnalysis() != null && referral.getAnalysis().getSampleItem() != null) {
            sample = referral.getAnalysis().getSampleItem().getSample();
        }
        Organization receivingLab = referral.getOrganization();
        ReferralSubcontract subcontract = referral.getSubcontract();

        String accession = sample == null ? null : sample.getAccessionNumber();
        String testName = referral.getAnalysis() == null || referral.getAnalysis().getTest() == null ? null
                : referral.getAnalysis().getTest().getLocalizedTestName().getLocalizedValue();
        String expectedReturn = subcontract == null ? null : subcontract.getExpectedReturnDateForDisplay();

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("referralId", safe(referral.getId()));
        variables.put("orderId", safe(accession));
        variables.put("sampleAccessionNumber", safe(accession));
        variables.put("labName", receivingLab == null ? "" : safe(receivingLab.getOrganizationName()));
        variables.put("testName", safe(testName));
        variables.put("referredTests", resolveReferredTestsForSample(sample, safe(testName)));
        variables.put("referralDate", referral.getRequestDate() == null ? ""
                : DateUtil.convertTimestampToStringDate(referral.getRequestDate()));
        // expectedReturnDate alias kept for already-seeded templates referencing it.
        variables.put("expectedReturn", safe(expectedReturn));
        variables.put("expectedReturnDate", safe(expectedReturn));
        variables.put("sendingLabName",
                safe(ConfigurationProperties.getInstance().getPropertyValue(Property.configurationName)));
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
                LogEvent.logWarn(this.getClass().getSimpleName(), "buildSubcontractDispatchedContext",
                        "could not resolve patient for sample " + sample.getId());
            }
        }
        variables.put("patientFirstName", patientFirst);
        variables.put("patientLastName", patientLast);

        variables.put("agreementReference", subcontract == null ? "" : safe(subcontract.getAgreementReference()));
        variables.put("handoffDatetime", subcontract == null ? "" : safe(subcontract.getHandoffDatetimeForDisplay()));
        variables.put("cocContactName", subcontract == null ? "" : safe(subcontract.getCocContactName()));

        return new NotificationContext("SUBCONTRACT_DISPATCHED", referral, sample, receivingLab, actorUserId,
                variables);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private String resolveReferredTestsForSample(Sample sample, String fallbackTestName) {
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
            if (baseObjectDAO.getReferralsByAnalysisIds(java.util.Collections.singletonList(analysis.getId()))
                    .isEmpty()) {
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
}
