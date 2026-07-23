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
package org.openelisglobal.referencetables.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.referencetables.dao.ReferenceTablesDAO;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Yi Chen
 */
@Component
@Transactional
public class ReferenceTablesDAOImpl extends BaseDAOImpl<ReferenceTables, String> implements ReferenceTablesDAO {

    public ReferenceTablesDAOImpl() {
        super(ReferenceTables.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(ReferenceTables referenceTables) throws LIMSRuntimeException {
        try {
            ReferenceTables reftbl = entityManager.unwrap(Session.class).get(ReferenceTables.class,
                    referenceTables.getId());
            if (reftbl != null) {
                PropertyUtils.copyProperties(referenceTables, reftbl);
            } else {
                referenceTables.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Referencetables getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferenceTables> getAllReferenceTables() throws LIMSRuntimeException {
        List<ReferenceTables> list = new Vector<>();
        try {
            String sql = "from ReferenceTables";
            Query<ReferenceTables> query = entityManager.unwrap(Session.class).createQuery(sql, ReferenceTables.class);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Referencetables getAllReferenceTables()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferenceTables> getPageOfReferenceTables(int startingRecNo) throws LIMSRuntimeException {
        List<ReferenceTables> list = new Vector<>();
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from ReferenceTables r order by r.tableName";
            Query<ReferenceTables> query = entityManager.unwrap(Session.class).createQuery(sql, ReferenceTables.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);
            // query.setCacheMode(org.hibernate.CacheMode.REFRESH);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Referencetables getPageOfReferenceTables()", e);
        }

        return list;
    }

    public ReferenceTables readReferenceTables(String idString) {
        ReferenceTables referenceTables = null;
        try {
            referenceTables = entityManager.unwrap(Session.class).get(ReferenceTables.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Referencetables readReferenceTables(idString)", e);
        }

        return referenceTables;
    }

    // bugzilla 1411
    @Override
    @Transactional(readOnly = true)
    public Integer getTotalReferenceTablesCount() throws LIMSRuntimeException {
        return getCount();
    }

    // bugzilla 1482
    @Override
    public boolean duplicateReferenceTablesExists(ReferenceTables referenceTables, boolean isNew)
            throws LIMSRuntimeException {
        try {

            List<ReferenceTables> list = new ArrayList<>();
            String sql;

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            if (isNew) {
                sql = "from ReferenceTables t where trim(lower(t.tableName)) = :param";
            } else {
                sql = "from ReferenceTables t where trim(lower(t.tableName)) = :param and id != :param2";
            }

            // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "Yi in
            // duplicateReferencetables sql is " + sql);
            Query<ReferenceTables> query = entityManager.unwrap(Session.class).createQuery(sql, ReferenceTables.class);
            // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
            // "duplicateReferencetables sql is " + sql);

            query.setParameter("param", referenceTables.getTableName().toLowerCase().trim());

            // initialize with 0 (for new records where no id has been generated
            // yet
            String referenceTablesId = "0";
            if (!StringUtil.isNullorNill(referenceTables.getId())) {
                referenceTablesId = referenceTables.getId();
            }

            if (!isNew) {
                query.setParameter("param2", referenceTablesId);
            }
            list = query.list();
            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateReferenceTablesExists()", e);
        }
    }

    // bugzilla 2571 go through ReferenceTablesDAO to get reference tables info
    @Override
    @Transactional(readOnly = true)
    public List<ReferenceTables> getAllReferenceTablesForHl7Encoding() throws LIMSRuntimeException {
        List<ReferenceTables> list = new Vector<>();
        try {
            String sql = "from ReferenceTables rt where trim(upper(rt.isHl7Encoded)) = 'Y'";
            Query<ReferenceTables> query = entityManager.unwrap(Session.class).createQuery(sql, ReferenceTables.class);
            // query.setMaxResults(10);
            // query.setFirstResult(3);
            list = query.list();
        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReferenceTables getAllReferenceTablesForHl7Encoding()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public ReferenceTables getReferenceTableByName(ReferenceTables referenceTables) throws LIMSRuntimeException {
        return getReferenceTableByName(referenceTables.getTableName());
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalReferenceTableCount() throws LIMSRuntimeException {
        return getCount();
    }

    @Override
    @Transactional(readOnly = true)
    public ReferenceTables getReferenceTableByName(String tableName) {
        try {
            String sql = "from ReferenceTables rt where trim(lower(rt.tableName)) = :tableName";
            Query<ReferenceTables> query = entityManager.unwrap(Session.class).createQuery(sql, ReferenceTables.class);
            query.setParameter("tableName", tableName.toLowerCase().trim());

            ReferenceTables table = query.setMaxResults(1).uniqueResult();

            return table;

        } catch (HibernateException e) {
            handleException(e, "getReferenceTableByName");
        }

        return null;
    }
}
