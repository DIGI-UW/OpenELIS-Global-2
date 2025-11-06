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
package org.openelisglobal.requester.daoimpl;

import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.requester.dao.SampleRequesterDAO;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 */
@Component
@Transactional
public class SampleRequesterDAOImpl extends BaseDAOImpl<SampleRequester, String> implements SampleRequesterDAO {

    public SampleRequesterDAOImpl() {
        super(SampleRequester.class);
    }

    @Override
    public void delete(SampleRequester sampleRequester) throws LIMSRuntimeException {
        entityManager.unwrap(Session.class).delete(sampleRequester);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRequester> getRequestersForSampleId(String sampleId) throws LIMSRuntimeException {
        String sql = "From SampleRequester sr where sr.sampleId = :sampleId";

        try {
            Query<SampleRequester> query = entityManager.unwrap(Session.class).createQuery(sql, SampleRequester.class);
            query.setParameter("sampleId", Long.parseLong(sampleId));
            List<SampleRequester> requester = query.list();

            return requester;

        } catch (HibernateException e) {
            handleException(e, "getRequesterForSampleId");
        }
        return null;
    }

    @Override
    public List<SampleRequester> getRequestersForRequesterId(String requesterId, String requesterTypeId) {
        String hql = "From SampleRequester sr where sr.requester_id = :requesterId and sr.requester_type_id ="
                + " :requesterTypeId";

        try {
            Query<SampleRequester> query = entityManager.unwrap(Session.class).createQuery(hql, SampleRequester.class);
            query.setParameter("requesterId", Long.parseLong(requesterId));
            query.setParameter("requesterTypeId", Long.parseLong(requesterTypeId));
            List<SampleRequester> requester = query.list();

            return requester;

        } catch (HibernateException e) {
            handleException(e, "getRequesterForSampleId");
        }
        return null;
    }

    public SampleRequester readOld(long sampleId, long requesterTypeId) {
        String sql = "From SampleRequester sr where sr.sampleId = :sampleId and sr.requesterTypeId ="
                + " :requesterTypeId";
        try {
            Query<SampleRequester> query = entityManager.unwrap(Session.class).createQuery(sql, SampleRequester.class);
            query.setParameter("sampleId", sampleId);
            query.setParameter("requesterTypeId", requesterTypeId);
            SampleRequester requester = query.uniqueResult();

            return requester;
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleRequester readOld()", e);
        }
    }
}
