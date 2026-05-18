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
package org.openelisglobal.statusofsample.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.statusofsample.dao.StatusOfSampleDAO;
import org.openelisglobal.statusofsample.valueholder.StatusOfSample;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author bill mcgough
 */
@Component
@Transactional
public class StatusOfSampleDAOImpl extends BaseDAOImpl<StatusOfSample, String> implements StatusOfSampleDAO {

    public StatusOfSampleDAOImpl() {
        super(StatusOfSample.class);
    }

    // bugzilla 1942
    @Override
    @Transactional(readOnly = true)
    public StatusOfSample getDataByStatusTypeAndStatusCode(StatusOfSample statusofsample) throws LIMSRuntimeException {

        try {
            // AIS - bugzilla 1546 - Used Upper
            String sql = "from StatusOfSample ss where UPPER(ss.statusType) = UPPER(:param) and ss.code = :param2";
            Query<StatusOfSample> query = entityManager.unwrap(Session.class).createQuery(sql, StatusOfSample.class);
            query.setParameter("param", statusofsample.getStatusType());
            query.setParameter("param2", statusofsample.getCode());
            List<StatusOfSample> list = query.list();

            StatusOfSample statusOfSamp = null;

            if (list.size() > 0) {
                statusOfSamp = list.get(0);
            }

            return statusOfSamp;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in StatusOfSample getDataByStatusTypeAndStatusCode()", e);
        }
    }

    /**
     * getData()
     *
     * @param statusOfSample
     * @throws LIMSRuntimeException
     */
    @Override
    @Transactional(readOnly = true)
    public void getData(StatusOfSample statusOfSample) throws LIMSRuntimeException {

        try {
            StatusOfSample sos = entityManager.unwrap(Session.class).get(StatusOfSample.class, statusOfSample.getId());
            if (sos != null) {
                PropertyUtils.copyProperties(statusOfSample, sos);
            } else {
                statusOfSample.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in StatusOfSample getData()", e);
        }
    }

    /**
     * getAllStatusOfSamples()
     *
     * @return List
     * @throws LIMSRuntimeException
     */
    @Override
    @Transactional(readOnly = true)
    public List<StatusOfSample> getAllStatusOfSamples() throws LIMSRuntimeException {

        List<StatusOfSample> list;
        try {
            String sql = "from StatusOfSample sos order by sos.statusOfSampleName ";
            Query<StatusOfSample> query = entityManager.unwrap(Session.class).createQuery(sql, StatusOfSample.class);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in StatusOfSample getAllStatusOfSamples()", e);
        }

        return list;
    }

    /**
     * getPageOfStatusOfSamples()
     *
     * @param startingRecNo
     * @return List
     * @throws LIMSRuntimeException
     */
    @Override
    @Transactional(readOnly = true)
    public List<StatusOfSample> getPageOfStatusOfSamples(int startingRecNo) throws LIMSRuntimeException {

        List<StatusOfSample> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            // bugzilla 1399
            String sql = "from StatusOfSample s order by s.statusType, s.code";
            Query<StatusOfSample> query = entityManager.unwrap(Session.class).createQuery(sql, StatusOfSample.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in StatusOfSample getPageOfStatusOfSamples()", e);
        }

        return list;
    }

    /**
     * readStatusOfSample()
     *
     * @param idString
     * @return StatusOfSample
     */
    public StatusOfSample readStatusOfSample(String idString) {

        StatusOfSample sos = null;
        try {
            sos = entityManager.unwrap(Session.class).get(StatusOfSample.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in StatusOfSample readStatusOfSample()", e);
        }

        return sos;
    }

    // bugzilla 1761 removed getStatus() - no longer needed

    /**
     * getTotalStatusOfSampleCount()
     *
     * @return Integer - total count
     */
    @Override
    @Transactional(readOnly = true)
    public Integer getTotalStatusOfSampleCount() throws LIMSRuntimeException {
        return getCount();
    }

    // bugzilla 1482
    /**
     * duplicateStatusOfSampleExists() - checks for duplicate description & status
     * type
     *
     * @param statusOfSample
     * @return boolean
     */
    @Override
    public boolean duplicateStatusOfSampleExists(StatusOfSample statusOfSample) throws LIMSRuntimeException {
        try {

            List<StatusOfSample> list;

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from StatusOfSample t where trim(lower(t.code)) = :param and trim(lower(t.statusType)) ="
                    + " :param2 and t.id != :param3";
            Query<StatusOfSample> query = entityManager.unwrap(Session.class).createQuery(sql, StatusOfSample.class);
            query.setParameter("param", statusOfSample.getCode().toLowerCase().trim());
            query.setParameter("param2", statusOfSample.getStatusType().toLowerCase().trim());

            // initialize with 0 (for new records where no id has been generated
            // yet
            String statusOfSampleId = "0";
            if (!StringUtil.isNullorNill(statusOfSample.getId())) {
                statusOfSampleId = statusOfSample.getId();
            }
            query.setParameter("param3", statusOfSampleId);

            list = query.list();

            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateStatusOfSampleExists()", e);
        }
    }
} // end of class
