package org.openelisglobal.patient.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.patient.dao.PatientContactDAO;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PatientContactDAOImpl extends BaseDAOImpl<PatientContact, String> implements PatientContactDAO {

    public PatientContactDAOImpl() {
        super(PatientContact.class);
    }

    @Override
    public List<PatientContact> getForPatient(String patientId) {
        List<PatientContact> patients;

        try {
            String sql = "From PatientContact p where p.patientId = :patientId";
            Query<PatientContact> query = entityManager.unwrap(Session.class).createQuery(sql, PatientContact.class);
            query.setParameter("patientId", Integer.parseInt(patientId));
            patients = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PatientContactDAOImpl getForPatient() ", e);
        }
        return patients;
    }
}
