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
package org.openelisglobal.qaevent.daoimpl;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qaevent.dao.QaObservationTypeDAO;
import org.openelisglobal.qaevent.valueholder.QaObservationType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class QaObservationTypeDAOImpl extends BaseDAOImpl<QaObservationType, String> implements QaObservationTypeDAO {

    public QaObservationTypeDAOImpl() {
        super(QaObservationType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public QaObservationType getQaObservationTypeByName(String typeName) throws LIMSRuntimeException {
        String sql = "FROM QaObservationType where name = :name";

        try {
            Query<QaObservationType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    QaObservationType.class);
            query.setParameter("name", typeName);
            QaObservationType type = query.uniqueResult();
            return type;
        } catch (HibernateException e) {
            handleException(e, "getQaObservationTypeByName");
        }
        return null;
    }
}
