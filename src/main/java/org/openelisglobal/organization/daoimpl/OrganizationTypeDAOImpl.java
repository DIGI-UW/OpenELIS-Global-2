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

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.organization.dao.OrganizationTypeDAO;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrganizationTypeDAOImpl extends BaseDAOImpl<OrganizationType, String> implements OrganizationTypeDAO {

    public OrganizationTypeDAOImpl() {
        super(OrganizationType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationType> getAllOrganizationTypes() throws LIMSRuntimeException {
        List<OrganizationType> list = null;
        try {
            String sql = "from OrganizationType";
            Query<OrganizationType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    OrganizationType.class);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Organization getAllOrganizationTypes()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationType getOrganizationTypeByName(String name) throws LIMSRuntimeException {
        String sql = null;
        try {
            sql = "from OrganizationType o where o.name = :name";
            Query<OrganizationType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    OrganizationType.class);

            query.setParameter("name", name);

            List<OrganizationType> list = query.list();
            return list.size() > 0 ? list.get(0) : null;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrganizationType getOrganizationTypeByName()", e);
        }
    }
}
