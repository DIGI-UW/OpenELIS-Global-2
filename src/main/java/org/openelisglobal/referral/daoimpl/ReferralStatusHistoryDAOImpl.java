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
import org.openelisglobal.referral.dao.ReferralStatusHistoryDAO;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ReferralStatusHistoryDAOImpl extends BaseDAOImpl<ReferralStatusHistory, String>
        implements ReferralStatusHistoryDAO {

    public ReferralStatusHistoryDAOImpl() {
        super(ReferralStatusHistory.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralStatusHistory> findByReferralIdOrderedByChangedAt(String referralId)
            throws LIMSRuntimeException {
        if (GenericValidator.isBlankOrNull(referralId)) {
            return new ArrayList<>();
        }
        String sql = "FROM ReferralStatusHistory h WHERE h.referralId = :referralId ORDER BY h.changedAt ASC, h.id ASC";
        try {
            Query<ReferralStatusHistory> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ReferralStatusHistory.class);
            query.setParameter("referralId", referralId);
            return query.list();
        } catch (HibernateException e) {
            handleException(e, "findByReferralIdOrderedByChangedAt");
        }
        return new ArrayList<>();
    }
}
