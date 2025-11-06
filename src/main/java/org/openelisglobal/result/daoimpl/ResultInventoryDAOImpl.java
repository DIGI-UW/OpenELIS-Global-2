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
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.result.dao.ResultInventoryDAO;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultInventory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ResultInventoryDAOImpl extends BaseDAOImpl<ResultInventory, String> implements ResultInventoryDAO {

    public ResultInventoryDAOImpl() {
        super(ResultInventory.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultInventory> getAllResultInventorys() throws LIMSRuntimeException {
        List<ResultInventory> resultInventories;
        try {
            String sql = "from ResultInventory";
            Query<ResultInventory> query = entityManager.unwrap(Session.class).createQuery(sql, ResultInventory.class);

            resultInventories = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ResultInventory getAllResultInventorys()", e);
        }

        return resultInventories;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(ResultInventory resultInventory) throws LIMSRuntimeException {
        try {
            ResultInventory tmpResultInventory = entityManager.unwrap(Session.class).get(ResultInventory.class,
                    resultInventory.getId());
            if (tmpResultInventory != null) {
                PropertyUtils.copyProperties(resultInventory, tmpResultInventory);
            } else {
                resultInventory.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ResultInventory getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultInventory> getResultInventorysByResult(Result result) throws LIMSRuntimeException {
        List<ResultInventory> resultInventories = null;
        try {

            String sql = "from ResultInventory r where r.resultId = :resultId";
            Query<ResultInventory> query = entityManager.unwrap(Session.class).createQuery(sql, ResultInventory.class);
            query.setParameter("resultId", Integer.parseInt(result.getId()));

            resultInventories = query.list();
            return resultInventories;

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ResultInventory getResultInventoryByResult()", e);
        }
    }

    public ResultInventory readResultInventory(String idString) {
        ResultInventory data = null;
        try {
            data = entityManager.unwrap(Session.class).get(ResultInventory.class, idString);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ResultInventory readResultInventory()", e);
        }

        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultInventory getResultInventoryById(ResultInventory resultInventory) throws LIMSRuntimeException {
        try {
            ResultInventory re = entityManager.unwrap(Session.class).get(ResultInventory.class,
                    resultInventory.getId());
            return re;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ResultInventory getResultInventoryById()", e);
        }
    }
}
