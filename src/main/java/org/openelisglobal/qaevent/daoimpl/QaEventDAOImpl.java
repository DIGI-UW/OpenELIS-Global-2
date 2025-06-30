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
 */
package org.openelisglobal.qaevent.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.qaevent.dao.QaEventDAO;
import org.openelisglobal.qaevent.valueholder.QaEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class QaEventDAOImpl extends BaseDAOImpl<QaEvent, String> implements QaEventDAO {

    public QaEventDAOImpl() {
        super(QaEvent.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(QaEvent qaEvent) throws LIMSRuntimeException {
        try {
            QaEvent qaEv = entityManager.unwrap(Session.class).get(QaEvent.class, qaEvent.getId());
            if (qaEv != null) {
                PropertyUtils.copyProperties(qaEvent, qaEv);

            } else {
                qaEvent.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in QaEvent getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QaEvent> getAllQaEvents() throws LIMSRuntimeException {
        List<QaEvent> list;
        try {
            String sql = "from QaEvent qe order by qe.id";
            Query<QaEvent> query = entityManager.unwrap(Session.class).createQuery(sql, QaEvent.class);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in QaEvent getAllQaEvents()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QaEvent> getPageOfQaEvents(int startingRecNo) throws LIMSRuntimeException {
        List<QaEvent> list = new Vector<>();
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from QaEvent qe order by qe.qaEventName";
            Query<QaEvent> query = entityManager.unwrap(Session.class).createQuery(sql, QaEvent.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in QaEvent getPageOfQaEvents()", e);
        }

        return list;
    }

    public QaEvent readQaEvent(String idString) {
        QaEvent qaEvent = null;
        try {
            qaEvent = entityManager.unwrap(Session.class).get(QaEvent.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in QaEvent readQaEvent()", e);
        }

        return qaEvent;
    }

    // this is for autocomplete
    @Override
    @Transactional(readOnly = true)
    public List<QaEvent> getQaEvents(String filter) throws LIMSRuntimeException {
        List<QaEvent> list = new Vector<>();
        try {
            String sql = "from QaEvent qe where upper(qe.qaEventName) like upper(:param) order by"
                    + " upper(qe.qaEventName)";
            Query<QaEvent> query = entityManager.unwrap(Session.class).createQuery(sql, QaEvent.class);
            query.setParameter("param", filter + "%");

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in QaEvent getQaEvents(String filter)", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public QaEvent getQaEventByName(QaEvent qaEvent) throws LIMSRuntimeException {
        try {
            String sql = "from QaEvent qe where qe.qaEventName = :param";
            Query<QaEvent> query = entityManager.unwrap(Session.class).createQuery(sql, QaEvent.class);
            query.setParameter("param", qaEvent.getQaEventName());

            List<QaEvent> list = query.list();
            QaEvent qe = null;
            if (list.size() > 0) {
                qe = list.get(0);
            }

            return qe;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in QaEvent getQaEventByName()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalQaEventCount() throws LIMSRuntimeException {
        return getCount();
    }

    @Override
    public boolean duplicateQaEventExists(QaEvent qaEvent) throws LIMSRuntimeException {
        try {

            List<QaEvent> list = new ArrayList<>();

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from QaEvent t where ((trim(lower(t.qaEventName)) = :param and trim(lower(t.type)) ="
                    + " :param3 and t.id != :param2) or (trim(lower(t.description)) = :param4 and"
                    + " trim(lower(t.type)) = :param3 and t.id != :param2)) ";

            Query<QaEvent> query = entityManager.unwrap(Session.class).createQuery(sql, QaEvent.class);
            query.setParameter("param", qaEvent.getQaEventName().toLowerCase().trim());
            query.setParameter("param3", qaEvent.getType());
            query.setParameter("param4", qaEvent.getDescription().toLowerCase().trim());

            // initialize with 0 (for new records where no id has been generated yet
            String qaEventId = "0";
            if (!StringUtil.isNullorNill(qaEvent.getId())) {
                qaEventId = qaEvent.getId();
            }
            query.setParameter("param2", qaEventId);

            list = query.list();

            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateQaEventExists()", e);
        }
    }
}
