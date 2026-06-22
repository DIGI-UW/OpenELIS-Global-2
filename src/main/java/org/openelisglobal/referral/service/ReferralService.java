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

public interface ReferralService extends BaseObjectService<Referral, String> {

    Referral getReferralById(String referralId);

    Referral getReferralByAnalysisId(String analysisId);

    List<Referral> getReferralsBySampleId(String id);

    List<Referral> getUncanceledOpenReferrals();

    List<Referral> getSentReferrals();

    List<UUID> getSentReferralUuids();

    List<Referral> getReferralsByOrganization(String organizationId, Date lowDate, Date highDate);

    List<Referral> getReferralsByAccessionNumber(String labNumber);

    List<Referral> getReferralByPatientId(String selPatient);

    ReferralDisplayItem convertToDisplayItem(Referral referral);

    List<ReferralDisplayItem> getReferralItems(ReferredOutTestsForm form);

    List<Referral> getReferralsByTestAndDate(ReferDateType dateType, Timestamp startTimestamp, Timestamp endTimestamp,
            List<String> testUnitIds, List<String> testIds);
}
