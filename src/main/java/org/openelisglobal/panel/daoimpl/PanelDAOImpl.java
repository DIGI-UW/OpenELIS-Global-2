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

import jakarta.persistence.OptimisticLockException;
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
            if (StringUtil.isNullorNill(panel.getPanelName())) {
                return false;
            }

            List<Panel> list = new ArrayList<>();

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from Panel t where trim(lower(t.panelName)) = :param and t.id != :panelId";
            Query<Panel> query = entityManager.unwrap(Session.class).createQuery(sql, Panel.class);
            // Use MANUAL flush mode to prevent auto-flushing pending changes in the
            // session.
            // This ensures the duplicate check queries only committed data in the database,
            // not uncommitted entities in the current session (e.g., the entity being
            // validated).
            // Without this, Hibernate would auto-flush and include the entity being
            // checked,
            // causing false positives in duplicate detection.
            query.setHibernateFlushMode(org.hibernate.FlushMode.MANUAL);
            query.setParameter("param", panel.getPanelName().toLowerCase().trim());

            // initialize with 0 (for new records where no id has been generated
            // yet
            String panelId = "0";
            if (!StringUtil.isNullorNill(panel.getId())) {
                panelId = panel.getId();
            }
            query.setParameter("panelId", panelId);

            list = query.list();

            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (HibernateException e) {
            handleException(e, "duplicatePanelExists");
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicatePanelExists() - invalid parameter", e);
        }

        return false;
    }

    @Override
    public boolean duplicatePanelDescriptionExists(Panel panel) throws LIMSRuntimeException {
        try {
            if (StringUtil.isNullorNill(panel.getDescription())) {
                return false;
            }

            List<Panel> list = new ArrayList<>();

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from Panel t where trim(lower(t.description)) = :param and t.id != :panelId";
            Query<Panel> query = entityManager.unwrap(Session.class).createQuery(sql, Panel.class);
            // Use MANUAL flush mode to prevent auto-flushing pending changes in the
            // session.
            // This ensures the duplicate check queries only committed data in the database,
            // not uncommitted entities in the current session (e.g., the entity being
            // validated).
            // Without this, Hibernate would auto-flush and include the entity being
            // checked,
            // causing false positives in duplicate detection.
            query.setHibernateFlushMode(org.hibernate.FlushMode.MANUAL);
            query.setParameter("param", panel.getDescription().toLowerCase().trim());

            // initialize with 0 (for new records where no id has been generated
            // yet
            String panelId = "0";
            if (!StringUtil.isNullorNill(panel.getId())) {
                panelId = panel.getId();
            }
            query.setParameter("panelId", panelId);

            list = query.list();

            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (HibernateException e) {
            handleException(e, "duplicatePanelDescriptionExists");
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicatePanelDescriptionExists() - invalid parameter", e);
        }

        return false;
    }

    @Override
    public boolean duplicatePanelCodeExists(Panel panel) throws LIMSRuntimeException {
        try {

            if (StringUtil.isNullorNill(panel.getCode())) {
                return false;
            }

            List<Panel> list = new ArrayList<>();

            String sql = "from Panel t where t.code = :param and t.id != :panelId";
            Query<Panel> query = entityManager.unwrap(Session.class).createQuery(sql, Panel.class);
            // Use MANUAL flush mode to prevent auto-flushing pending changes in the
            // session.
            // This ensures the duplicate check queries only committed data in the database,
            // not uncommitted entities in the current session (e.g., the entity being
            // validated).
            // Without this, Hibernate would auto-flush and include the entity being
            // checked,
            // causing false positives in duplicate detection.
            query.setHibernateFlushMode(org.hibernate.FlushMode.MANUAL);
            query.setParameter("param", panel.getCode());

            String panelId = "0";
            if (!StringUtil.isNullorNill(panel.getId())) {
                panelId = panel.getId();
            }
            query.setParameter("panelId", Integer.parseInt(panelId));

            list = query.list();

            return !list.isEmpty();

        } catch (HibernateException e) {
            handleException(e, "duplicatePanelCodeExists");
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicatePanelCodeExists() - invalid parameter", e);
        }

        return false;
    }

    @Override
    public void updatePanelFields(Panel panel) throws LIMSRuntimeException {
        try {
            Session session = entityManager.unwrap(Session.class);

            String sql = "UPDATE panel SET name = :name, code = :code, description = :description, loinc = :loinc, is_active = :active, lastupdated = :now WHERE id = :id";

            int updated = session.createNativeQuery(sql).setParameter("name", panel.getPanelName())
                    .setParameter("code", panel.getCode()).setParameter("description", panel.getDescription())
                    .setParameter("loinc", panel.getLoinc()).setParameter("active", panel.getIsActive())
                    .setParameter("now", new java.sql.Timestamp(System.currentTimeMillis()))
                    .setParameter("id", Integer.parseInt(panel.getId())).executeUpdate();

            if (updated != 1) {
                throw new OptimisticLockException("Panel was modified concurrently");
            }

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in updatePanelFields()", e);
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
    public String insert(Panel panel) {
        try {
            entityManager.createNativeQuery(
                    "SELECT setval('clinlims.panel_seq', (SELECT CAST(COALESCE(MAX(id),0) AS bigint) FROM clinlims.panel) + 1)")
                    .getSingleResult();
        } catch (Exception e) {

            LogEvent.logWarn(this.getClass().getSimpleName(), "insert", "Failed to sync panel_seq: " + e.getMessage());
        }
        return super.insert(panel);
    }

    @Override
    public void ensureSequence() {
        try {

            Number maxId = (Number) entityManager.createNativeQuery("select coalesce(max(id),0) from clinlims.panel")
                    .getSingleResult();

            String sql = "select setval('clinlims.panel_seq', CAST(" + maxId.longValue() + " AS bigint), true)";
            LogEvent.logDebug(this.getClass().getSimpleName(), "ensureSequence", "Executing SQL: " + sql);
            entityManager.createNativeQuery(sql).getSingleResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ensureSequence()", e);
        }
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

    @Override
    @Transactional(readOnly = true)
    public List<Panel> getPanelsByLabUnitId(String labUnitId, Boolean active) throws LIMSRuntimeException {
        try {
            // Build HQL query with subquery to filter by lab unit at database level
            StringBuilder sql = new StringBuilder(
                    "FROM Panel p WHERE p.id IN (SELECT plu.panelId FROM PanelLabUnit plu WHERE plu.labUnitId = :labUnitId)");

            // Add active status filter if specified
            if (active != null) {
                if (Boolean.TRUE.equals(active)) {
                    sql.append(" AND p.isActive = 'Y'");
                } else {
                    sql.append(" AND p.isActive != 'Y'");
                }
            }

            sql.append(" ORDER BY p.panelName");

            Query<Panel> query = entityManager.unwrap(Session.class).createQuery(sql.toString(), Panel.class);
            query.setParameter("labUnitId", labUnitId);

            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Panel getPanelsByLabUnitId()", e);
        }
    }
}
