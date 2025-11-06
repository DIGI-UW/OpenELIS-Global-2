/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */
package org.openelisglobal.referral.daoimpl;

import java.sql.Date;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.referral.dao.ReferringTestResultDAO;
import org.openelisglobal.referral.valueholder.ReferringTestResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 */
@Component
@Transactional
public class ReferringTestResultDAOImpl extends BaseDAOImpl<ReferringTestResult, String>
        implements ReferringTestResultDAO {

    public ReferringTestResultDAOImpl() {
        super(ReferringTestResult.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferringTestResult> getReferringTestResultsForSampleItem(String sampleItemId)
            throws LIMSRuntimeException {
        String sql = "from ReferringTestResult rtr where rtr.sampleItemId = :sampleItemId";
        try {
            Query<ReferringTestResult> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ReferringTestResult.class);
            query.setParameter("sampleItemId", Integer.parseInt(sampleItemId));
            List<ReferringTestResult> list = query.list();
            return list;
        } catch (HibernateException e) {
            handleException(e, "getReferringTestResultsForSampleItem");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferringTestResult> getResultsInDateRange(Date lowDate, Date highDate) throws LIMSRuntimeException {
        String sql = "from ReferringTestResult rtr where rtr.lastupdated BETWEEN :lowDate AND :highDate";
        try {
            Query<ReferringTestResult> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ReferringTestResult.class);
            query.setParameter("lowDate", lowDate);
            query.setParameter("highDate", highDate);

            List<ReferringTestResult> list = query.list();
            return list;
        } catch (HibernateException e) {
            handleException(e, "getResultsInDateRange");
        }

        return null;
    }
}
