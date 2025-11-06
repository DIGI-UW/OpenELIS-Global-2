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
package org.openelisglobal.result.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.result.dao.ResultSignatureDAO;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ResultSignatureDAOImpl extends BaseDAOImpl<ResultSignature, String> implements ResultSignatureDAO {

    public ResultSignatureDAOImpl() {
        super(ResultSignature.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(ResultSignature resultSignature) throws LIMSRuntimeException {
        try {
            ResultSignature tmpResultSignature = entityManager.unwrap(Session.class).get(ResultSignature.class,
                    resultSignature.getId());
            if (tmpResultSignature != null) {
                PropertyUtils.copyProperties(resultSignature, tmpResultSignature);
            } else {
                resultSignature.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ResultSignature getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultSignature> getResultSignaturesByResult(Result result) throws LIMSRuntimeException {
        List<ResultSignature> resultSignatures = null;
        try {

            String sql = "from ResultSignature r where r.resultId = :resultId";
            Query<ResultSignature> query = entityManager.unwrap(Session.class).createQuery(sql, ResultSignature.class);
            query.setParameter("resultId", Integer.parseInt(result.getId()));

            resultSignatures = query.list();
            return resultSignatures;

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ResultSignature getResultSignatureResult()", e);
        }
    }

    public ResultSignature readResultSignature(String idString) {
        ResultSignature data = null;
        try {
            data = entityManager.unwrap(Session.class).get(ResultSignature.class, idString);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ResultSignature readResultSignature()", e);
        }

        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultSignature getResultSignatureById(ResultSignature resultSignature) throws LIMSRuntimeException {
        try {
            ResultSignature re = entityManager.unwrap(Session.class).get(ResultSignature.class,
                    resultSignature.getId());
            return re;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ResultSignature getResultSignatureById()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultSignature> getResultSignaturesByResults(List<Result> resultList) throws LIMSRuntimeException {
        if (resultList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> resultIds = new ArrayList<>();
        for (Result result : resultList) {
            resultIds.add(Integer.parseInt(result.getId()));
        }

        String sql = "From ResultSignature rs where rs.resultId in (:resultIdList)";

        try {
            Query<ResultSignature> query = entityManager.unwrap(Session.class).createQuery(sql, ResultSignature.class);
            query.setParameterList("resultIdList", resultIds);
            List<ResultSignature> sigs = query.list();
            return sigs;
        } catch (HibernateException e) {
            handleException(e, "getResultSignaturesByResults");
        }
        return null;
    }
}
