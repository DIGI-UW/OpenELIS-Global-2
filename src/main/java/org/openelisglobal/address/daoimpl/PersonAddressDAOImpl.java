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
import org.openelisglobal.address.dao.PersonAddressDAO;
import org.openelisglobal.address.valueholder.AddressPK;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PersonAddressDAOImpl extends BaseDAOImpl<PersonAddress, AddressPK> implements PersonAddressDAO {

    public PersonAddressDAOImpl() {
        super(PersonAddress.class);
    }

    @Override
    public List<PersonAddress> getAddressPartsByPersonId(String personId) throws LIMSRuntimeException {
        String sql = "from PersonAddress pa where pa.compoundId.targetId = :personId";

        try {
            Query<PersonAddress> query = entityManager.unwrap(Session.class).createQuery(sql, PersonAddress.class);
            query.setParameter("personId", Integer.parseInt(personId));
            List<PersonAddress> addressPartList = query.list();
            return addressPartList;
        } catch (HibernateException e) {
            handleException(e, "getAddressPartsByPersonId");
        }

        return null;
    }

    @Override
    public PersonAddress getByPersonIdAndPartId(String personId, String addressPartId) throws LIMSRuntimeException {
        String sql = "from PersonAddress pa where pa.compoundId.targetId = :personId and"
                + " pa.compoundId.addressPartId = :partId";

        try {
            Query<PersonAddress> query = entityManager.unwrap(Session.class).createQuery(sql, PersonAddress.class);
            query.setParameter("personId", Integer.parseInt(personId));
            query.setParameter("partId", Integer.parseInt(addressPartId));
            PersonAddress addressPart = query.uniqueResult();
            return addressPart;
        } catch (HibernateException e) {
            handleException(e, "getByPersonIdAndPartId");
        }

        return null;
    }
}
