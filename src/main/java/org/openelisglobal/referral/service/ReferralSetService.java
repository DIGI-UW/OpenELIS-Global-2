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

    /**
     * Env/vector "Refer Out / Subcontract" entry point (S-14 / OGC-624). Inserts
     * Referral + DRAFT ReferralSubcontract + initial null→DRAFT history row per
     * item, without pushing to the FHIR store — the FHIR push happens later, on the
     * DRAFT → DISPATCHED transition.
     *
     * <p>
     * Called synchronously from {@code SamplePatientEntryServiceImpl.persistData}
     * for env/vector workflows so referral persistence is atomic with the order
     * save (no silent async failure when the FHIR store is unreachable).
     */
    void createDraftReferralSetsForOrderEntry(List<ReferralItem> referralItems, SamplePatientUpdateData updateData);
}
