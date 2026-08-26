package org.openelisglobal.referral.service;

import java.util.List;
import org.openelisglobal.referral.dto.ReferenceLabMetricsDTO;
import org.openelisglobal.referral.dto.ReferenceLabReferralDTO;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReferenceLabResultsService {

    enum DashboardView {
        OUTSTANDING, RETURNED, HISTORY
    }

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
    List<ReferenceLabReferralDTO> getDashboardReferrals(DashboardView view);

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
    ReferenceLabMetricsDTO getDashboardMetrics();

    /**
     * Accept a returned referral (OGC-803): live-read the results from the remote
     * FHIR store, post them to the originating Analysis, then mark the referral
     * reconciled. This is the reception gate — nothing reaches the patient record
     * until a user Accepts.
     */
    @PreAuthorize("hasAuthority('PRIV_REFERRAL_MANAGE')")
    void acceptReferral(String referralId, String actorUserId);
}
