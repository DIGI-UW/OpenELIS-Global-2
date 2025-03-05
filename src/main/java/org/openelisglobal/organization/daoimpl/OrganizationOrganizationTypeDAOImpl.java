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
package org.openelisglobal.organization.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.organization.dao.OrganizationOrganizationTypeDAO;
import org.openelisglobal.organization.valueholder.Organization;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// TODO move to service layer, or other DAOs
@Component
@Transactional
public class OrganizationOrganizationTypeDAOImpl implements OrganizationOrganizationTypeDAO {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional
    public void deleteAllLinksForOrganization(String id) throws LIMSRuntimeException {

        try {
            String sql = "delete from organization_organization_type where org_id = :id";
            NativeQuery query = entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("id", Integer.parseInt(id));
            query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationOrganizationType deleteAllLinksForOrganization()", e);
        }
    }

    @Override
    @Transactional
    public void linkOrganizationAndType(Organization org, String typeId) throws LIMSRuntimeException {

        try {
            String sql = "INSERT INTO organization_organization_type(org_id, org_type_id)VALUES (:org_id,"
                    + " :type_id);";
            NativeQuery query = entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("org_id", Integer.parseInt(org.getId()));
            query.setParameter("type_id", Integer.parseInt(typeId));
            query.executeUpdate();

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationOrganizationType linkOrganizationAndType()", e);
        }
    }

    @Override
    @Transactional
    public List<String> getOrganizationIdsForType(String typeId) throws LIMSRuntimeException {
        List<String> orgIdList = null;
        String sql = "select cast(org_id AS varchar) from organization_organization_type where org_type_id ="
                + " :orgTypeId";

        try {
            NativeQuery query = entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("orgTypeId", Integer.parseInt(typeId));
            orgIdList = query.list();

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationOrganizationType getOrganizationForType()", e);
        }
        return orgIdList;
    }

    @Override
    @Transactional
    public List<String> getTypeIdsForOrganizationId(String organizationId) throws LIMSRuntimeException {
        List<String> orgIdList = null;
        String sql = "select cast(org_type_id AS varchar) from organization_organization_type where org_id ="
                + " :orgId";

        try {
            NativeQuery query = entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("orgId", Integer.parseInt(organizationId));
            orgIdList = query.list();

        } catch (RuntimeException e) {
            handleException(e, "getTypeIdsForOrganizationId");
        }
        return orgIdList;
    }

    private void handleException(Exception e, String string) {
        LogEvent.logError(e);
    }
}
