package org.openelisglobal.referral.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.referral.valueholder.ReferralResult;

public interface ReferralResultService extends BaseObjectService<ReferralResult, String> {

    ReferralResult getReferralResultById(String referralResultId);

    List<ReferralResult> getReferralResultsForReferral(String referralId);

    List<ReferralResult> getReferralsByResultId(String resultId);
}
