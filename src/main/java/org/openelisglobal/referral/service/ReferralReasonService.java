package org.openelisglobal.referral.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.referral.valueholder.ReferralReason;

public interface ReferralReasonService extends BaseObjectService<ReferralReason, String> {
    List<ReferralReason> getAllReferralReasons();
}
