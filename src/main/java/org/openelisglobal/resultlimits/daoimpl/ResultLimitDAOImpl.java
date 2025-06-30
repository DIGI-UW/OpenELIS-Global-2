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
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.resultlimits.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.resultlimits.dao.ResultLimitDAO;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ResultLimitDAOImpl extends BaseDAOImpl<ResultLimit, String> implements ResultLimitDAO {

    public ResultLimitDAOImpl() {
        super(ResultLimit.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(ResultLimit resultLimit) throws LIMSRuntimeException {
        try {
            ResultLimit tmpLimit = entityManager.unwrap(Session.class).get(ResultLimit.class, resultLimit.getId());
            if (tmpLimit != null) {
                PropertyUtils.copyProperties(resultLimit, tmpLimit);
            } else {
                resultLimit.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ResultLimit getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultLimit> getAllResultLimits() throws LIMSRuntimeException {
        List<ResultLimit> list;
        try {
            String sql = "from ResultLimit";
            Query<ResultLimit> query = entityManager.unwrap(Session.class).createQuery(sql, ResultLimit.class);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ResultLimit getAllResultLimits()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultLimit> getPageOfResultLimits(int startingRecNo) throws LIMSRuntimeException {
        List<ResultLimit> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from ResultLimit t order by t.id";
            Query<ResultLimit> query = entityManager.unwrap(Session.class).createQuery(sql, ResultLimit.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ResultLimit getPageOfResultLimits()", e);
        }

        return list;
    }

    public ResultLimit readResultLimit(String idString) {
        ResultLimit recoveredLimit;
        try {
            recoveredLimit = entityManager.unwrap(Session.class).get(ResultLimit.class, idString);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ResultLimit readResultLimit()", e);
        }

        return recoveredLimit;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultLimit> getAllResultLimitsForTest(String testId) throws LIMSRuntimeException {

        if (GenericValidator.isBlankOrNull(testId)) {
            return new ArrayList<>();
        }

        try {
            String sql = "from ResultLimit rl where rl.testId = :test_id";
            Query<ResultLimit> query = entityManager.unwrap(Session.class).createQuery(sql, ResultLimit.class);
            query.setParameter("test_id", Integer.parseInt(testId));

            List<ResultLimit> list = query.list();
            return list;
        } catch (RuntimeException e) {
            handleException(e, "getAllResultLimitsForTest");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultLimit getResultLimitById(String resultLimitId) throws LIMSRuntimeException {
        try {
            ResultLimit resultLimit = entityManager.unwrap(Session.class).get(ResultLimit.class, resultLimitId);
            return resultLimit;
        } catch (RuntimeException e) {
            handleException(e, "getResultLimitById");
        }

        return null;
    }
}
