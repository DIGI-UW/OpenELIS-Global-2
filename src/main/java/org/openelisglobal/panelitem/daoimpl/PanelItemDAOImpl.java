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
package org.openelisglobal.panelitem.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.panelitem.dao.PanelItemDAO;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class PanelItemDAOImpl extends BaseDAOImpl<PanelItem, String> implements PanelItemDAO {

    public PanelItemDAOImpl() {
        super(PanelItem.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(PanelItem panelItem) throws LIMSRuntimeException {
        try {
            PanelItem data = entityManager.unwrap(Session.class).get(PanelItem.class, panelItem.getId());
            if (data != null) {
                PropertyUtils.copyProperties(panelItem, data);
            } else {
                panelItem.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PanelItem getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelItem> getAllPanelItems() throws LIMSRuntimeException {
        List<PanelItem> list;
        try {
            String sql = "from PanelItem P order by P.panel.id ";
            Query<PanelItem> query = entityManager.unwrap(Session.class).createQuery(sql, PanelItem.class);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PanelItem getAllPanelItems()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelItem> getPageOfPanelItems(int startingRecNo) throws LIMSRuntimeException {
        List<PanelItem> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from PanelItem p order by p.panel.panelName, p.testName";
            Query<PanelItem> query = entityManager.unwrap(Session.class).createQuery(sql, PanelItem.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PanelItem getPageOfPanelItems()", e);
        }

        return list;
    }

    public PanelItem readPanelItem(String idString) {
        PanelItem pi;
        try {
            pi = entityManager.unwrap(Session.class).get(PanelItem.class, idString);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PanelItem readPanelItem()", e);
        }

        return pi;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelItem> getPanelItems(String filter) throws LIMSRuntimeException {
        List<PanelItem> list;
        try {
            String sql = "from PanelItem p where upper(p.methodName) like upper(:param) order by"
                    + " upper(p.methodName)";
            Query<PanelItem> query = entityManager.unwrap(Session.class).createQuery(sql, PanelItem.class);
            query.setParameter("param", filter + "%");
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PanelItem getPanelItems(String filter)", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelItem> getPanelItemsForPanel(String panelId) throws LIMSRuntimeException {
        List<PanelItem> list;
        try {
            String sql = "from PanelItem p where p.panel.id = :panelId";
            Query<PanelItem> query = entityManager.unwrap(Session.class).createQuery(sql, PanelItem.class);
            query.setParameter("panelId", Integer.parseInt(panelId));

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PanelItem getPanelItemsForPanel(String panelId)", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalPanelItemCount() throws LIMSRuntimeException {
        return getCount();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getDuplicateSortOrderForPanel(PanelItem panelItem) throws LIMSRuntimeException {
        try {
            List<PanelItem> list;

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from PanelItem t where trim(lower(t.panel.panelName)) = :param and t.sortOrder ="
                    + " :sortOrder and t.id != :panelItemId";
            Query<PanelItem> query = entityManager.unwrap(Session.class).createQuery(sql, PanelItem.class);

            query.setParameter("param", panelItem.getPanelName().toLowerCase().trim());
            query.setParameter("sortOrder", Integer.parseInt(panelItem.getSortOrder()));

            // initialize with 0 (for new records where no id has been generated
            // yet
            String panelItemId = "0";
            if (!StringUtil.isNullorNill(panelItem.getId())) {
                panelItemId = panelItem.getId();
            }

            query.setParameter("panelItemId", Integer.parseInt(panelItemId));

            list = query.list();

            return !list.isEmpty();

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getDuplicateSortOrderForPanel()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelItem> getPanelItemByTestId(String testId) throws LIMSRuntimeException {
        String sql = "From PanelItem pi where pi.test.id = :testId";

        try {
            Query<PanelItem> query = entityManager.unwrap(Session.class).createQuery(sql, PanelItem.class);
            query.setParameter("testId", Integer.parseInt(testId));
            List<PanelItem> panelItems = query.list();
            return panelItems;

        } catch (HibernateException e) {
            handleException(e, "getPanelItemByTestId");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelItem> getPanelItemsForPanelAndItemList(String panelId, List<Integer> testList)
            throws LIMSRuntimeException {
        String sql = "From PanelItem pi where pi.panel.id = :panelId and pi.test.id in (:testList)";
        try {
            Query<PanelItem> query = entityManager.unwrap(Session.class).createQuery(sql, PanelItem.class);
            query.setParameter("panelId", Integer.parseInt(panelId));
            query.setParameterList("testList", testList);
            List<PanelItem> items = query.list();
            return items;
        } catch (HibernateException e) {
            handleException(e, "getPanelItemsFromPanelAndItemList");
        }
        return null;
    }
}
