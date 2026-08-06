package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.dao.MicroCaseAmendmentDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateIdentificationEventDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.openelisglobal.microbiology.valueholder.MicroIdentificationEventType;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroIdentificationHistoryServiceImpl implements MicroIdentificationHistoryService {

    private final MicroIsolateIdentificationEventDAO identificationEventDAO;
    private final MicroCaseAmendmentDAO amendmentDAO;
    private final MicroIsolateDAO isolateDAO;

    public MicroIdentificationHistoryServiceImpl(MicroIsolateIdentificationEventDAO identificationEventDAO,
            MicroCaseAmendmentDAO amendmentDAO, MicroIsolateDAO isolateDAO) {
        this.identificationEventDAO = identificationEventDAO;
        this.amendmentDAO = amendmentDAO;
        this.isolateDAO = isolateDAO;
    }

    @Override
    @Transactional
    public MicroIsolateIdentificationEvent recordChange(MicroIsolate previous, MicroIsolate updated, String reason,
            String performedBy) {
        MicroCaseAmendment amendment = amendmentDAO.getOpenByCaseId(updated.getCaseId());
        if (amendment != null && !hasText(reason)) {
            throw new IllegalArgumentException("REIDENTIFICATION_REASON_REQUIRED");
        }

        MicroIsolateIdentificationEvent event = new MicroIsolateIdentificationEvent();
        event.setIsolateId(updated.getId());
        event.setAmendmentId(amendment == null ? null : amendment.getId());
        event.setEventType(hasIdentification(previous) ? MicroIdentificationEventType.REIDENTIFIED.name()
                : MicroIdentificationEventType.IDENTIFIED.name());
        event.setPreviousOrganismId(previous.getOrganismId());
        event.setPreviousOrganismText(previous.getPreliminaryOrganismText());
        event.setPreviousSignificance(previous.getSignificance());
        event.setPreviousIdentificationStatus(previous.getIdentificationStatus());
        event.setPreviousIdentificationMethod(previous.getIdentificationMethod());
        event.setPreviousIdentificationConfidence(previous.getIdentificationConfidence());
        event.setNewOrganismId(updated.getOrganismId());
        event.setNewOrganismText(updated.getPreliminaryOrganismText());
        event.setNewSignificance(updated.getSignificance());
        event.setNewIdentificationStatus(updated.getIdentificationStatus());
        event.setNewIdentificationMethod(updated.getIdentificationMethod());
        event.setNewIdentificationConfidence(updated.getIdentificationConfidence());
        event.setReason(hasText(reason) ? reason.trim() : "Identification updated");
        event.setChangedAt(MicroCaseServiceImpl.now());
        event.setChangedBy(performedBy);
        identificationEventDAO.insert(event);
        return event;
    }

    @Override
    @Transactional
    public void revertAmendment(String amendmentId, String reason, String performedBy) {
        List<MicroIsolateIdentificationEvent> events = identificationEventDAO.getByAmendmentId(amendmentId);
        for (int index = events.size() - 1; index >= 0; index--) {
            MicroIsolateIdentificationEvent source = events.get(index);
            if (MicroIdentificationEventType.AMENDMENT_REVERTED.name().equals(source.getEventType())) {
                continue;
            }
            MicroIsolate isolate = isolateDAO.get(source.getIsolateId())
                    .orElseThrow(() -> new IllegalStateException("AMENDMENT_ISOLATE_MISSING"));
            MicroIsolate current = snapshot(isolate);
            isolate.setOrganismId(source.getPreviousOrganismId());
            isolate.setPreliminaryOrganismText(source.getPreviousOrganismText());
            isolate.setSignificance(source.getPreviousSignificance());
            isolate.setIdentificationStatus(source.getPreviousIdentificationStatus());
            isolate.setIdentificationMethod(source.getPreviousIdentificationMethod());
            isolate.setIdentificationConfidence(source.getPreviousIdentificationConfidence());
            isolateDAO.update(isolate);
            appendRevertEvent(amendmentId, current, isolate, reason, performedBy);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroIsolateIdentificationEvent> getHistory(String isolateId) {
        MicroCaseServiceImpl.requireText(isolateId, "isolateId");
        return identificationEventDAO.getByIsolateId(isolateId);
    }

    private boolean hasIdentification(MicroIsolate isolate) {
        return hasText(isolate.getOrganismId()) || hasText(isolate.getPreliminaryOrganismText())
                || hasText(isolate.getIdentificationStatus());
    }

    private MicroIsolate snapshot(MicroIsolate isolate) {
        MicroIsolate snapshot = new MicroIsolate();
        snapshot.setId(isolate.getId());
        snapshot.setCaseId(isolate.getCaseId());
        snapshot.setOrganismId(isolate.getOrganismId());
        snapshot.setPreliminaryOrganismText(isolate.getPreliminaryOrganismText());
        snapshot.setSignificance(isolate.getSignificance());
        snapshot.setIdentificationStatus(isolate.getIdentificationStatus());
        snapshot.setIdentificationMethod(isolate.getIdentificationMethod());
        snapshot.setIdentificationConfidence(isolate.getIdentificationConfidence());
        return snapshot;
    }

    private void appendRevertEvent(String amendmentId, MicroIsolate previous, MicroIsolate updated, String reason,
            String performedBy) {
        MicroIsolateIdentificationEvent event = new MicroIsolateIdentificationEvent();
        event.setIsolateId(updated.getId());
        event.setAmendmentId(amendmentId);
        event.setEventType(MicroIdentificationEventType.AMENDMENT_REVERTED.name());
        event.setPreviousOrganismId(previous.getOrganismId());
        event.setPreviousOrganismText(previous.getPreliminaryOrganismText());
        event.setPreviousSignificance(previous.getSignificance());
        event.setPreviousIdentificationStatus(previous.getIdentificationStatus());
        event.setPreviousIdentificationMethod(previous.getIdentificationMethod());
        event.setPreviousIdentificationConfidence(previous.getIdentificationConfidence());
        event.setNewOrganismId(updated.getOrganismId());
        event.setNewOrganismText(updated.getPreliminaryOrganismText());
        event.setNewSignificance(updated.getSignificance());
        event.setNewIdentificationStatus(updated.getIdentificationStatus());
        event.setNewIdentificationMethod(updated.getIdentificationMethod());
        event.setNewIdentificationConfidence(updated.getIdentificationConfidence());
        event.setReason(reason);
        event.setChangedAt(MicroCaseServiceImpl.now());
        event.setChangedBy(performedBy);
        identificationEventDAO.insert(event);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
