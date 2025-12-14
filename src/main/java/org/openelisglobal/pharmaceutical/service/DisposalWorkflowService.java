package org.openelisglobal.pharmaceutical.service;

import java.util.List;
import org.openelisglobal.pharmaceutical.valueholder.DisposalRecord;

public interface DisposalWorkflowService {

    DisposalRecord get(Integer id);

    List<DisposalRecord> getAll();

    DisposalRecord save(DisposalRecord disposalRecord);

    DisposalRecord update(DisposalRecord disposalRecord);

    void delete(Integer id);

    List<DisposalRecord> findBySampleId(Integer sampleId);

    List<DisposalRecord> findByStatus(DisposalRecord.DisposalStatus status);

    List<DisposalRecord> findPendingApprovals();

    DisposalRecord requestDisposal(Integer sampleId, DisposalRecord.DisposalReason reason,
            DisposalRecord.DisposalMethod method, String justification, String userId);

    DisposalRecord approveDisposal(Integer disposalRecordId, String approverId);

    DisposalRecord rejectDisposal(Integer disposalRecordId, String rejectionReason, String approverId);

    DisposalRecord executeDisposal(Integer disposalRecordId, String witnessId, String disposalNotes, String userId);

    DisposalRecord scheduleDisposal(Integer disposalRecordId, java.sql.Timestamp scheduledDate, String userId);

    boolean canRequestDisposal(Integer sampleId);

    String generateDisposalCertificate(Integer disposalRecordId);
}
