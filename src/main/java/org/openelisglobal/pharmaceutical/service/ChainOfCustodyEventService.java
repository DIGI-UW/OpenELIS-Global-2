package org.openelisglobal.pharmaceutical.service;

import java.util.List;
import org.openelisglobal.pharmaceutical.valueholder.ChainOfCustodyEvent;

public interface ChainOfCustodyEventService {

    ChainOfCustodyEvent get(Integer id);

    List<ChainOfCustodyEvent> getAll();

    ChainOfCustodyEvent save(ChainOfCustodyEvent event);

    ChainOfCustodyEvent update(ChainOfCustodyEvent event);

    void delete(Integer id);

    List<ChainOfCustodyEvent> findBySampleId(Integer sampleId);

    List<ChainOfCustodyEvent> findByAliquotId(Integer aliquotId);

    List<ChainOfCustodyEvent> findByAction(ChainOfCustodyEvent.CustodyAction action);

    List<ChainOfCustodyEvent> findPendingApprovals();

    ChainOfCustodyEvent recordCustodyEvent(Integer sampleId, Integer aliquotId,
            ChainOfCustodyEvent.CustodyAction action, String comments, String userId);

    ChainOfCustodyEvent recordTransfer(Integer sampleId, Integer aliquotId,
            String fromLocation, String toLocation, String userId);

    ChainOfCustodyEvent approveEvent(Integer eventId, String approverId);

    ChainOfCustodyEvent rejectEvent(Integer eventId, String rejectionReason, String approverId);

    List<ChainOfCustodyEvent> getFullCustodyChain(Integer sampleId);
}
