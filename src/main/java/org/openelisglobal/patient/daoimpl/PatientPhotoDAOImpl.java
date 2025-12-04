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
package org.openelisglobal.patient.daoimpl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.patient.dao.PatientPhotoDAO;
import org.openelisglobal.patient.valueholder.PatientPhoto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PatientPhotoDAOImpl extends BaseDAOImpl<PatientPhoto, Integer> implements PatientPhotoDAO {

    public PatientPhotoDAOImpl() {
        super(PatientPhoto.class);
    }

    @Override
    public PatientPhoto getByPatientId(String patientId) {
        try {

            if (entityManager == null) {
                return null;
            }

            Session session = entityManager.unwrap(Session.class);
            if (session == null) {
                return null;
            }

            String hql = "FROM PatientPhoto p WHERE p.patientId = :patientId";
            Query<PatientPhoto> query = session.createQuery(hql, PatientPhoto.class);
            query.setParameter("patientId", patientId);

            PatientPhoto result = query.uniqueResult();

            return result;
        } catch (Exception e) {
            System.out.println("---- Exception in getByPatientId: " + e.getMessage() + " ----");
            handleException(e, "getByPatientId");
            return null;
        }
    }

}
