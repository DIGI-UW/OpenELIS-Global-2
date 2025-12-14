package org.openelisglobal.pharmaceutical.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.dao.ChainOfCustodyEventDAO;
import org.openelisglobal.pharmaceutical.dao.DisposalRecordDAO;
import org.openelisglobal.pharmaceutical.dao.PharmaceuticalSampleDAO;
import org.openelisglobal.pharmaceutical.valueholder.ChainOfCustodyEvent;
import org.openelisglobal.pharmaceutical.valueholder.DisposalRecord;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DisposalWorkflowServiceImpl implements DisposalWorkflowService {

    @Autowired
    private DisposalRecordDAO disposalRecordDAO;

    @Autowired
    private PharmaceuticalSampleDAO pharmaceuticalSampleDAO;

    @Autowired
    private ChainOfCustodyEventDAO chainOfCustodyEventDAO;

    @Override
    @Transactional(readOnly = true)
    public DisposalRecord get(Integer id) {
        return disposalRecordDAO.get(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisposalRecord> getAll() {
        return disposalRecordDAO.getAll();
    }

    @Override
    public DisposalRecord save(DisposalRecord disposalRecord) {
        Integer id = disposalRecordDAO.insert(disposalRecord);
        disposalRecord.setId(id);
        return disposalRecord;
    }

    @Override
    public DisposalRecord update(DisposalRecord disposalRecord) {
        disposalRecordDAO.update(disposalRecord);
        return disposalRecord;
    }

    @Override
    public void delete(Integer id) {
        DisposalRecord disposalRecord = get(id);
        if (disposalRecord != null) {
            disposalRecordDAO.delete(disposalRecord);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisposalRecord> findBySampleId(Integer sampleId) {
        return disposalRecordDAO.findBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisposalRecord> findByStatus(DisposalRecord.DisposalStatus status) {
        return disposalRecordDAO.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisposalRecord> findPendingApprovals() {
        return disposalRecordDAO.findPendingApprovals();
    }

    @Override
    public DisposalRecord requestDisposal(Integer sampleId, DisposalRecord.DisposalReason reason,
            DisposalRecord.DisposalMethod method, String justification, String userId) {

        if (!canRequestDisposal(sampleId)) {
            throw new LIMSRuntimeException("Sample cannot be disposed: active testing or pending results");
        }

        PharmaceuticalSample sample = pharmaceuticalSampleDAO.get(sampleId).orElse(null);
        if (sample == null) {
            throw new LIMSRuntimeException("Sample not found: " + sampleId);
        }

        DisposalRecord record = new DisposalRecord();
        record.setSampleId(sampleId);
        record.setReason(reason);
        record.setMethod(method);
        record.setJustification(justification);
        record.setStatus(DisposalRecord.DisposalStatus.PENDING_APPROVAL);
        record.setRequestedAt(new Timestamp(System.currentTimeMillis()));
        record.setRequestedBy(userId);
        record.setSysUserId(userId);

        Integer id = disposalRecordDAO.insert(record);
        record.setId(id);

        sample.setStatus(PharmaceuticalSample.SampleStatus.PENDING_DISPOSAL);
        sample.setSysUserId(userId);
        pharmaceuticalSampleDAO.update(sample);

        recordCustodyEvent(sampleId, ChainOfCustodyEvent.CustodyAction.DISPOSED,
                "Disposal requested: " + reason + " - " + justification, userId);

        return record;
    }

    @Override
    public DisposalRecord approveDisposal(Integer disposalRecordId, String approverId) {
        DisposalRecord record = get(disposalRecordId);
        if (record == null) {
            throw new LIMSRuntimeException("Disposal record not found: " + disposalRecordId);
        }

        if (record.getStatus() != DisposalRecord.DisposalStatus.PENDING_APPROVAL) {
            throw new LIMSRuntimeException("Disposal record is not pending approval");
        }

        record.setStatus(DisposalRecord.DisposalStatus.APPROVED);
        record.setApprovedAt(new Timestamp(System.currentTimeMillis()));
        record.setApprovedBy(approverId);
        record.setSysUserId(approverId);
        disposalRecordDAO.update(record);

        recordCustodyEvent(record.getSampleId(), ChainOfCustodyEvent.CustodyAction.DISPOSED,
                "Disposal approved by " + approverId, approverId);

        return record;
    }

    @Override
    public DisposalRecord rejectDisposal(Integer disposalRecordId, String rejectionReason, String approverId) {
        DisposalRecord record = get(disposalRecordId);
        if (record == null) {
            throw new LIMSRuntimeException("Disposal record not found: " + disposalRecordId);
        }

        if (record.getStatus() != DisposalRecord.DisposalStatus.PENDING_APPROVAL) {
            throw new LIMSRuntimeException("Disposal record is not pending approval");
        }

        record.setStatus(DisposalRecord.DisposalStatus.REJECTED);
        record.setApprovedAt(new Timestamp(System.currentTimeMillis()));
        record.setApprovedBy(approverId);
        String existingJustification = record.getJustification();
        if (existingJustification != null && !existingJustification.isEmpty()) {
            record.setJustification(existingJustification + "\n\nRejection reason: " + rejectionReason);
        } else {
            record.setJustification("Rejection reason: " + rejectionReason);
        }
        record.setSysUserId(approverId);
        disposalRecordDAO.update(record);

        PharmaceuticalSample sample = pharmaceuticalSampleDAO.get(record.getSampleId()).orElse(null);
        if (sample != null) {
            sample.setStatus(PharmaceuticalSample.SampleStatus.IN_STORAGE);
            sample.setSysUserId(approverId);
            pharmaceuticalSampleDAO.update(sample);
        }

        return record;
    }

    @Override
    public DisposalRecord executeDisposal(Integer disposalRecordId, String witnessId,
            String disposalNotes, String userId) {
        DisposalRecord record = get(disposalRecordId);
        if (record == null) {
            throw new LIMSRuntimeException("Disposal record not found: " + disposalRecordId);
        }

        if (record.getStatus() != DisposalRecord.DisposalStatus.APPROVED
                && record.getStatus() != DisposalRecord.DisposalStatus.SCHEDULED) {
            throw new LIMSRuntimeException("Disposal record must be approved or scheduled before execution");
        }

        record.setStatus(DisposalRecord.DisposalStatus.COMPLETED);
        record.setExecutedAt(new Timestamp(System.currentTimeMillis()));
        record.setExecutedBy(userId);
        record.setWitnessId(witnessId);
        if (disposalNotes != null && !disposalNotes.isEmpty()) {
            String existingJustification = record.getJustification();
            if (existingJustification != null && !existingJustification.isEmpty()) {
                record.setJustification(existingJustification + "\n\nDisposal notes: " + disposalNotes);
            } else {
                record.setJustification("Disposal notes: " + disposalNotes);
            }
        }
        record.setSysUserId(userId);
        disposalRecordDAO.update(record);

        PharmaceuticalSample sample = pharmaceuticalSampleDAO.get(record.getSampleId()).orElse(null);
        if (sample != null) {
            sample.setStatus(PharmaceuticalSample.SampleStatus.DISPOSED);
            sample.setSysUserId(userId);
            pharmaceuticalSampleDAO.update(sample);
        }

        recordCustodyEvent(record.getSampleId(), ChainOfCustodyEvent.CustodyAction.DISPOSED,
                "Disposal executed. Method: " + record.getMethod() + ". Witness: " + witnessId, userId);

        return record;
    }

    @Override
    public DisposalRecord scheduleDisposal(Integer disposalRecordId, Timestamp scheduledDate, String userId) {
        DisposalRecord record = get(disposalRecordId);
        if (record == null) {
            throw new LIMSRuntimeException("Disposal record not found: " + disposalRecordId);
        }

        if (record.getStatus() != DisposalRecord.DisposalStatus.APPROVED) {
            throw new LIMSRuntimeException("Disposal must be approved before scheduling");
        }

        record.setStatus(DisposalRecord.DisposalStatus.SCHEDULED);
        record.setScheduledAt(scheduledDate);
        record.setSysUserId(userId);
        disposalRecordDAO.update(record);

        return record;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canRequestDisposal(Integer sampleId) {
        PharmaceuticalSample sample = pharmaceuticalSampleDAO.get(sampleId).orElse(null);
        if (sample == null) {
            return false;
        }

        PharmaceuticalSample.SampleStatus status = sample.getStatus();
        if (status == PharmaceuticalSample.SampleStatus.IN_TESTING
                || status == PharmaceuticalSample.SampleStatus.PENDING_DISPOSAL
                || status == PharmaceuticalSample.SampleStatus.DISPOSED) {
            return false;
        }

        List<DisposalRecord> existingRecords = disposalRecordDAO.findBySampleId(sampleId);
        for (DisposalRecord record : existingRecords) {
            if (record.getStatus() == DisposalRecord.DisposalStatus.PENDING_APPROVAL
                    || record.getStatus() == DisposalRecord.DisposalStatus.APPROVED
                    || record.getStatus() == DisposalRecord.DisposalStatus.SCHEDULED) {
                return false;
            }
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public String generateDisposalCertificate(Integer disposalRecordId) {
        DisposalRecord record = get(disposalRecordId);
        if (record == null) {
            throw new LIMSRuntimeException("Disposal record not found: " + disposalRecordId);
        }

        if (record.getStatus() != DisposalRecord.DisposalStatus.COMPLETED) {
            throw new LIMSRuntimeException("Disposal must be completed before generating certificate");
        }

        PharmaceuticalSample sample = pharmaceuticalSampleDAO.get(record.getSampleId()).orElse(null);

        StringBuilder certificate = new StringBuilder();
        certificate.append("DISPOSAL CERTIFICATE\n");
        certificate.append("====================\n\n");
        certificate.append("Certificate Number: DISP-").append(disposalRecordId).append("\n");
        certificate.append("Date Generated: ").append(new Timestamp(System.currentTimeMillis())).append("\n\n");

        if (sample != null) {
            certificate.append("Sample Information:\n");
            certificate.append("  Sample ID: ").append(sample.getUniqueSampleId()).append("\n");
            certificate.append("  Sample Name: ").append(sample.getSampleName()).append("\n");
            certificate.append("  Lot/Batch: ").append(sample.getLotBatch()).append("\n\n");
        }

        certificate.append("Disposal Details:\n");
        certificate.append("  Reason: ").append(record.getReason()).append("\n");
        certificate.append("  Method: ").append(record.getMethod()).append("\n");
        certificate.append("  Requested By: ").append(record.getRequestedBy()).append("\n");
        certificate.append("  Requested At: ").append(record.getRequestedAt()).append("\n");
        certificate.append("  Approved By: ").append(record.getApprovedBy()).append("\n");
        certificate.append("  Approved At: ").append(record.getApprovedAt()).append("\n");
        certificate.append("  Executed By: ").append(record.getExecutedBy()).append("\n");
        certificate.append("  Executed At: ").append(record.getExecutedAt()).append("\n");
        certificate.append("  Witness: ").append(record.getWitnessId()).append("\n\n");

        certificate.append("This certificate confirms the proper disposal of the above sample\n");
        certificate.append("in accordance with applicable regulations and procedures.\n");

        return certificate.toString();
    }

    private void recordCustodyEvent(Integer sampleId, ChainOfCustodyEvent.CustodyAction action,
            String comments, String userId) {
        ChainOfCustodyEvent event = new ChainOfCustodyEvent();
        event.setSampleId(sampleId);
        event.setAction(action);
        event.setComments(comments);
        event.setEventTimestamp(new Timestamp(System.currentTimeMillis()));
        event.setPerformedBy(userId);
        event.setApprovalStatus(ChainOfCustodyEvent.ApprovalStatus.NOT_REQUIRED);
        event.setSysUserId(userId);
        chainOfCustodyEventDAO.insert(event);
    }
}
