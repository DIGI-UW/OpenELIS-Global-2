package org.openelisglobal.referral.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.referral.action.beanitems.ReferralDisplayItem;
import org.openelisglobal.referral.form.ReferredOutTestsForm;
import org.openelisglobal.referral.form.ReferredOutTestsForm.ReferDateType;
import org.openelisglobal.referral.valueholder.Referral;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReferralService extends BaseObjectService<Referral, String> {

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
    Referral getReferralById(String referralId);

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
    Referral getReferralByAnalysisId(String analysisId);

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
    List<Referral> getReferralsBySampleId(String id);

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
    List<Referral> getUncanceledOpenReferrals();

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
    List<Referral> getSentReferrals();

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
    List<UUID> getSentReferralUuids();

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
    List<Referral> getReferralsByOrganization(String organizationId, Date lowDate, Date highDate);

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
    List<Referral> getReferralsByAccessionNumber(String labNumber);

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
    List<Referral> getReferralByPatientId(String selPatient);

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
    ReferralDisplayItem convertToDisplayItem(Referral referral);

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
    List<ReferralDisplayItem> getReferralItems(ReferredOutTestsForm form);

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_VIEW')")
    List<Referral> getReferralsByTestAndDate(ReferDateType dateType, Timestamp startTimestamp, Timestamp endTimestamp,
            List<String> testUnitIds, List<String> testIds);
}
