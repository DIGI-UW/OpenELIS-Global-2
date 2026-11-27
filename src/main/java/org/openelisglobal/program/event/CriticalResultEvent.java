package org.openelisglobal.program.event;

import java.time.OffsetDateTime;
import org.springframework.context.ApplicationEvent;

/**
 * A case was signed out with a critical result. It names the case and who
 * signed it out, never the diagnosis or the patient, so whatever listens can
 * only tell that a critical result was released.
 */
public class CriticalResultEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final String caseType;
    private final Integer caseId;
    private final String accessionNumber;
    private final String sysUserId;
    private final OffsetDateTime occurredAt;

    public CriticalResultEvent(Object source, String caseType, Integer caseId, String accessionNumber, String sysUserId,
            OffsetDateTime occurredAt) {
        super(source);
        this.caseType = caseType;
        this.caseId = caseId;
        this.accessionNumber = accessionNumber;
        this.sysUserId = sysUserId;
        this.occurredAt = occurredAt;
    }

    public String getCaseType() {
        return caseType;
    }

    public Integer getCaseId() {
        return caseId;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public String getSysUserId() {
        return sysUserId;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}
