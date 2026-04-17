package org.openelisglobal.referral.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.referral.valueholder.ReferralType;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
public interface ReferralTypeService extends BaseObjectService<ReferralType, String> {
    ReferralType getReferralTypeByName(String name);
}
