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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.siteinformation.daoimpl;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.siteinformation.dao.SiteInformationDomainDAO;
import org.openelisglobal.siteinformation.valueholder.SiteInformationDomain;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SiteInformationDomainDAOImpl extends BaseDAOImpl<SiteInformationDomain, String>
        implements SiteInformationDomainDAO {

    public SiteInformationDomainDAOImpl() {
        super(SiteInformationDomain.class);
    }

    @Override
    @Transactional(readOnly = true)
    public SiteInformationDomain getByName(String name) throws LIMSRuntimeException {
        String sql = "from SiteInformationDomain sid where sid.name = :name";

        try {
            Query<SiteInformationDomain> query = entityManager.unwrap(Session.class).createQuery(sql,
                    SiteInformationDomain.class);
            query.setParameter("name", name);
            SiteInformationDomain domain = query.uniqueResult();
            return domain;
        } catch (HibernateException e) {
            handleException(e, "getByName");
        }
        return null;
    }
}
