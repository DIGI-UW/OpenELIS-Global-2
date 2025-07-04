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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): ITECH, University of Washington, Seattle WA.
 */
package org.openelisglobal.sampleqaevent.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleqaevent.dao.SampleQaEventDAO;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * $Header$
 *
 * @author Diane Benz
 * @date created 06/12/2008
 * @version $Revision$ bugzilla 2510
 */
@Component
@Transactional
public class SampleQaEventDAOImpl extends BaseDAOImpl<SampleQaEvent, String> implements SampleQaEventDAO {

    public SampleQaEventDAOImpl() {
        super(SampleQaEvent.class);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleQaEvent getData(String sampleQaEventId) throws LIMSRuntimeException {
        try {
            SampleQaEvent data = entityManager.unwrap(Session.class).get(SampleQaEvent.class, sampleQaEventId);
            return data;
        } catch (RuntimeException e) {
            handleException(e, "getData");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SampleQaEvent sampleQaEvent) throws LIMSRuntimeException {
        try {
            SampleQaEvent data = entityManager.unwrap(Session.class).get(SampleQaEvent.class, sampleQaEvent.getId());
            if (data != null) {
                PropertyUtils.copyProperties(sampleQaEvent, data);
            } else {
                sampleQaEvent.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleQaEvent getData()", e);
        }
    }

    public SampleQaEvent readSampleQaEvent(String idString) {
        SampleQaEvent sp = null;
        try {
            sp = entityManager.unwrap(Session.class).get(SampleQaEvent.class, idString);
        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleQaEvent readSampleQaEvent()", e);
        }

        return sp;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleQaEvent> getSampleQaEventsBySample(SampleQaEvent sampleQaEvent) throws LIMSRuntimeException {
        List<SampleQaEvent> sampleQaEvents = new ArrayList<>();

        try {
            String sql = "from SampleQaEvent aqe where aqe.sample = :param";
            Query<SampleQaEvent> query = entityManager.unwrap(Session.class).createQuery(sql, SampleQaEvent.class);
            query.setParameter("param", sampleQaEvent.getSample().getId());

            sampleQaEvents = query.list();

            return sampleQaEvents;

        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleQaEventDAO getSampleQaEventsBySample(SampleQaEvent)", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleQaEvent> getSampleQaEventsBySample(Sample sample) throws LIMSRuntimeException {
        List<SampleQaEvent> sampleQaEvents;

        try {
            String sql = "from SampleQaEvent aqe where aqe.sample = :sampleId";
            Query<SampleQaEvent> query = entityManager.unwrap(Session.class).createQuery(sql, SampleQaEvent.class);
            query.setParameter("sampleId", Integer.parseInt(sample.getId()));

            sampleQaEvents = query.list();

            return sampleQaEvents;
        } catch (RuntimeException e) {
            handleException(e, "getSampleQaEventsBySample(Sample sample)");
        }

        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public SampleQaEvent getSampleQaEventBySampleAndQaEvent(SampleQaEvent sampleQaEvent) throws LIMSRuntimeException {
        SampleQaEvent analQaEvent = null;
        try {
            // Use an expression to read in the SampleQaEvent whose
            // sample and qaevent is given
            String sql = "from SampleQaEvent aqe where aqe.sample = :param and aqe.qaEvent = :param2";
            Query<SampleQaEvent> query = entityManager.unwrap(Session.class).createQuery(sql, SampleQaEvent.class);
            query.setParameter("param", Integer.parseInt(sampleQaEvent.getSample().getId()));
            query.setParameter("param2", Integer.parseInt(sampleQaEvent.getQaEvent().getId()));
            List<SampleQaEvent> list = query.list();
            if ((list != null) && !list.isEmpty()) {
                analQaEvent = list.get(0);
            }
        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Exception occurred in getSampleQaEventBySampleAndQaEvent", e);
        }
        return analQaEvent;
    }

    /**
     * @see org.openelisglobal.sampleqaevent.dao.SampleQaEventDAO#getSampleQaEventsByUpdatedDate(java.sql.Date,
     *      java.sql.Date)
     */
    @Override
    @Transactional(readOnly = true)
    public List<SampleQaEvent> getSampleQaEventsByUpdatedDate(Date lowDate, Date highDate) throws LIMSRuntimeException {
        List<SampleQaEvent> sampleQaEvents = null;

        try {
            String sql = "FROM SampleQaEvent sqe WHERE sqe.lastupdated >= :lowDate AND sqe.lastupdated <="
                    + " :highDate";
            Query<SampleQaEvent> query = entityManager.unwrap(Session.class).createQuery(sql, SampleQaEvent.class);
            query.setParameter("lowDate", lowDate);
            query.setParameter("highDate", highDate);

            sampleQaEvents = query.list();
        } catch (RuntimeException e) {
            handleException(e, "getSampleQaEventsByDate");
        }

        return sampleQaEvents;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleQaEvent> getAllUncompleatedEvents() throws LIMSRuntimeException {
        String sql = "From SampleQaEvent sqa where sqa.completedDate is null";

        try {
            Query<SampleQaEvent> query = entityManager.unwrap(Session.class).createQuery(sql, SampleQaEvent.class);
            List<SampleQaEvent> events = query.list();
            return events;
        } catch (HibernateException e) {
            handleException(e, "getAllUncompleatedEvents");
        }

        return null;
    }
}
