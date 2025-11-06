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

import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.referral.dao.ReferralReasonDAO;
import org.openelisglobal.referral.valueholder.ReferralReason;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 */
@Component
@Transactional
public class ReferralReasonDAOImpl extends BaseDAOImpl<ReferralReason, String> implements ReferralReasonDAO {
    public ReferralReasonDAOImpl() {
        super(ReferralReason.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralReason> getAllReferralReasons() throws LIMSRuntimeException {
        String sql = "from ReferralReason";

        try {
            Query<ReferralReason> query = entityManager.unwrap(Session.class).createQuery(sql, ReferralReason.class);
            List<ReferralReason> reasons = query.list();
            return reasons;
        } catch (HibernateException e) {
            handleException(e, "getAllReferralReasons");
        }

        return null;
    }
}
