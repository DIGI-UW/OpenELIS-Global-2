package org.openelisglobal.referral.service;

import java.util.List;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReferralItemService {

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
    List<ReferralItem> getReferralItems();
}
