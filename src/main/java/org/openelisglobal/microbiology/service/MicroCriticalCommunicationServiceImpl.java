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
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationStatus;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationTargetType;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code micro_critical_communication} is the clinical call/read-back record of
 * truth. This service additionally projects each communication into the generic
 * {@link Alert} entity so it surfaces in the existing Alerts Dashboard instead
 * of a parallel alerts experience. The projection is a surfacing view, not a
 * second system of record: the clinical log is authoritative, and the Alert row
 * is kept in step with it.
 */
@Service
public class MicroCriticalCommunicationServiceImpl implements MicroCriticalCommunicationService {

    private static final String ALERT_ENTITY_TYPE = "MicrobiologyCriticalCommunication";
    private static final Logger logger = LoggerFactory.getLogger(MicroCriticalCommunicationServiceImpl.class);

    private final MicroCriticalCommunicationDAO communicationDAO;
    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicroIsolateDAO isolateDAO;
    private final ResultService resultService;
    private final AlertService alertService;

    public MicroCriticalCommunicationServiceImpl(MicroCriticalCommunicationDAO communicationDAO, MicroCaseDAO caseDAO,
            MicroCaseActivityDAO activityDAO, MicroIsolateDAO isolateDAO, ResultService resultService,
            AlertService alertService) {
        this.communicationDAO = communicationDAO;
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.isolateDAO = isolateDAO;
        this.resultService = resultService;
        this.alertService = alertService;
    }

    @Override
    @Transactional
    public MicroCriticalCommunication logCommunication(String caseId, String recipient, String message,
            boolean followUpNeeded, String performedBy) {
        return logCommunication(caseId, MicroCriticalCommunicationTargetType.CASE, caseId, recipient, null, "PHONE",
                message, followUpNeeded, performedBy);
    }

    @Override
    @Transactional
    public MicroCriticalCommunication logCommunication(String caseId, MicroCriticalCommunicationTargetType targetType,
            String targetId, String recipient, String recipientContact, String communicationMethod, String message,
            boolean followUpNeeded, String performedBy) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        MicroCaseServiceImpl.requireText(recipient, "recipient");
        MicroCaseServiceImpl.requireText(communicationMethod, "communicationMethod");
        MicroCaseServiceImpl.requireText(message, "message");
        MicroCase microCase = caseDAO.get(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Microbiology case not found"));
        MicroCriticalCommunicationTargetType resolvedTargetType = targetType == null
                ? MicroCriticalCommunicationTargetType.CASE
                : targetType;
        String resolvedTargetId = targetId == null || targetId.trim().isEmpty() ? caseId : targetId;
        validateTarget(microCase, resolvedTargetType, resolvedTargetId);
        MicroCriticalCommunication communication = new MicroCriticalCommunication();
        communication.setCaseId(caseId);
        communication.setTargetType(resolvedTargetType.name());
        communication.setTargetId(resolvedTargetId);
        communication.setRecipient(recipient);
        communication.setRecipientContact(recipientContact);
        communication.setCommunicationMethod(communicationMethod.trim().toUpperCase());
        communication.setMessage(message);
        communication.setCommunicatedAt(MicroCaseServiceImpl.now());
        communication.setCommunicatedBy(performedBy);
        communication.setFollowUpNeeded(followUpNeeded);
        communication.setAcknowledgementStatus(MicroCriticalCommunicationStatus.OPEN.name());
        communicationDAO.insert(communication);
        recordActivity(caseId, MicroCaseActivityType.CRITICAL_COMMUNICATION_LOGGED, performedBy,
                "Critical communication logged", "{\"communicationId\":\"" + communication.getId() + "\"}");
        Alert alert = projectToAlertsDashboard(communication);
        if (alert != null && alert.getId() != null) {
            communication.setAlertId(alert.getId());
            communicationDAO.update(communication);
        }
        return communication;
    }

