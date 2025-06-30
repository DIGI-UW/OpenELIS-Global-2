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
package org.openelisglobal.panel.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.panel.dao.PanelDAO;
import org.openelisglobal.panel.valueholder.Panel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class PanelDAOImpl extends BaseDAOImpl<Panel, String> implements PanelDAO {

    public PanelDAOImpl() {
        super(Panel.class);
    }

    private static Map<String, String> ID_NAME_MAP = null;
    private static Map<String, String> ID_DESCRIPTION_MAP = null;
    private static Map<String, String> NAME_ID_MAP = null;

    @Override
    @Transactional(readOnly = true)
    public void getData(Panel panel) throws LIMSRuntimeException {
        try {
            Panel pan = entityManager.unwrap(Session.class).get(Panel.class, panel.getId());
            if (pan != null) {
                PropertyUtils.copyProperties(panel, pan);
            } else {
                panel.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Panel getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Panel getPanelById(String panelId) throws LIMSRuntimeException {
        try {
            Panel panel = entityManager.unwrap(Session.class).get(Panel.class, panelId);
            return panel;
        } catch (HibernateException e) {
            handleException(e, "getDataById");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Panel> getAllActivePanels() throws LIMSRuntimeException {
        try {
            String sql = "from Panel p where p.isActive = 'Y' order by p.panelName";
            Query<Panel> query = entityManager.unwrap(Session.class).createQuery(sql, Panel.class);

            List<Panel> list = query.list();
            return list;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Panel getAllActivePanels()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Panel> getAllPanels() throws LIMSRuntimeException {
        try {
            String sql = "from Panel p order by p.sortOrderInt";
            Query<Panel> query = entityManager.unwrap(Session.class).createQuery(sql, Panel.class);

            List<Panel> list = query.list();
            return list;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Panel getAllPanels()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Panel> getPageOfPanels(int startingRecNo) throws LIMSRuntimeException {
        List<Panel> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            // bugzilla 1399
            String sql = "from Panel p order by p.panelName";
            Query<Panel> query = entityManager.unwrap(Session.class).createQuery(sql, Panel.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Panel getPageOfPanels()", e);
        }

        return list;
    }

    public Panel readPanel(String idString) {
        Panel panel = null;
        try {
            panel = entityManager.unwrap(Session.class).get(Panel.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Panel readPanel()", e);
        }

        return panel;
    }

    // this is for autocomplete
    @Override
    @Transactional(readOnly = true)
    public List<Panel> getActivePanels(String filter) throws LIMSRuntimeException {
        List<Panel> list = null;
        try {
            String sql = "from Panel p where isActive = 'Y' and upper(p.panelName) like upper(:param) order by"
                    + " upper(p.panelName)";
            Query<Panel> query = entityManager.unwrap(Session.class).createQuery(sql, Panel.class);
            query.setParameter("param", filter + "%");

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Panel getPanels()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public Panel getPanelByName(Panel panel) throws LIMSRuntimeException {
        return getPanelByName(panel.getPanelName());
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalPanelCount() throws LIMSRuntimeException {
        return getCount();
    }

    @Override
    public boolean duplicatePanelExists(Panel panel) throws LIMSRuntimeException {
        try {

            List<Panel> list = new ArrayList<>();

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from Panel t where trim(lower(t.panelName)) = :param and t.id != :panelId";
            Query<Panel> query = entityManager.unwrap(Session.class).createQuery(sql, Panel.class);
            query.setParameter("param", panel.getPanelName().toLowerCase().trim());

            // initialize with 0 (for new records where no id has been generated
            // yet
            String panelId = "0";
            if (!StringUtil.isNullorNill(panel.getId())) {
                panelId = panel.getId();
            }
            query.setParameter("panelId", Integer.parseInt(panelId));

            list = query.list();

            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicatePanelExists()", e);
        }
    }

    @Override
    public boolean duplicatePanelDescriptionExists(Panel panel) throws LIMSRuntimeException {
        try {

            List<Panel> list = new ArrayList<>();

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from Panel t where trim(lower(t.description)) = :param and t.id != :panelId";
            Query<Panel> query = entityManager.unwrap(Session.class).createQuery(sql, Panel.class);
            query.setParameter("param", panel.getDescription().toLowerCase().trim());

            // initialize with 0 (for new records where no id has been generated
            // yet
            String panelId = "0";
            if (!StringUtil.isNullorNill(panel.getId())) {
                panelId = panel.getId();
            }
            query.setParameter("panelId", Integer.parseInt(panelId));

            list = query.list();

            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicatePanelDescriptionExists()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getNameForPanelId(String id) {
        if (ID_NAME_MAP == null) {
            loadMaps();
        }

        return ID_NAME_MAP != null ? ID_NAME_MAP.get(id) : id;
    }

    @Override
    @Transactional(readOnly = true)
    public String getDescriptionForPanelId(String id) {
        if (ID_DESCRIPTION_MAP == null) {
            loadMaps();
        }

        return ID_DESCRIPTION_MAP != null ? ID_DESCRIPTION_MAP.get(id) : id;
    }

    @Override
    @Transactional(readOnly = true)
    public String getIdForPanelName(String name) {
        if (NAME_ID_MAP == null) {
            loadMaps();
        }

        return NAME_ID_MAP != null ? NAME_ID_MAP.get(name) : null;
    }

    private void loadMaps() {
        List<Panel> allPanels = getAllActivePanels();

        if (allPanels != null) {
            ID_NAME_MAP = new HashMap<>();
            ID_DESCRIPTION_MAP = new HashMap<>();
            NAME_ID_MAP = new HashMap<>();

            for (Object panelObj : allPanels) {
                Panel panel = (Panel) panelObj;
                ID_NAME_MAP.put(panel.getId(), panel.getPanelName());
                ID_DESCRIPTION_MAP.put(panel.getId(), panel.getDescription());
                NAME_ID_MAP.put(panel.getPanelName(), panel.getId());
            }
        }
    }

    @Override
    public void clearIDMaps() {
        ID_NAME_MAP = null;
        ID_DESCRIPTION_MAP = null;
    }

    @Override
    @Transactional(readOnly = true)
    public Panel getPanelByName(String panelName) {
        if (panelName == null) {
            panelName = "";
        }
        try {
            String sql = "from Panel p where p.panelName = :name";
            Query<Panel> query = entityManager.unwrap(Session.class).createQuery(sql, Panel.class);
            query.setParameter("name", panelName);

            List<Panel> panelList = query.list();
            return panelList.isEmpty() ? null : panelList.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Panel getPanelByName()", e);
        }
    }

    @Override
    public Panel getPanelByLoincCode(String loincCode) {
        if (loincCode == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "getPanelByLoincCode", "loincCode is null");
        }
        LogEvent.logDebug(this.getClass().getSimpleName(), "getPanelByLoincCode", "loincCode is: " + loincCode);

        String sql = "From Panel p where p.loinc = :loinc";
        try {
            Query<Panel> query = entityManager.unwrap(Session.class).createQuery(sql, Panel.class);
            query.setParameter("loinc", loincCode);
            return query.uniqueResult();
        } catch (HibernateException e) {
            handleException(e, "getPanelByLoincCode");
        }

        return null;
    }
}
