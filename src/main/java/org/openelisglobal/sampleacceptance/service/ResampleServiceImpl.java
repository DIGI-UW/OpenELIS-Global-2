package org.openelisglobal.sampleacceptance.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.notification.service.NotificationContext;
import org.openelisglobal.notification.service.NotificationTriggerDispatcher;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.sampleproject.service.SampleProjectService;
import org.openelisglobal.sampleproject.valueholder.SampleProject;
import org.openelisglobal.sampletyperequest.service.SampleTypeRequestService;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.vector.service.VectorPoolService;
import org.openelisglobal.vector.valueholder.VectorPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ResampleServiceImpl implements ResampleService {

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private NCEventService ncEventService;

    @Autowired
    private NceSpecimenService nceSpecimenService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private SampleOrganizationService sampleOrganizationService;

    @Autowired
    private SampleProjectService sampleProjectService;

    @Autowired
    private SampleTypeRequestService sampleTypeRequestService;

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Autowired
    private NotificationTriggerDispatcher notificationTriggerDispatcher;

    @Autowired
    private VectorPoolService vectorPoolService;

    @Override
    @Transactional
    public ResampleResult resample(String sampleItemId, String reason, Integer userId) {
        String sysUserId = userId != null ? String.valueOf(userId) : null;

        SampleItem originalItem = sampleItemService.get(sampleItemId);
        if (originalItem == null) {
            throw new LIMSRuntimeException("Cannot resample: sample item not found for id " + sampleItemId);
        }
        Sample original = originalItem.getSample();
        if (original == null) {
            throw new LIMSRuntimeException("Cannot resample: no parent sample for sample item " + sampleItemId);
        }
        String originalSampleId = original.getId();
        List<Analysis> originalAnalyses = analysisService.getAnalysesBySampleItem(originalItem);
        Date today = new Date(System.currentTimeMillis());
        String newAccession = generateAccessionNumber();

        // 1. Record the NCE and link it to the failed specimen.
        NcEvent nce = new NcEvent();
        nce.setNceNumber("RS-" + newAccession);
        nce.setDescription(reason);
        nce.setReportDate(today);
        nce.setDateOfEvent(today);
        nce.setSysUserId(sysUserId);
        Integer nceId = ncEventService.insert(nce);

        NceSpecimen specimen = new NceSpecimen();
        specimen.setNceId(nceId);
        specimen.setSampleItemId(Integer.valueOf(originalItem.getId()));
        specimen.setSysUserId(sysUserId);
        nceSpecimenService.insert(specimen);

        // 2. Create the replacement draft order, pre-populated from the parent order.
        String enteredStatusId = statusService.getStatusID(SampleStatus.Entered);
        Sample replacement = new Sample();
        replacement.setAccessionNumber(newAccession);
        replacement.setDomain(original.getDomain());
        replacement.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        replacement.setEnteredDate(today);
        replacement.setRevision("0");
        replacement.setStatusId(enteredStatusId);
        replacement.setResampledFromSampleId(originalSampleId);
        // Carry the order-context scalars that identify the order rather than the
        // physical specimen (FR-10.3). Collection/received timestamps are intentionally
        // NOT carried — they belong to the new physical collection.
        replacement.setClientReference(original.getClientReference());
        replacement.setPriority(original.getPriority());
        replacement.setGpsLatitude(original.getGpsLatitude());
        replacement.setGpsLongitude(original.getGpsLongitude());
        replacement.setGpsAccuracyMeters(original.getGpsAccuracyMeters());
        replacement.setGpsCaptureMethod(original.getGpsCaptureMethod());
        replacement.setSysUserId(sysUserId);
        String newSampleId = sampleService.insert(replacement);

        // 3. Carry the customer/requester onto the replacement so it is a usable order
        // (FR-10): the same patient + requesting provider (sample_human) and the
        // customer organization (sample_organization). Skipped for non-human domains
        // that have no such rows.
        Patient patient = sampleHumanService.getPatientForSample(original);
        if (patient != null) {
            SampleHuman sampleHuman = new SampleHuman();
            sampleHuman.setSampleId(newSampleId);
            sampleHuman.setPatientId(patient.getId());
            Provider provider = sampleHumanService.getProviderForSample(original);
            if (provider != null) {
                sampleHuman.setProviderId(provider.getId());
            }
            sampleHuman.setSysUserId(sysUserId);
            sampleHumanService.insert(sampleHuman);
        }
        SampleOrganization originalOrg = sampleOrganizationService.getDataBySample(original);
        if (originalOrg != null && originalOrg.getOrganization() != null) {
            SampleOrganization sampleOrg = new SampleOrganization();
            sampleOrg.setSample(replacement);
            sampleOrg.setOrganization(originalOrg.getOrganization());
            sampleOrg.setSampleOrganizationType(originalOrg.getSampleOrganizationType());
            sampleOrg.setSysUserId(sysUserId);
            sampleOrganizationService.insert(sampleOrg);
        }
        // Program / compliance-standard association (FR-10.3 "site / compliance
        // standard").
        SampleProject originalProject = sampleProjectService.getSampleProjectBySampleId(originalSampleId);
        if (originalProject != null && originalProject.getProject() != null) {
            SampleProject sampleProject = new SampleProject();
            sampleProject.setSample(replacement);
            sampleProject.setProject(originalProject.getProject());
            sampleProject.setIsPermanent(originalProject.getIsPermanent());
            sampleProject.setSysUserId(sysUserId);
            sampleProjectService.insert(sampleProject);
        }

        // 4. Clone ONLY the failed specimen and its tests onto the replacement as a
        // fresh draft. The specimen awaits collection (Entered) and analyses start
        // NotStarted via the standard fresh-order path, rather than inheriting the
        // original's statuses. Keep an old→new specimen map so the specimen's
        // observation answers can be remapped below.
        Map<String, SampleItem> oldItemIdToNewItem = new HashMap<>();
        SampleItem newItem = new SampleItem();
        newItem.setSample(replacement);
        newItem.setTypeOfSample(originalItem.getTypeOfSample());
        newItem.setSortOrder(originalItem.getSortOrder());
        newItem.setQuantity(originalItem.getQuantity());
        newItem.setSourceOfSampleId(originalItem.getSourceOfSampleId());
        newItem.setStatusId(enteredStatusId);
        newItem.setSysUserId(sysUserId);
        sampleItemService.insert(newItem);
        oldItemIdToNewItem.put(originalItem.getId(), newItem);

        for (Analysis originalAnalysis : originalAnalyses) {
            Analysis newAnalysis = analysisService.buildAnalysis(originalAnalysis.getTest(), newItem);
            newAnalysis.setSysUserId(sysUserId);
            analysisService.insert(newAnalysis);
        }

        // 5. Carry the questionnaire / default-conditions answers (observation_history)
        // onto the replacement (FR-10.3): order-level answers (no specimen) carry
        // as-is; the resampled specimen's answers remap to the new specimen; answers
        // belonging to the order's OTHER specimens are dropped.
        for (ObservationHistory obs : observationHistoryService.getObservationHistoriesBySampleId(originalSampleId)) {
            if (obs.getSampleItemId() != null && oldItemIdToNewItem.get(obs.getSampleItemId()) == null) {
                continue;
            }
            ObservationHistory newObs = new ObservationHistory();
            newObs.setSampleId(newSampleId);
            newObs.setPatientId(obs.getPatientId());
            newObs.setObservationHistoryTypeId(obs.getObservationHistoryTypeId());
            newObs.setValue(obs.getValue());
            newObs.setValueType(obs.getValueType());
            if (obs.getSampleItemId() != null) {
                newObs.setSampleItemId(oldItemIdToNewItem.get(obs.getSampleItemId()).getId());
            }
            newObs.setSysUserId(sysUserId);
            observationHistoryService.insert(newObs);
        }

        // 6. Reject the failed specimen (NOT the whole order) and cancel its analyses,
        // so the order's accepted specimens proceed while this one is removed from the
        // workflow. Set the bidirectional resample links between the parent order and
        // the replacement for cross-reference.
        rejectSampleItem(originalItem, originalAnalyses, sysUserId);

        // Cancel a matching-type sample_type_request so the request-based Enter
        // Order / Collect views drop the rejected type (its replacement lives on the
        // new order); otherwise those steps re-load from sample_type_request and the
        // rejected specimen reappears. In this decoupled workflow requests are NOT
        // linked to sample_items, so match by type and cancel a single still-active
        // request of the rejected specimen's type.
        String rejectedTypeId = originalItem.getTypeOfSampleId();
        if (rejectedTypeId != null) {
            for (SampleTypeRequest request : sampleTypeRequestService.getRequestsBySampleId(originalSampleId)) {
                if (request.getStatus() == SampleTypeRequest.Status.CANCELLED) {
                    continue;
                }
                TypeOfSample requestType = request.getTypeOfSample();
                if (requestType != null && rejectedTypeId.equals(requestType.getId())) {
                    request.setStatus(SampleTypeRequest.Status.CANCELLED);
                    request.setSysUserId(sysUserId);
                    sampleTypeRequestService.update(request);
                    break;
                }
            }
        }

        original.setResampledToSampleId(newSampleId);
        original.setSysUserId(sysUserId);
        sampleService.update(original);

        // 7. Notify the requester (FR-12) through the existing Notification Trigger
        // system. The context is built now, in-transaction, while associations are
        // attached; the dispatch fires post-commit so the committed order is visible to
        // the async dispatcher. Inert until an admin enables the SAMPLE_RESAMPLED
        // trigger; send failures follow the existing retry policy (no rollback).
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("labNumber", safe(original.getAccessionNumber()));
        variables.put("sampleAccessionNumber", safe(original.getAccessionNumber()));
        variables.put("reason", safe(reason));
        variables.put("newLabNumber", safe(newAccession));
        variables.put("newSampleId", safe(newSampleId));
        NotificationContext notificationContext = new NotificationContext("SAMPLE_RESAMPLED", null, original, null,
                sysUserId, variables);
        fireResampledNotificationAfterCommit(notificationContext, originalSampleId);

        return new ResampleResult(originalSampleId, newSampleId, newAccession, nceId);
    }

    @Override
    @Transactional
    public void reject(String sampleItemId, String reason, Integer userId) {
        SampleItem item = sampleItemService.get(sampleItemId);
        if (item == null) {
            throw new IllegalArgumentException("Cannot reject: sample item not found for id " + sampleItemId);
        }
        // A fanned-out pool member cannot be rejected in isolation — its tests live on
        // the pool, not the member — so rejecting any member rejects the whole pool
        // (the pool is the unit of acceptance for vector). Non-pooled specimens
        // (Clinical/Env, or a single-organism vector order) reject directly.
        VectorPool pool = vectorPoolService.getIntakePoolBySampleItemId(sampleItemId);
        if (pool != null) {
            rejectPool(pool.getId(), reason, userId);
            return;
        }
        String sysUserId = userId != null ? String.valueOf(userId) : null;
        rejectSampleItem(item, analysisService.getAnalysesBySampleItem(item), sysUserId);
    }

    @Override
    @Transactional
    public void rejectPool(Integer vectorPoolId, String reason, Integer userId) {
        if (vectorPoolId == null) {
            throw new IllegalArgumentException("Cannot reject pool: null vector pool id");
        }
        List<SampleItem> members = vectorPoolService.getMembersByPoolId(vectorPoolId);
        if (members.isEmpty()) {
            throw new IllegalArgumentException("Cannot reject pool: no members for vector pool " + vectorPoolId);
        }
        String sysUserId = userId != null ? String.valueOf(userId) : null;
        // Vector tests are keyed to the pool (analysis.vector_pool_id, sample_item
        // null after fan-out), so cancel those here; then mark every live member
        // rejected (all-or-nothing). No replacement order — a field catch can't be
        // re-collected.
        cancelAnalyses(analysisService.getAnalysesByVectorPoolId(String.valueOf(vectorPoolId)), sysUserId);
        for (SampleItem member : members) {
            if (member.isVoided() || member.isRejected()) {
                continue;
            }
            rejectSampleItem(member, analysisService.getAnalysesBySampleItem(member), sysUserId);
        }
    }

    /**
     * Reject one specimen: cancel its analyses and flag the {@code sample_item}
     * rejected. Shared by {@link #resample} (which also builds a replacement) and
     * the plain {@link #reject}/{@link #rejectPool} paths (no replacement). The
     * caller supplies the specimen's analyses — resample already holds them; the
     * plain paths fetch them.
     */
    private void rejectSampleItem(SampleItem item, List<Analysis> analyses, String sysUserId) {
        cancelAnalyses(analyses, sysUserId);
        item.setRejected(true);
        item.setSysUserId(sysUserId);
        sampleItemService.update(item);
    }

    private void cancelAnalyses(List<Analysis> analyses, String sysUserId) {
        String canceledStatusId = statusService.getStatusID(AnalysisStatus.Canceled);
        for (Analysis analysis : analyses) {
            analysis.setStatusId(canceledStatusId);
            analysis.setSysUserId(sysUserId);
            analysisService.update(analysis);
        }
    }

    /**
     * Fire the SAMPLE_RESAMPLED notification once the resample transaction commits,
     * so the new order is visible to the async dispatcher and a notification
     * failure never rolls back the resample. Mirrors the proven fire-after-commit
     * pattern in {@code ReferralServiceImpl}.
     */
    private void fireResampledNotificationAfterCommit(NotificationContext context, String originalSampleId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        notificationTriggerDispatcher.fire(context);
                    } catch (RuntimeException e) {
                        LogEvent.logError(ResampleServiceImpl.class.getSimpleName(),
                                "fireResampledNotificationAfterCommit",
                                "failed to fire SAMPLE_RESAMPLED for sample " + originalSampleId);
                        LogEvent.logError(e);
                    }
                }
            });
        } else {
            notificationTriggerDispatcher.fire(context);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * Mint a fresh lab number via the configured accession generator. Fails loud
     * rather than fabricating a value: a resample without a valid,
     * format-conformant lab number must roll back instead of minting an accession
     * that bypasses the configured format, sequence, and reservation.
     */
    private String generateAccessionNumber() {
        IAccessionNumberGenerator generator = AccessionNumberUtil.getMainAccessionNumberGenerator();
        if (generator == null) {
            throw new LIMSRuntimeException(
                    "Cannot resample: no main accession number generator is configured; refusing to mint a"
                            + " non-conformant lab number");
        }
        String accession = generator.getNextAvailableAccessionNumber(null, true);
        if (accession == null || accession.isBlank()) {
            throw new LIMSRuntimeException(
                    "Cannot resample: accession number generator returned no value for the replacement order");
        }
        return accession;
    }
}
