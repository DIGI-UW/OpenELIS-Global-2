package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCriticalCommunicationDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCriticalCommunicationServiceImpl implements MicroCriticalCommunicationService {

    private final MicroCriticalCommunicationDAO communicationDAO;
    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;

    public MicroCriticalCommunicationServiceImpl(MicroCriticalCommunicationDAO communicationDAO, MicroCaseDAO caseDAO,
            MicroCaseActivityDAO activityDAO) {
        this.communicationDAO = communicationDAO;
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
    }

    @Override
    @Transactional
    public MicroCriticalCommunication logCommunication(String caseId, String recipient, String message,
            boolean followUpNeeded, String performedBy) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        MicroCaseServiceImpl.requireText(message, "message");
        caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Microbiology case not found"));
        MicroCriticalCommunication communication = new MicroCriticalCommunication();
        communication.setCaseId(caseId);
        communication.setRecipient(recipient);
        communication.setMessage(message);
        communication.setCommunicatedAt(MicroCaseServiceImpl.now());
        communication.setCommunicatedBy(performedBy);
        communication.setFollowUpNeeded(followUpNeeded);
        communication.setAcknowledgementStatus(MicroCriticalCommunicationStatus.OPEN.name());
        communicationDAO.insert(communication);
        recordActivity(caseId, MicroCaseActivityType.CRITICAL_COMMUNICATION_LOGGED, performedBy,
                "Critical communication logged", "{\"communicationId\":\"" + communication.getId() + "\"}");
        return communication;
    }

    @Override
    @Transactional
    public MicroCriticalCommunication acknowledge(String communicationId, String performedBy) {
        MicroCaseServiceImpl.requireText(communicationId, "communicationId");
        MicroCriticalCommunication communication = communicationDAO.get(communicationId)
                .orElseThrow(() -> new IllegalArgumentException("Critical communication not found"));
        communication.setAcknowledgementStatus(MicroCriticalCommunicationStatus.ACKNOWLEDGED.name());
        communication.setAcknowledgedAt(MicroCaseServiceImpl.now());
        communication.setAcknowledgedBy(performedBy);
        communicationDAO.update(communication);
        recordActivity(communication.getCaseId(), MicroCaseActivityType.CRITICAL_COMMUNICATION_ACKNOWLEDGED,
                performedBy, "Critical communication acknowledged",
                "{\"communicationId\":\"" + communication.getId() + "\"}");
        return communication;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCriticalCommunication> getByCaseId(String caseId) {
        return communicationDAO.getByCaseId(caseId);
    }

    private void recordActivity(String caseId, MicroCaseActivityType activityType, String performedBy, String note,
            String structuredData) {
        MicroCaseActivity activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType(activityType.name());
        activity.setOccurredAt(MicroCaseServiceImpl.now());
        activity.setPerformedBy(performedBy);
        activity.setNote(note);
        activity.setStructuredData(structuredData);
        activityDAO.insert(activity);
    }
}
