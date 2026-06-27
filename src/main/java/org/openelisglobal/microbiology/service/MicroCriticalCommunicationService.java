package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;

public interface MicroCriticalCommunicationService {

    MicroCriticalCommunication logCommunication(String caseId, String recipient, String message, boolean followUpNeeded,
            String performedBy);

    MicroCriticalCommunication acknowledge(String communicationId, String performedBy);

    List<MicroCriticalCommunication> getByCaseId(String caseId);
}
