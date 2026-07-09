package org.openelisglobal.referral.service;

import java.util.List;
import org.openelisglobal.referral.dto.ReferenceLabMetricsDTO;
import org.openelisglobal.referral.dto.ReferenceLabReferralDTO;

public interface ReferenceLabResultsService {

    enum DashboardView {
        OUTSTANDING, RETURNED, HISTORY
    }

    List<ReferenceLabReferralDTO> getDashboardReferrals(DashboardView view);

    ReferenceLabMetricsDTO getDashboardMetrics();

    /**
     * Accept a returned referral (OGC-803): live-read the results from the remote
     * FHIR store, post them to the originating Analysis, then mark the referral
     * reconciled. This is the reception gate — nothing reaches the patient record
     * until a user Accepts.
     */
    void acceptReferral(String referralId, String actorUserId);
}
