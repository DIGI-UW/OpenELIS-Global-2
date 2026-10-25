package org.openelisglobal.referral.service;

import java.util.List;
import org.openelisglobal.referral.dto.ReferenceLabMetricsDTO;
import org.openelisglobal.referral.dto.ReferenceLabReferralDTO;

public interface ReferenceLabResultsService {

    enum DashboardView {
        OUTSTANDING, RETURNED, HISTORY
    }

    List<ReferenceLabReferralDTO> getDashboardReferrals(DashboardView view);

    /** The view's referrals whose order the user may read by lab unit. */
    List<ReferenceLabReferralDTO> getDashboardReferrals(DashboardView view, String sysUserId);

    ReferenceLabMetricsDTO getDashboardMetrics();

    /** The metrics counted over the referrals the user may read by lab unit. */
    ReferenceLabMetricsDTO getDashboardMetrics(String sysUserId);

    /**
     * Whether the user may act on the referral, by the lab units of its order. An
     * unknown referral answers true, so the action itself reports it missing.
     */
    boolean isReferralVisible(String referralId, String sysUserId);

    /**
     * Accept a returned referral (OGC-803): live-read the results from the remote
     * FHIR store, post them to the originating Analysis, then mark the referral
     * reconciled. This is the reception gate — nothing reaches the patient record
     * until a user Accepts.
     */
    void acceptReferral(String referralId, String actorUserId);
}
