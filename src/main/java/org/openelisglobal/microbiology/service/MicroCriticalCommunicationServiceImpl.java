package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCriticalCommunicationDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * M-11 (FR-018): {@code micro_critical_communication} is the clinical
 * call/read-back record of truth. This service additionally projects each
 * communication into the generic {@link Alert} entity so it surfaces in the
 * existing Alerts Dashboard instead of a parallel alerts experience. The
 * projection is a surfacing view, not a second system of record: the clinical
 * log is authoritative, and the Alert row is kept in step with it.
 */
@Service
public class MicroCriticalCommunicationServiceImpl implements MicroCriticalCommunicationService {

    private static final String ALERT_ENTITY_TYPE = "MicrobiologyCriticalCommunication";
    private static final Logger logger = LoggerFactory.getLogger(MicroCriticalCommunicationServiceImpl.class);

    private final MicroCriticalCommunicationDAO communicationDAO;
    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final AlertService alertService;

    public MicroCriticalCommunicationServiceImpl(MicroCriticalCommunicationDAO communicationDAO, MicroCaseDAO caseDAO,
            MicroCaseActivityDAO activityDAO, AlertService alertService) {
        this.communicationDAO = communicationDAO;
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.alertService = alertService;
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
        projectToAlertsDashboard(communication);
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
        acknowledgeProjectedAlert(communicationId, performedBy);
        return communication;
    }

    private void projectToAlertsDashboard(MicroCriticalCommunication communication) {
        alertService.createAlert(AlertType.MICROBIOLOGY_CRITICAL, ALERT_ENTITY_TYPE, communication.getId(),
                AlertSeverity.CRITICAL, communication.getMessage(), "{\"caseId\":\"" + communication.getCaseId()
                        + "\",\"communicationId\":\"" + communication.getId() + "\"}");
    }

    private void acknowledgeProjectedAlert(String communicationId, String performedBy) {
        Integer userId;
        try {
            userId = Integer.valueOf(performedBy);
        } catch (NumberFormatException e) {
            logger.warn("Could not sync Alerts Dashboard projection for critical communication {}: "
                    + "performedBy '{}' is not a numeric system user id", communicationId, performedBy);
            return;
        }
        for (Alert alert : alertService.getAlertsByEntityRef(ALERT_ENTITY_TYPE, communicationId)) {
            if (alert.getStatus() == AlertStatus.OPEN) {
                alertService.acknowledgeAlert(alert.getId(), userId);
            }
        }
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
