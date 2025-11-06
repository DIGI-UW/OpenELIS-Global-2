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
package org.openelisglobal.scriptlet.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.scriptlet.dao.ScriptletDAO;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class ScriptletDAOImpl extends BaseDAOImpl<Scriptlet, String> implements ScriptletDAO {

    public ScriptletDAOImpl() {
        super(Scriptlet.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Scriptlet scriptlet) throws LIMSRuntimeException {
        try {
            Scriptlet sc = entityManager.unwrap(Session.class).get(Scriptlet.class, scriptlet.getId());
            if (sc != null) {
                PropertyUtils.copyProperties(scriptlet, sc);
            } else {
                scriptlet.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Scriptlet getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scriptlet> getAllScriptlets() throws LIMSRuntimeException {
        List<Scriptlet> list;
        try {
            String sql = "from Scriptlet";
            Query<Scriptlet> query = entityManager.unwrap(Session.class).createQuery(sql, Scriptlet.class);
            // query.setMaxResults(10);
            // query.setFirstResult(3);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Scriptlet getAllScriptlets()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scriptlet> getPageOfScriptlets(int startingRecNo) throws LIMSRuntimeException {
        List<Scriptlet> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            // bugzilla 1399
            String sql = "from Scriptlet s order by s.scriptletName";
            Query<Scriptlet> query = entityManager.unwrap(Session.class).createQuery(sql, Scriptlet.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Scriptlet getPageOfScriptlets()", e);
        }

        return list;
    }

    public Scriptlet readScriptlet(String idString) {
        Scriptlet scriptlet = null;
        try {
            scriptlet = entityManager.unwrap(Session.class).get(Scriptlet.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Scriptlet readScriptlet()", e);
        }

        return scriptlet;
    }

    // this is for autocomplete
    @Override
    @Transactional(readOnly = true)
    public List<Scriptlet> getScriptlets(String filter) throws LIMSRuntimeException {
        List<Scriptlet> list;
        try {
            String sql = "from Scriptlet s where upper(s.scriptletName) like upper(:param) order by"
                    + " upper(s.scriptletName)";
            Query<Scriptlet> query = entityManager.unwrap(Session.class).createQuery(sql, Scriptlet.class);
            query.setParameter("param", filter + "%");

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Scriptlet getScriptlets(String filter)", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public Scriptlet getScriptletByName(Scriptlet scriptlet) throws LIMSRuntimeException {
        try {
            String sql = "from Scriptlet s where s.scriptletName = :param";
            Query<Scriptlet> query = entityManager.unwrap(Session.class).createQuery(sql, Scriptlet.class);
            query.setParameter("param", scriptlet.getScriptletName());

            List<Scriptlet> list = query.list();
            Scriptlet s = null;
            if (list.size() > 0) {
                s = list.get(0);
            }

            return s;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Scriptlet getScriptletByName()", e);
        }
    }

    // bugzilla 1411
    @Override
    @Transactional(readOnly = true)
    public Integer getTotalScriptletCount() throws LIMSRuntimeException {
        return getCount();
    }

    @Override
    public boolean duplicateScriptletExists(Scriptlet scriptlet) throws LIMSRuntimeException {
        try {

            List<Scriptlet> list = new ArrayList<>();

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from Scriptlet t where trim(lower(t.scriptletName)) = :param and t.id != :param2";
            Query<Scriptlet> query = entityManager.unwrap(Session.class).createQuery(sql, Scriptlet.class);
            query.setParameter("param", scriptlet.getScriptletName().toLowerCase().trim());

            // initialize with 0 (for new records where no id has been generated
            // yet
            String scriptletId = "0";
            if (!StringUtil.isNullorNill(scriptlet.getId())) {
                scriptletId = scriptlet.getId();
            }
            query.setParameter("param2", scriptletId);

            list = query.list();

            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateScriptletExists()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Scriptlet getScriptletById(String scriptletId) throws LIMSRuntimeException {
        try {
            Scriptlet scriptlet = entityManager.unwrap(Session.class).get(Scriptlet.class, scriptletId);
            return scriptlet;
        } catch (RuntimeException e) {
            handleException(e, "getScriptletById");
        }

        return null;
    }
}
