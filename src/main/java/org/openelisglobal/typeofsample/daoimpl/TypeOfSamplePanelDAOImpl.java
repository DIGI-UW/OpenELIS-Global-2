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
package org.openelisglobal.typeofsample.daoimpl;

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
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.typeofsample.dao.TypeOfSamplePanelDAO;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TypeOfSamplePanelDAOImpl extends BaseDAOImpl<TypeOfSamplePanel, String> implements TypeOfSamplePanelDAO {

    public TypeOfSamplePanelDAOImpl() {
        super(TypeOfSamplePanel.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(TypeOfSamplePanel typeOfSamplePanel) throws LIMSRuntimeException {

        try {
            TypeOfSamplePanel tos = entityManager.unwrap(Session.class).get(TypeOfSamplePanel.class,
                    typeOfSamplePanel.getId());
            if (tos != null) {
                PropertyUtils.copyProperties(typeOfSamplePanel, tos);
            } else {
                typeOfSamplePanel.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TypeOfSamplePanel getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSamplePanel> getAllTypeOfSamplePanels() throws LIMSRuntimeException {

        List<TypeOfSamplePanel> list;
        try {
            String sql = "from TypeOfSamplePanel";
            Query<TypeOfSamplePanel> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TypeOfSamplePanel.class);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TypeOfSamplePanel getAllTypeOfSamplePanels()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSamplePanel> getPageOfTypeOfSamplePanel(int startingRecNo) throws LIMSRuntimeException {

        List<TypeOfSamplePanel> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                    + 1;

            String sql = "from TypeOfSamplePanel t order by t.typeOfSampleId, t.panelId";
            Query<TypeOfSamplePanel> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TypeOfSamplePanel.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TypeOfSamplePanel getPageOfTypeOfSamples()", e);
        }

        return list;
    }

    public TypeOfSamplePanel readTypeOfSamplePanel(String idString) {
        TypeOfSamplePanel tos = null;
        try {
            tos = entityManager.unwrap(Session.class).get(TypeOfSamplePanel.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TypeOfSamplePanel readTypeOfSample()", e);
        }

        return tos;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalTypeOfSamplePanelCount() throws LIMSRuntimeException {
        return getCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSamplePanel> getTypeOfSamplePanelsForSampleType(String sampleType) {
        List<TypeOfSamplePanel> list;

        String sql = "from TypeOfSamplePanel tp where tp.typeOfSampleId = :sampleId order by tp.panelId";

        try {
            if (sampleType.equals("null")) {
                // so parseInt doesn't throw
                sampleType = "0";
            }
            Query<TypeOfSamplePanel> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TypeOfSamplePanel.class);
            query.setParameter("sampleId", Integer.parseInt(sampleType));
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TypeOfSamplePanelDAOImpl getTypeOfSamplePanelsForSampleType", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSamplePanel> getTypeOfSamplePanelsForPanel(String panelId) throws LIMSRuntimeException {
        List<TypeOfSamplePanel> list = new ArrayList<>();
        String sql = "from TypeOfSamplePanel tosp where tosp.panelId = :panelId";

        try {
            Query<TypeOfSamplePanel> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TypeOfSamplePanel.class);
            query.setParameter("panelId", Integer.parseInt(panelId));
            List<TypeOfSamplePanel> typeOfSamplePanels = query.list();
            return typeOfSamplePanels;
        } catch (HibernateException e) {
            handleException(e, "getTypeOfSamplePanelsForPanel");
        }

        return list;
    }
}
