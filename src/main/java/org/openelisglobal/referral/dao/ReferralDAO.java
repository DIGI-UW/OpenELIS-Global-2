/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.referral.dao;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.referral.form.ReferredOutTestsForm.ReferDateType;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;

public interface ReferralDAO extends BaseDAO<Referral, String> {

    // public boolean insertData(Referral referral) throws LIMSRuntimeException;

    public Referral getReferralById(String referralId) throws LIMSRuntimeException;

    public Referral getReferralByAnalysisId(String analysisId) throws LIMSRuntimeException;

    public List<Referral> getReferralsByStatus(List<ReferralStatus> status);

    // public void updateData(Referral referral) throws LIMSRuntimeException;

    public List<Referral> getAllReferralsBySampleId(String id) throws LIMSRuntimeException;

    /**
     * @param organizationId - the PK of an organization
     * @param lowDate        - referral request date low
     * @param highDate       - referral request date high
     * @return a list in the of referrals
     */
    public List<Referral> getAllReferralsByOrganization(String organizationId, Date lowDate, Date highDate);

    public List<Referral> getReferralsByAnalysisIds(List<String> analysisIds);

    public List<Referral> getReferralsByTestAndDate(ReferDateType dateType, Timestamp startTimestamp,
            Timestamp endTimestamp, List<String> testUnitIds, List<String> testIds);
}
