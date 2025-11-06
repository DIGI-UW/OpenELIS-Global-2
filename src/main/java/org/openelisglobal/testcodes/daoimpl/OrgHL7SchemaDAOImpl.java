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
package org.openelisglobal.testcodes.daoimpl;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.testcodes.dao.OrgHL7SchemaDAO;
import org.openelisglobal.testcodes.valueholder.OrganizationHL7Schema;
import org.openelisglobal.testcodes.valueholder.OrganizationSchemaPK;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrgHL7SchemaDAOImpl extends BaseDAOImpl<OrganizationHL7Schema, OrganizationSchemaPK>
        implements OrgHL7SchemaDAO {

    public OrgHL7SchemaDAOImpl() {
        super(OrganizationHL7Schema.class);
    }

    @Override
    public OrganizationHL7Schema getOrganizationHL7SchemaByOrgId(String orgId) throws LIMSRuntimeException {
        String sql = "from OrganizationHL7Schema hs where hs.compoundId.organizationId = :id";

        try {
            Query<OrganizationHL7Schema> query = entityManager.unwrap(Session.class).createQuery(sql,
                    OrganizationHL7Schema.class);
            query.setParameter("id", orgId);
            OrganizationHL7Schema hs = query.uniqueResult();
            return hs;
        } catch (HibernateException e) {
            handleException(e, "getOrganizationHL7SchemaByOrgId");
        }
        return null;
    }
}
