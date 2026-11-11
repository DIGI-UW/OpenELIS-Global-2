package org.openelisglobal.patient.daoimpl;

import java.util.Collections;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.patient.dao.PatientIdDocumentDAO;
import org.openelisglobal.patient.valueholder.PatientIdDocument;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PatientIdDocumentDAOImpl extends BaseDAOImpl<PatientIdDocument, Integer> implements PatientIdDocumentDAO {

    public PatientIdDocumentDAOImpl() {
        super(PatientIdDocument.class);
    }

    @Override
    public List<PatientIdDocument> getByPatientId(String patientId) {
        try {
            if (entityManager == null) {
                return Collections.emptyList();
            }

            Session session = entityManager.unwrap(Session.class);
            if (session == null) {
                return Collections.emptyList();
            }

            String hql = "FROM PatientIdDocument d WHERE d.patientId = :patientId AND d.deleted = false";
            Query<PatientIdDocument> query = session.createQuery(hql, PatientIdDocument.class);
            query.setParameter("patientId", patientId);

            return query.getResultList();
        } catch (Exception e) {
            handleException(e, "getByPatientId");
            return Collections.emptyList();
        }
    }
}
