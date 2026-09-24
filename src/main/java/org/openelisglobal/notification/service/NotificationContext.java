package org.openelisglobal.notification.service;

import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * Snapshot of the data needed to fire a single notification trigger. Carries
 * the entity that caused the event plus any pre-resolved ancillaries the
 * recipient resolver and template renderer will need.
 */
public class NotificationContext {

    private final String eventCode;
    private final Referral referral;
    private final Sample sample;
    private final Organization receivingLab;
    private final String submittingUserId;
    private final Map<String, String> variables;

    public NotificationContext(String eventCode, Referral referral, Sample sample, Organization receivingLab,
            String submittingUserId, Map<String, String> variables) {
        this.eventCode = eventCode;
        this.referral = referral;
        this.sample = sample;
        this.receivingLab = receivingLab;
        this.submittingUserId = submittingUserId;
        this.variables = variables == null ? new HashMap<>() : variables;
    }

    public String getEventCode() {
        return eventCode;
    }

    public Referral getReferral() {
        return referral;
    }

    public Sample getSample() {
        return sample;
    }

    public Organization getReceivingLab() {
        return receivingLab;
    }

    public String getSubmittingUserId() {
        return submittingUserId;
    }

    public Map<String, String> getVariables() {
        return variables;
    }
}
