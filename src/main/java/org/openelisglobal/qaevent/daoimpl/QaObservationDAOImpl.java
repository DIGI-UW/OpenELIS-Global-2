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
import org.openelisglobal.qaevent.dao.QaObservationDAO;
import org.openelisglobal.qaevent.valueholder.QaObservation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class QaObservationDAOImpl extends BaseDAOImpl<QaObservation, String> implements QaObservationDAO {

    public QaObservationDAOImpl() {
        super(QaObservation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public QaObservation getQaObservationByTypeAndObserved(String typeName, String observedType, String observedId)
            throws LIMSRuntimeException {
        String sql = "FROM QaObservation o where o.observationType.name = :observationName and o.observedType ="
                + " :observedType and o.observedId = :observedId ";

        try {
            Query<QaObservation> query = entityManager.unwrap(Session.class).createQuery(sql, QaObservation.class);
            query.setParameter("observationName", typeName);
            query.setParameter("observedType", observedType);
            query.setParameter("observedId", Integer.parseInt(observedId));
            QaObservation observation = query.uniqueResult();
            return observation;
        } catch (HibernateException e) {
            handleException(e, "getQaObservationByTypeAndObserved");
        }
        return null;
    }

    public QaObservation readQaObservation(String idString) {
        QaObservation qaObservation = null;
        try {
            qaObservation = entityManager.unwrap(Session.class).get(QaObservation.class, idString);
        } catch (RuntimeException e) {
            handleException(e, "readQaObservation");
        }

        return qaObservation;
    }
}
