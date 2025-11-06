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
package org.openelisglobal.referral.daoimpl;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.referral.dao.ReferralResultDAO;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 */
@Component
@Transactional
public class ReferralResultDAOImpl extends BaseDAOImpl<ReferralResult, String> implements ReferralResultDAO {

    public ReferralResultDAOImpl() {
        super(ReferralResult.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ReferralResult getReferralResultById(String referralResultId) throws LIMSRuntimeException {
        if (!GenericValidator.isBlankOrNull(referralResultId)) {
            try {
                ReferralResult referralResult = entityManager.unwrap(Session.class).get(ReferralResult.class,
                        referralResultId);
                return referralResult;
            } catch (HibernateException e) {
                handleException(e, "getReferralResultById");
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralResult> getReferralResultsForReferral(String referralId) throws LIMSRuntimeException {
        if (!GenericValidator.isBlankOrNull(referralId)) {
            String sql = "from ReferralResult rr where rr.referralId = :referralId order by rr.id";

            try {
                Query<ReferralResult> query = entityManager.unwrap(Session.class).createQuery(sql,
                        ReferralResult.class);
                query.setParameter("referralId", Integer.parseInt(referralId));
                List<ReferralResult> resultList = query.list();
                return resultList;

            } catch (HibernateException e) {
                handleException(e, "getReferralResultsForReferral");
            }
        }

        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralResult> getReferralsByResultId(String resultId) throws LIMSRuntimeException {
        String sql = "From ReferralResult rr where rr.result.id= :resultId";

        try {
            Query<ReferralResult> query = entityManager.unwrap(Session.class).createQuery(sql, ReferralResult.class);
            query.setParameter("resultId", Integer.parseInt(resultId));
            List<ReferralResult> referralResults = query.list();
            return referralResults;
        } catch (HibernateException e) {
            handleException(e, "getReferralsByResultId");
        }

        return null;
    }
}
