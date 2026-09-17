package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationTargetType;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroCriticalCommunicationService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroCriticalCommunication logCommunication(String caseId, String recipient, String message, boolean followUpNeeded,
            String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroCriticalCommunication logCommunication(String caseId, MicroCriticalCommunicationTargetType targetType,
            String targetId, String recipient, String recipientContact, String communicationMethod, String message,
            boolean followUpNeeded, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroCriticalCommunication acknowledge(String communicationId, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroCriticalCommunication close(String communicationId, String resolutionNote, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    void synchronizeAcknowledgementFromAlert(String communicationId, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    void synchronizeResolutionFromAlert(String communicationId, String resolutionNote, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroCriticalCommunication> getByCaseId(String caseId);
}
