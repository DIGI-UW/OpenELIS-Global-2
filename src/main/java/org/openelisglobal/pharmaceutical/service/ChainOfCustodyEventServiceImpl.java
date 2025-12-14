package org.openelisglobal.pharmaceutical.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.dao.AliquotDAO;
import org.openelisglobal.pharmaceutical.dao.ChainOfCustodyEventDAO;
import org.openelisglobal.pharmaceutical.valueholder.Aliquot;
import org.openelisglobal.pharmaceutical.valueholder.ChainOfCustodyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChainOfCustodyEventServiceImpl implements ChainOfCustodyEventService {

    @Autowired
    private ChainOfCustodyEventDAO chainOfCustodyEventDAO;

    @Autowired
    private AliquotDAO aliquotDAO;

    @Override
    @Transactional(readOnly = true)
    public ChainOfCustodyEvent get(Integer id) {
        return chainOfCustodyEventDAO.get(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyEvent> getAll() {
        return chainOfCustodyEventDAO.getAll();
    }

    @Override
    public ChainOfCustodyEvent save(ChainOfCustodyEvent event) {
        Integer id = chainOfCustodyEventDAO.insert(event);
        event.setId(id);
        return event;
    }

    @Override
    public ChainOfCustodyEvent update(ChainOfCustodyEvent event) {
        chainOfCustodyEventDAO.update(event);
        return event;
    }

    @Override
    public void delete(Integer id) {
        ChainOfCustodyEvent event = get(id);
        if (event != null) {
            chainOfCustodyEventDAO.delete(event);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyEvent> findBySampleId(Integer sampleId) {
        return chainOfCustodyEventDAO.findBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyEvent> findByAliquotId(Integer aliquotId) {
        return chainOfCustodyEventDAO.findByAliquotId(aliquotId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyEvent> findByAction(ChainOfCustodyEvent.CustodyAction action) {
        return chainOfCustodyEventDAO.findByAction(action);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyEvent> findPendingApprovals() {
        return chainOfCustodyEventDAO.findPendingApprovals();
    }

    @Override
    public ChainOfCustodyEvent recordCustodyEvent(Integer sampleId, Integer aliquotId,
            ChainOfCustodyEvent.CustodyAction action, String comments, String userId) {

        ChainOfCustodyEvent event = new ChainOfCustodyEvent();
        event.setSampleId(sampleId);
        event.setAliquotId(aliquotId);
        event.setAction(action);
        event.setComments(comments);
        event.setEventTimestamp(new Timestamp(System.currentTimeMillis()));
        event.setPerformedBy(userId);
        event.setApprovalStatus(determineApprovalRequirement(action));
        event.setSysUserId(userId);

        Integer id = chainOfCustodyEventDAO.insert(event);
        event.setId(id);

        return event;
    }

    @Override
    public ChainOfCustodyEvent recordTransfer(Integer sampleId, Integer aliquotId,
            String fromLocation, String toLocation, String userId) {

        ChainOfCustodyEvent event = new ChainOfCustodyEvent();
        event.setSampleId(sampleId);
        event.setAliquotId(aliquotId);
        event.setAction(ChainOfCustodyEvent.CustodyAction.TRANSFERRED);
        event.setFromLocation(fromLocation);
        event.setToLocation(toLocation);
        event.setComments("Transferred from " + fromLocation + " to " + toLocation);
        event.setEventTimestamp(new Timestamp(System.currentTimeMillis()));
        event.setPerformedBy(userId);
        event.setApprovalStatus(ChainOfCustodyEvent.ApprovalStatus.PENDING);
        event.setSysUserId(userId);

        Integer id = chainOfCustodyEventDAO.insert(event);
        event.setId(id);

        if (aliquotId != null) {
            Aliquot aliquot = aliquotDAO.get(aliquotId).orElse(null);
            if (aliquot != null) {
                aliquot.setSysUserId(userId);
                aliquotDAO.update(aliquot);
            }
        }

        return event;
    }

    @Override
    public ChainOfCustodyEvent approveEvent(Integer eventId, String approverId) {
        ChainOfCustodyEvent event = get(eventId);
        if (event == null) {
            throw new LIMSRuntimeException("Chain of custody event not found: " + eventId);
        }

        if (event.getApprovalStatus() != ChainOfCustodyEvent.ApprovalStatus.PENDING) {
            throw new LIMSRuntimeException("Event is not pending approval");
        }

        event.setApprovalStatus(ChainOfCustodyEvent.ApprovalStatus.APPROVED);
        event.setApprovedBy(approverId);
        event.setApprovedAt(new Timestamp(System.currentTimeMillis()));
        event.setSysUserId(approverId);
        chainOfCustodyEventDAO.update(event);

        return event;
    }

    @Override
    public ChainOfCustodyEvent rejectEvent(Integer eventId, String rejectionReason, String approverId) {
        ChainOfCustodyEvent event = get(eventId);
        if (event == null) {
            throw new LIMSRuntimeException("Chain of custody event not found: " + eventId);
        }

        if (event.getApprovalStatus() != ChainOfCustodyEvent.ApprovalStatus.PENDING) {
            throw new LIMSRuntimeException("Event is not pending approval");
        }

        event.setApprovalStatus(ChainOfCustodyEvent.ApprovalStatus.REJECTED);
        event.setApprovedBy(approverId);
        event.setApprovedAt(new Timestamp(System.currentTimeMillis()));
        String existingComments = event.getComments();
        if (existingComments != null && !existingComments.isEmpty()) {
            event.setComments(existingComments + "\nRejection reason: " + rejectionReason);
        } else {
            event.setComments("Rejection reason: " + rejectionReason);
        }
        event.setSysUserId(approverId);
        chainOfCustodyEventDAO.update(event);

        return event;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyEvent> getFullCustodyChain(Integer sampleId) {
        List<ChainOfCustodyEvent> fullChain = new ArrayList<>();

        List<ChainOfCustodyEvent> sampleEvents = chainOfCustodyEventDAO.findBySampleId(sampleId);
        fullChain.addAll(sampleEvents);

        List<Aliquot> aliquots = aliquotDAO.findByParentSampleId(sampleId);
        for (Aliquot aliquot : aliquots) {
            List<ChainOfCustodyEvent> aliquotEvents = chainOfCustodyEventDAO.findByAliquotId(aliquot.getId());
            fullChain.addAll(aliquotEvents);
        }

        fullChain.sort((e1, e2) -> e1.getEventTimestamp().compareTo(e2.getEventTimestamp()));

        return fullChain;
    }

    private ChainOfCustodyEvent.ApprovalStatus determineApprovalRequirement(
            ChainOfCustodyEvent.CustodyAction action) {
        switch (action) {
            case TRANSFERRED:
            case DISPOSED:
            case SHIPPED:
                return ChainOfCustodyEvent.ApprovalStatus.PENDING;
            default:
                return ChainOfCustodyEvent.ApprovalStatus.NOT_REQUIRED;
        }
    }
}