    @Override
    @Transactional
    public MicroCriticalCommunication acknowledge(String communicationId, String performedBy) {
        MicroCaseServiceImpl.requireText(communicationId, "communicationId");
        MicroCriticalCommunication communication = communicationDAO.get(communicationId)
                .orElseThrow(() -> new IllegalArgumentException("Critical communication not found"));
        if (MicroCriticalCommunicationStatus.CLOSED.name().equals(communication.getAcknowledgementStatus())) {
            throw new IllegalStateException("Closed critical communication cannot be acknowledged");
        }
        if (MicroCriticalCommunicationStatus.ACKNOWLEDGED.name().equals(communication.getAcknowledgementStatus())) {
            return communication;
        }
        acknowledgeRecord(communication, performedBy);
        acknowledgeProjectedAlert(communication);
        return communication;
    }

    @Override
    @Transactional
    public MicroCriticalCommunication close(String communicationId, String resolutionNote, String performedBy) {
        MicroCaseServiceImpl.requireText(communicationId, "communicationId");
        MicroCaseServiceImpl.requireText(resolutionNote, "resolutionNote");
        MicroCriticalCommunication communication = communicationDAO.get(communicationId)
                .orElseThrow(() -> new IllegalArgumentException("Critical communication not found"));
        if (MicroCriticalCommunicationStatus.OPEN.name().equals(communication.getAcknowledgementStatus())) {
            throw new IllegalStateException("Critical communication must be acknowledged before it can be closed");
        }
        if (MicroCriticalCommunicationStatus.CLOSED.name().equals(communication.getAcknowledgementStatus())) {
            return communication;
        }
        closeRecord(communication, resolutionNote, performedBy);
        resolveProjectedAlert(communication, resolutionNote, performedBy);
        return communication;
    }

    @Override
    @Transactional
    public void synchronizeAcknowledgementFromAlert(String communicationId, String performedBy) {
        MicroCriticalCommunication communication = communicationDAO.get(communicationId).orElse(null);
        if (communication == null
                || !MicroCriticalCommunicationStatus.OPEN.name().equals(communication.getAcknowledgementStatus())) {
            return;
        }
        acknowledgeRecord(communication, performedBy);
    }

    @Override
    @Transactional
    public void synchronizeResolutionFromAlert(String communicationId, String resolutionNote, String performedBy) {
        MicroCriticalCommunication communication = communicationDAO.get(communicationId).orElse(null);
        if (communication == null
                || MicroCriticalCommunicationStatus.CLOSED.name().equals(communication.getAcknowledgementStatus())) {
            return;
        }
        if (MicroCriticalCommunicationStatus.OPEN.name().equals(communication.getAcknowledgementStatus())) {
            acknowledgeRecord(communication, performedBy);
        }
        closeRecord(communication,
                resolutionNote == null || resolutionNote.trim().isEmpty() ? "Resolved through Alerts Dashboard"
                        : resolutionNote,
                performedBy);
    }

    private void acknowledgeRecord(MicroCriticalCommunication communication, String performedBy) {
        communication.setAcknowledgementStatus(MicroCriticalCommunicationStatus.ACKNOWLEDGED.name());
        communication.setAcknowledgedAt(MicroCaseServiceImpl.now());
        communication.setAcknowledgedBy(performedBy);
        communicationDAO.update(communication);
        recordActivity(communication.getCaseId(), MicroCaseActivityType.CRITICAL_COMMUNICATION_ACKNOWLEDGED,
                performedBy, "Critical communication acknowledged",
                "{\"communicationId\":\"" + communication.getId() + "\"}");
    }

    private void closeRecord(MicroCriticalCommunication communication, String resolutionNote, String performedBy) {
        communication.setAcknowledgementStatus(MicroCriticalCommunicationStatus.CLOSED.name());
        communication.setClosedAt(MicroCaseServiceImpl.now());
        communication.setClosedBy(performedBy);
        communication.setResolutionNote(resolutionNote);
        communicationDAO.update(communication);
        recordActivity(communication.getCaseId(), MicroCaseActivityType.CRITICAL_COMMUNICATION_CLOSED, performedBy,
                "Critical communication closed", "{\"communicationId\":\"" + communication.getId() + "\"}");
    }

