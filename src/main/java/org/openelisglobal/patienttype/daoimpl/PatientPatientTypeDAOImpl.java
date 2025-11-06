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
package org.openelisglobal.patienttype.daoimpl;

import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.patienttype.dao.PatientPatientTypeDAO;
import org.openelisglobal.patienttype.valueholder.PatientPatientType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PatientPatientTypeDAOImpl extends BaseDAOImpl<PatientPatientType, String>
        implements PatientPatientTypeDAO {

    public PatientPatientTypeDAOImpl() {
        super(PatientPatientType.class);
    }

    @Transactional(readOnly = true)
    public PatientPatientType getCurrentPatientPatientType(String id) {
        PatientPatientType current = null;
        try {
            current = entityManager.unwrap(Session.class).get(PatientPatientType.class, id);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PatientPatientType getCurrentPatientPatientType()", e);
        }

        return current;
    }

    @Override
    @Transactional(readOnly = true)
    public PatientPatientType getPatientPatientTypeForPatient(String patientId) throws LIMSRuntimeException {
        List<PatientPatientType> patientTypes;

        try {
            String sql = "from PatientPatientType pi where pi.patientId = :patientId";
            Query<PatientPatientType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    PatientPatientType.class);
            query.setParameter("patientId", Integer.parseInt(patientId));

            patientTypes = query.list();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PatientIdentityDAOImpl getPatientPatientTypeForPatient()", e);
        }

        if (patientTypes.size() > 0) {
            PatientPatientType patientPatientType = patientTypes.get(0);

            return patientPatientType;
        }

        return null;
    }
}
