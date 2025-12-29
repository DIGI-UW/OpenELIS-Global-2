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
package org.openelisglobal.organization.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.organization.dao.OrganizationSampleTypeDAO;
import org.openelisglobal.organization.valueholder.OrganizationSampleType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrganizationSampleTypeDAOImpl extends BaseDAOImpl<OrganizationSampleType, String>
        implements OrganizationSampleTypeDAO {

    public OrganizationSampleTypeDAOImpl() {
        super(OrganizationSampleType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationSampleType> getByOrganizationId(String organizationId) throws LIMSRuntimeException {
        String sql = "FROM OrganizationSampleType ost WHERE ost.organizationId = :orgId";
        try {
            Query<OrganizationSampleType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    OrganizationSampleType.class);
            query.setParameter("orgId", Integer.parseInt(organizationId));
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationSampleTypeDAO getByOrganizationId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationSampleType> getBySampleTypeId(String sampleTypeId) throws LIMSRuntimeException {
        String sql = "FROM OrganizationSampleType ost WHERE ost.sampleTypeId = :sampleTypeId";
        try {
            Query<OrganizationSampleType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    OrganizationSampleType.class);
            query.setParameter("sampleTypeId", Integer.parseInt(sampleTypeId));
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationSampleTypeDAO getBySampleTypeId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationSampleType> getActiveByOrganizationId(String organizationId) throws LIMSRuntimeException {
        String sql = "FROM OrganizationSampleType ost " + "WHERE ost.organizationId = :orgId "
                + "AND ost.isActive = true " + "ORDER BY ost.sortOrder";
        try {
            Query<OrganizationSampleType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    OrganizationSampleType.class);
            query.setParameter("orgId", Integer.parseInt(organizationId));
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationSampleTypeDAO getActiveByOrganizationId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationSampleType getByOrganizationAndSampleType(String organizationId, String sampleTypeId)
            throws LIMSRuntimeException {
        String sql = "FROM OrganizationSampleType ost " + "WHERE ost.organizationId = :orgId "
                + "AND ost.sampleTypeId = :sampleTypeId";
        try {
            Query<OrganizationSampleType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    OrganizationSampleType.class);
            query.setParameter("orgId", Integer.parseInt(organizationId));
            query.setParameter("sampleTypeId", Integer.parseInt(sampleTypeId));
            List<OrganizationSampleType> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationSampleTypeDAO getByOrganizationAndSampleType()", e);
        }
    }

    @Override
    @Transactional
    public void deleteAllForOrganization(String organizationId) throws LIMSRuntimeException {
        String sql = "DELETE FROM OrganizationSampleType ost WHERE ost.organizationId = :orgId";
        try {
            Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setParameter("orgId", Integer.parseInt(organizationId));
            query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationSampleTypeDAO deleteAllForOrganization()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSampleTypeIdsForOrganization(String organizationId) throws LIMSRuntimeException {
        String sql = "SELECT ost.sampleTypeId FROM OrganizationSampleType ost WHERE ost.organizationId = :orgId";
        try {
            Query<String> query = entityManager.unwrap(Session.class).createQuery(sql, String.class);
            query.setParameter("orgId", Integer.parseInt(organizationId));
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationSampleTypeDAO getSampleTypeIdsForOrganization()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getOrganizationIdsForSampleType(String sampleTypeId) throws LIMSRuntimeException {
        String sql = "SELECT ost.organizationId FROM OrganizationSampleType ost WHERE ost.sampleTypeId = :sampleTypeId";
        try {
            Query<String> query = entityManager.unwrap(Session.class).createQuery(sql, String.class);
            query.setParameter("sampleTypeId", Integer.parseInt(sampleTypeId));
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationSampleTypeDAO getOrganizationIdsForSampleType()", e);
        }
    }
}
