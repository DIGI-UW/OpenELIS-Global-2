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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reportdefinition.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.reportdefinition.dao.ReportDefinitionDAO;
import org.openelisglobal.reportdefinition.valueholder.ReportDefinition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ReportDefinitionDAOImpl extends BaseDAOImpl<ReportDefinition, String> implements ReportDefinitionDAO {

    public ReportDefinitionDAOImpl() {
        super(ReportDefinition.class);
    }

    @Override
    public List<ReportDefinition> getAllActive() {
        try {
            String hql = "FROM ReportDefinition r WHERE r.isActive = :isActive ORDER BY r.name";
            Query<ReportDefinition> query = entityManager.unwrap(Session.class).createQuery(hql,
                    ReportDefinition.class);
            query.setParameter("isActive", Boolean.TRUE);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportDefinitionDAOImpl getAllActive()", e);
        }
    }

    @Override
    public List<ReportDefinition> getActiveDefinitions() {
        return getAllActive();
    }

    @Override
    public List<ReportDefinition> getByCategory(String category) {
        try {
            String hql = "FROM ReportDefinition r WHERE r.category = :category ORDER BY r.name";
            Query<ReportDefinition> query = entityManager.unwrap(Session.class).createQuery(hql,
                    ReportDefinition.class);
            query.setParameter("category", category);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportDefinitionDAOImpl getByCategory()", e);
        }
    }

    @Override
    public List<ReportDefinition> getDefinitionsByCategory(String category) {
        return getByCategory(category);
    }

    @Override
    public List<ReportDefinition> getByCreatedBy(String userId) {
        try {
            String hql = "FROM ReportDefinition r WHERE r.createdBy = :userId ORDER BY r.lastupdated DESC, r.name";
            Query<ReportDefinition> query = entityManager.unwrap(Session.class).createQuery(hql,
                    ReportDefinition.class);
            query.setParameter("userId", userId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ReportDefinitionDAOImpl getByCreatedBy()", e);
        }
    }
}
