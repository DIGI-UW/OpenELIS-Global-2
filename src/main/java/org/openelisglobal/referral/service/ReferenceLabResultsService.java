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
}
