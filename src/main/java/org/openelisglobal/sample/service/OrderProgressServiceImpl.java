package org.openelisglobal.sample.service;

import java.sql.Timestamp;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.sample.valueholder.OrderProgressStatus;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceChecklistService;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceEvaluation;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceRecordService;
import org.openelisglobal.sampleacceptance.valueholder.SampleAcceptanceRecord;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderProgressServiceImpl implements OrderProgressService {

    static final String MODE_MANDATORY = "MANDATORY";
    static final String MODE_OFF = "OFF";
    static final String CLINICAL = "clinical";

    @Autowired
    private SampleService sampleService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private SampleAcceptanceChecklistService acceptanceChecklistService;
    @Autowired
    private SampleAcceptanceRecordService acceptanceRecordService;
    @Autowired
    @Lazy
    private ReferralService referralService;
    @Autowired
    private ObservationHistoryService observationHistoryService;

    /**
     * The save may hand over the stored order itself or a detached copy of it (an
     * order reopened for editing, or the shell the FHIR ServiceRequest create
     * builds), so the progress and the storage decision are written to the stored
     * row, and the copy is brought up to date with what was stored.
     */
    @Override
    @Transactional
    public void recordStepSave(Sample sample, String progressStep, Boolean storageSkipped) {
        if (sample == null) {
            return;
        }
        Sample stored = sample.getId() == null ? null : sampleService.get(sample.getId());
        Sample target = stored == null ? sample : stored;
        OrderProgressStatus current = OrderProgressStatus.fromStored(target.getOrderProgressStatus());
        if (current == OrderProgressStatus.CANCELLED) {
            throw new IllegalArgumentException("order.cancelled");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        boolean changed = false;
        if (storageSkipped != null && !storageSkipped.equals(target.getStorageSkipped())) {
            target.setStorageSkipped(storageSkipped);
            changed = true;
        }
        if (current == null) {
            target.setOrderProgressStatus(OrderProgressStatus.ENTERED.name());
            target.setOrderEnteredAt(now);
            current = OrderProgressStatus.ENTERED;
            changed = true;
        }
        if (OrderProgressStatus.SAMPLES_PREPARED.name().equals(progressStep)
                && current == OrderProgressStatus.ENTERED) {
            target.setOrderProgressStatus(OrderProgressStatus.SAMPLES_PREPARED.name());
            target.setOrderPreparedAt(now);
            changed = true;
        }
        if (changed && target.getId() != null) {
            Sample merged = sampleService.update(target);
            if (merged != null) {
                target.setLastupdated(merged.getLastupdated());
            }
        }
        if (target != sample) {
            sample.setOrderProgressStatus(target.getOrderProgressStatus());
            sample.setOrderEnteredAt(target.getOrderEnteredAt());
            sample.setOrderPreparedAt(target.getOrderPreparedAt());
            sample.setStorageSkipped(target.getStorageSkipped());
            sample.setLastupdated(target.getLastupdated());
        }
    }

    @Override
    public OrderProgressStatus statusOf(Sample sample, boolean legacyPrepared, boolean legacyReleased) {
        OrderProgressStatus stored = OrderProgressStatus.fromStored(sample.getOrderProgressStatus());
        if (stored != null) {
            return stored;
        }
        if (legacyReleased) {
            return OrderProgressStatus.READY_FOR_TESTING;
        }
        return legacyPrepared ? OrderProgressStatus.SAMPLES_PREPARED : OrderProgressStatus.ENTERED;
    }

    @Override
    public boolean sampleCheckEnabled(String workflowType) {
        String domain = GenericValidator.isBlankOrNull(workflowType) ? "clinical" : workflowType;
        return !MODE_OFF.equalsIgnoreCase(acceptanceChecklistService.getEnforcement(domain));
    }

    @Override
    public boolean isComplete(OrderProgressStatus status, String workflowType) {
        if (status == OrderProgressStatus.READY_FOR_TESTING) {
            return true;
        }
        return status == OrderProgressStatus.SAMPLES_PREPARED && !sampleCheckEnabled(workflowType);
    }

    @Override
    public boolean isFullyReferred(String sampleId) {
        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sampleId);
        if (analyses == null) {
            return false;
        }
        boolean anyActive = false;
        for (Analysis analysis : analyses) {
            if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Canceled)) {
                continue;
            }
            anyActive = true;
            if (!isOpenReferral(referralService.getReferralByAnalysisId(analysis.getId()))) {
                return false;
            }
        }
        return anyActive;
    }

    @Override
    public boolean isOpenReferral(Referral referral) {
        return referral != null && referral.getId() != null && referral.getStatus() != ReferralStatus.CANCELLED
                && referral.getStatus() != ReferralStatus.REJECTED;
    }

    @Override
    public boolean isComplete(Sample sample, OrderProgressStatus status, String workflowType) {
        if (isComplete(status, workflowType)) {
            return true;
        }
        return sample != null && isComplete(status, workflowType, isFullyReferred(sample.getId()));
    }

    @Override
    public boolean isComplete(OrderProgressStatus status, String workflowType, boolean fullyReferred) {
        if (isComplete(status, workflowType)) {
            return true;
        }
        return fullyReferred && status != null && status.isAtLeast(OrderProgressStatus.SAMPLES_PREPARED);
    }

    @Override
    @Transactional
    public Sample release(String sampleId, String releaseNote, String sysUserId) {
        Sample sample = sampleService.get(sampleId);
        if (sample == null) {
            throw new IllegalArgumentException("order.notFound");
        }
        OrderProgressStatus current = OrderProgressStatus.fromStored(sample.getOrderProgressStatus());
        if (current == OrderProgressStatus.READY_FOR_TESTING) {
            return sample;
        }
        if (current == OrderProgressStatus.CANCELLED) {
            throw new IllegalStateException("order.cancelled");
        }
        String workflowType = workflowTypeOf(sample);
        if (CLINICAL.equals(workflowType) && current != OrderProgressStatus.SAMPLES_PREPARED) {
            throw new IllegalStateException("order.release.prepareIncomplete");
        }
        String mode = acceptanceChecklistService.getEnforcement(workflowType);
        if (MODE_MANDATORY.equalsIgnoreCase(mode)) {
            acceptanceRecordService.enforceAcceptanceGateForOrder(sampleId);
        } else if (!MODE_OFF.equalsIgnoreCase(mode) && hasUnansweredAcceptance(sampleId)
                && GenericValidator.isBlankOrNull(releaseNote)) {
            throw new IllegalArgumentException("order.release.reasonRequired");
        }
        sample.setOrderProgressStatus(OrderProgressStatus.READY_FOR_TESTING.name());
        sample.setOrderReadyAt(new Timestamp(System.currentTimeMillis()));
        sample.setOrderReleaseNote(GenericValidator.isBlankOrNull(releaseNote) ? null : releaseNote.trim());
        sample.setSysUserId(sysUserId);
        sampleService.update(sample);
        return sample;
    }

    @Override
    @Transactional
    public Sample cancel(String sampleId, String reason, String sysUserId) {
        if (GenericValidator.isBlankOrNull(reason)) {
            throw new IllegalArgumentException("order.cancel.reasonRequired");
        }
        Sample sample = sampleService.get(sampleId);
        if (sample == null) {
            throw new IllegalArgumentException("order.notFound");
        }
        OrderProgressStatus current = OrderProgressStatus.fromStored(sample.getOrderProgressStatus());
        if (current == OrderProgressStatus.CANCELLED) {
            throw new IllegalStateException("order.cancelled");
        }
        if (isComplete(sample, current, workflowTypeOf(sample))) {
            throw new IllegalStateException("order.cancel.complete");
        }
        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sampleId);
        for (Analysis analysis : analyses) {
            if (!statusService.matches(analysis.getStatusId(), AnalysisStatus.NotStarted)
                    && !statusService.matches(analysis.getStatusId(), AnalysisStatus.Canceled)) {
                throw new IllegalStateException("order.cancel.inProgress");
            }
        }
        String cancelledStatusId = statusService.getStatusID(AnalysisStatus.Canceled);
        for (Analysis analysis : analyses) {
            if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Canceled)) {
                continue;
            }
            analysis.setStatusId(cancelledStatusId);
            analysis.setSysUserId(sysUserId);
            analysisService.update(analysis);
        }
        String cancelledSampleStatusId = statusService.getStatusID(SampleStatus.Canceled);
        if (!GenericValidator.isBlankOrNull(cancelledSampleStatusId) && !"-1".equals(cancelledSampleStatusId)) {
            for (SampleItem item : sampleItemService.getSampleItemsBySampleId(sampleId)) {
                if (cancelledSampleStatusId.equals(item.getStatusId())) {
                    continue;
                }
                item.setStatusId(cancelledSampleStatusId);
                item.setSysUserId(sysUserId);
                sampleItemService.update(item);
            }
        }
        sample.setOrderProgressStatus(OrderProgressStatus.CANCELLED.name());
        sample.setOrderCancelledAt(new Timestamp(System.currentTimeMillis()));
        sample.setOrderCancelledBy(sysUserId);
        sample.setOrderCancelReason(reason.trim());
        sample.setSysUserId(sysUserId);
        sampleService.update(sample);
        return sample;
    }

    /**
     * Under Optional acceptance a release is free once every specimen's checklist
     * is answered; an unanswered (pending) specimen asks for a reason.
     */
    private boolean hasUnansweredAcceptance(String sampleId) {
        List<SampleAcceptanceEvaluation> evaluations = acceptanceRecordService.evaluateOrder(sampleId);
        if (evaluations == null) {
            return false;
        }
        return evaluations.stream()
                .anyMatch(evaluation -> SampleAcceptanceRecord.STATUS_PENDING.equals(evaluation.getOverallStatus()));
    }

    private String workflowTypeOf(Sample sample) {
        String stored = observationHistoryService.getRawValueForSample(ObservationType.ENV_WORKFLOW_TYPE,
                sample.getId());
        return GenericValidator.isBlankOrNull(stored) ? "clinical" : stored;
    }
}
