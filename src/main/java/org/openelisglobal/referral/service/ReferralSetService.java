package org.openelisglobal.referral.service;

import java.util.List;
import java.util.Set;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.referral.valueholder.ReferralSet;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReferralSetService {

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_MANAGE')")
    void updateReferralSets(List<ReferralSet> referralSetList, List<Sample> modifiedSamples, Set<Sample> parentSamples,
            List<ReferralResult> removableReferralResults, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_MANAGE')")
    void createSaveReferralSetsSamplePatientEntry(List<ReferralItem> referralItems, SamplePatientUpdateData updateData);
}
