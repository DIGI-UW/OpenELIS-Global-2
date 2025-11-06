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

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.referral.dao.ReferralTypeDAO;
import org.openelisglobal.referral.valueholder.ReferralType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 */
@Component
@Transactional
public class ReferralTypeDAOImpl extends BaseDAOImpl<ReferralType, String> implements ReferralTypeDAO {

    public ReferralTypeDAOImpl() {
        super(ReferralType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ReferralType getReferralTypeByName(String name) throws LIMSRuntimeException {
        String sql = "From ReferralType rt where rt.name = :name";

        try {
            Query<ReferralType> query = entityManager.unwrap(Session.class).createQuery(sql, ReferralType.class);
            query.setParameter("name", name);
            ReferralType referralType = query.uniqueResult();
            return referralType;
        } catch (HibernateException e) {
            handleException(e, "getReferralTypeByName");
        }

        return null;
    }
}