    private Alert projectToAlertsDashboard(MicroCriticalCommunication communication) {
        return alertService.createAlert(AlertType.MICROBIOLOGY_CRITICAL, ALERT_ENTITY_TYPE, communication.getId(),
                AlertSeverity.CRITICAL, communication.getMessage(),
                "{\"caseId\":\"" + communication.getCaseId() + "\",\"communicationId\":\"" + communication.getId()
                        + "\",\"targetType\":\"" + communication.getTargetType() + "\",\"targetId\":\""
                        + communication.getTargetId() + "\"}");
    }

    private void acknowledgeProjectedAlert(MicroCriticalCommunication communication) {
        Integer userId;
        try {
            userId = Integer.valueOf(communication.getAcknowledgedBy());
        } catch (NumberFormatException e) {
            logger.warn(
                    "Could not sync Alerts Dashboard projection for critical communication {}: "
                            + "acknowledgedBy '{}' is not a numeric system user id",
                    communication.getId(), communication.getAcknowledgedBy());
            return;
        }
        for (Alert alert : projectedAlerts(communication)) {
            if (alert.getStatus() == AlertStatus.OPEN) {
                alertService.acknowledgeAlert(alert.getId(), userId);
            }
        }
    }

    private void resolveProjectedAlert(MicroCriticalCommunication communication, String resolutionNote,
            String performedBy) {
        Integer userId;
        try {
            userId = Integer.valueOf(performedBy);
        } catch (NumberFormatException e) {
            logger.warn("Could not resolve Alerts Dashboard projection for critical communication {}: user '{}' is not "
                    + "a numeric system user id", communication.getId(), performedBy);
            return;
        }
        for (Alert alert : projectedAlerts(communication)) {
            if (alert.getStatus() == AlertStatus.ACKNOWLEDGED) {
                alertService.resolveAlert(alert.getId(), userId, resolutionNote);
            }
        }
    }

    private List<Alert> projectedAlerts(MicroCriticalCommunication communication) {
        if (communication.getAlertId() != null) {
            Alert alert = alertService.get(communication.getAlertId());
            return alert == null ? List.of() : List.of(alert);
        }
        return alertService.getAlertsByEntityRef(ALERT_ENTITY_TYPE, communication.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCriticalCommunication> getByCaseId(String caseId) {
        return communicationDAO.getByCaseId(caseId);
    }

    private void validateTarget(org.openelisglobal.microbiology.valueholder.MicroCase microCase,
            MicroCriticalCommunicationTargetType targetType, String targetId) {
        switch (targetType) {
        case CASE:
            if (!microCase.getId().equals(targetId)) {
                throw new IllegalArgumentException("Critical case target must match the current microbiology case");
            }
            return;
        case ISOLATE:
            MicroIsolate isolate = isolateDAO.get(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
            if (!microCase.getId().equals(isolate.getCaseId())) {
                throw new IllegalArgumentException("Isolate target does not belong to the microbiology case");
            }
            return;
        case SAMPLE_ITEM:
            if (!microCase.getSampleItemId().equals(targetId)) {
                throw new IllegalArgumentException("Sample item target does not belong to the microbiology case");
            }
            return;
        case RESULT:
            Result result = resultService.getResultById(targetId);
            if (result == null || result.getAnalysis() == null || result.getAnalysis().getSampleItem() == null
                    || !microCase.getSampleItemId().equals(result.getAnalysis().getSampleItem().getId())) {
                throw new IllegalArgumentException("Result target does not belong to the microbiology case specimen");
            }
            return;
        default:
            throw new IllegalArgumentException("Unsupported critical communication target");
        }
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
