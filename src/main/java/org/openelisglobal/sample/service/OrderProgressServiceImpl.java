package org.openelisglobal.sample.service;

import java.sql.Timestamp;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.qachecklist.service.SampleQaChecklistService;
import org.openelisglobal.sample.valueholder.OrderProgressStatus;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceChecklistService;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderProgressServiceImpl implements OrderProgressService {

    static final String MODE_MANDATORY = "MANDATORY";
    static final String MODE_OFF = "OFF";

    @Autowired
    private SampleService sampleService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private SampleAcceptanceChecklistService acceptanceChecklistService;
    @Autowired
    private SampleAcceptanceRecordService acceptanceRecordService;
    @Autowired
    private SampleQaChecklistService qaChecklistService;
    @Autowired
    private ObservationHistoryService observationHistoryService;

    /**
     * The save may hand over the stored order itself or a detached shell of it that
     * only carries the id (the FHIR ServiceRequest create builds one), so the
     * progress is read from and written to the stored row, and the shell is brought
     * up to date with what was stored.
     */
    @Override
    @Transactional
    public void recordStepSave(Sample sample, String progressStep) {
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
        if (current != OrderProgressStatus.SAMPLES_PREPARED) {
            throw new IllegalStateException("order.release.prepareIncomplete");
        }
        String mode = acceptanceChecklistService.getEnforcement(workflowTypeOf(sample));
        if (MODE_MANDATORY.equalsIgnoreCase(mode)) {
            acceptanceRecordService.enforceAcceptanceGateForOrder(sampleId);
        } else if (!MODE_OFF.equalsIgnoreCase(mode)
                && !qaChecklistService.areAllItemsVerified(Integer.valueOf(sampleId))
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
        if (isComplete(current, workflowTypeOf(sample))) {
            throw new IllegalStateException("order.cancel.complete");
        }
        String cancelledStatusId = statusService.getStatusID(AnalysisStatus.Canceled);
        for (Analysis analysis : analysisService.getAnalysesBySampleId(sampleId)) {
            if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)
                    || statusService.matches(analysis.getStatusId(), AnalysisStatus.Canceled)) {
                continue;
            }
            analysis.setStatusId(cancelledStatusId);
            analysis.setSysUserId(sysUserId);
            analysisService.update(analysis);
        }
        sample.setOrderProgressStatus(OrderProgressStatus.CANCELLED.name());
        sample.setOrderCancelledAt(new Timestamp(System.currentTimeMillis()));
        sample.setOrderCancelledBy(sysUserId);
        sample.setOrderCancelReason(reason.trim());
        String cancelledSampleStatusId = statusService.getStatusID(SampleStatus.Canceled);
        if (!GenericValidator.isBlankOrNull(cancelledSampleStatusId) && !"-1".equals(cancelledSampleStatusId)) {
            sample.setStatusId(cancelledSampleStatusId);
        }
        sample.setSysUserId(sysUserId);
        sampleService.update(sample);
        return sample;
    }

    private String workflowTypeOf(Sample sample) {
        String stored = observationHistoryService.getRawValueForSample(ObservationType.ENV_WORKFLOW_TYPE,
                sample.getId());
        return GenericValidator.isBlankOrNull(stored) ? "clinical" : stored;
    }
}
