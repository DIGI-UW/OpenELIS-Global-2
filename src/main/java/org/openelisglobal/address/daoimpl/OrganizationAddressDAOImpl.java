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
package org.openelisglobal.address.daoimpl;

import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.address.dao.OrganizationAddressDAO;
import org.openelisglobal.address.valueholder.AddressPK;
import org.openelisglobal.address.valueholder.OrganizationAddress;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrganizationAddressDAOImpl extends BaseDAOImpl<OrganizationAddress, AddressPK>
        implements OrganizationAddressDAO {

    public OrganizationAddressDAOImpl() {
        super(OrganizationAddress.class);
    }

    @Override
    public List<OrganizationAddress> getAddressPartsByOrganizationId(String organizationId)
            throws LIMSRuntimeException {
        String sql = "from OrganizationAddress pa where pa.compoundId.targetId = :organizationId";

        try {
            Query<OrganizationAddress> query = entityManager.unwrap(Session.class).createQuery(sql,
                    OrganizationAddress.class);
            query.setParameter("organizationId", Integer.parseInt(organizationId));
            List<OrganizationAddress> addressPartList = query.list();
            return addressPartList;
        } catch (HibernateException e) {
            handleException(e, "getAddressPartsByOrganizationId");
        }

        return null;
    }
}
