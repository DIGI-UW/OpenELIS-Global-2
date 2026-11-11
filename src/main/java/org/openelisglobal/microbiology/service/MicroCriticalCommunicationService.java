package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationTargetType;

public interface MicroCriticalCommunicationService {

    MicroCriticalCommunication logCommunication(String caseId, String recipient, String message, boolean followUpNeeded,
            String performedBy);

    MicroCriticalCommunication logCommunication(String caseId, MicroCriticalCommunicationTargetType targetType,
            String targetId, String recipient, String recipientContact, String communicationMethod, String message,
            boolean followUpNeeded, String performedBy);

    MicroCriticalCommunication acknowledge(String communicationId, String performedBy);

    MicroCriticalCommunication close(String communicationId, String resolutionNote, String performedBy);

    void synchronizeAcknowledgementFromAlert(String communicationId, String performedBy);

    void synchronizeResolutionFromAlert(String communicationId, String resolutionNote, String performedBy);

    List<MicroCriticalCommunication> getByCaseId(String caseId);
}
