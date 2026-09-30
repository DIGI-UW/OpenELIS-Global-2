package org.openelisglobal.referral.service;

import java.util.List;
import java.util.Set;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.referral.valueholder.ReferralSet;
import org.openelisglobal.referral.valueholder.ReferralSubcontract;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.valueholder.Sample;

public interface ReferralSetService {

    void updateReferralSets(List<ReferralSet> referralSetList, List<Sample> modifiedSamples, Set<Sample> parentSamples,
            List<ReferralResult> removableReferralResults, String sysUserId);

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

    /**
     * Subcontract metadata row for a referral built from a Refer Out form. Every
     * new referral gets one: the lifecycle transitions read it, so a referral
     * without one can never advance past the state it was created in.
     */
    ReferralSubcontract buildSubcontractFromItem(ReferralItem referralItem, String currentUserId);

    /**
     * The null→DRAFT row that opens a referral's activity log. Call it right after
     * the referral is inserted, so the audit trail starts at inception rather than
     * at the first transition.
     */
    void insertInitialDraftHistory(String referralId, String actorUserId);
}
