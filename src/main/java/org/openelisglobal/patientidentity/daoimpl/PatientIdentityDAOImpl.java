package org.openelisglobal.patientidentity.daoimpl;

import java.util.Collections;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.patientidentity.dao.PatientIdentityDAO;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PatientIdentityDAOImpl extends BaseDAOImpl<PatientIdentity, String> implements PatientIdentityDAO {

    public PatientIdentityDAOImpl() {
        super(PatientIdentity.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientIdentity> getPatientIdentitiesForPatient(String id) {

        List<PatientIdentity> identities;

        try {
            String sql = "from PatientIdentity pi where pi.patientId = :Id";
            Query<PatientIdentity> query = entityManager.unwrap(Session.class).createQuery(sql, PatientIdentity.class);
            query.setParameter("Id", Integer.parseInt(id));

            identities = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PatientIdentityDAOImpl getPatientIdentitiesForPatient()", e);
        }

        return identities;
    }

    @Transactional(readOnly = true)
    public PatientIdentity getCurrentPatientIdentity(String id) {
        PatientIdentity current = null;
        try {
            current = entityManager.unwrap(Session.class).get(PatientIdentity.class, id);
            // entityManager.unwrap(Session.class).flush(); // CSL remove old
            // entityManager.unwrap(Session.class).clear(); // CSL remove old
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PatientIdentity getCurrentPatientIdentity()", e);
        }

        return current;
    }

    // @Override
    // public void delete(String patientIdentityId, String activeUserId) throws
    // LIMSRuntimeException {
    // try {
    //
    // PatientIdentity oldData = readPatientIdentity(patientIdentityId);
    // Patient newData = new Patient();
    //
    // String event = IActionConstants.AUDIT_TRAIL_DELETE;
    // String tableName = "PATIENT_IDENTITY";
    // auditDAO.saveHistory(newData, oldData, activeUserId, event, tableName);
    //
    // entityManager.unwrap(Session.class).delete(oldData);
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    //
    // } catch (RuntimeException e) {
    //
    // LogEvent.logError("PatientIdentityDAOImpl", "delete()", e.toString());
    // throw new LIMSRuntimeException("Error in PatientIdentity delete()", e);
    // }
    // }

    public PatientIdentity readPatientIdentity(String idString) {

        PatientIdentity patientIdentity = null;
        try {
            patientIdentity = entityManager.unwrap(Session.class).get(PatientIdentity.class, idString);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PatientIdentity readPatientIdentity()", e);
        }

        return patientIdentity;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientIdentity> getPatientIdentitiesByValueAndType(String value, String identityType)
            throws LIMSRuntimeException {
        String sql = "From PatientIdentity pi where pi.identityData = :value and pi.identityTypeId ="
                + " :identityType";

        if (value == null) {
            return Collections.emptyList();
        }
        try {
            Query<PatientIdentity> query = entityManager.unwrap(Session.class).createQuery(sql, PatientIdentity.class);
            query.setParameter("value", value);
            query.setParameter("identityType", Integer.parseInt(identityType));

            List<PatientIdentity> identities = query.list();
            return identities;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PatientIdentity getPatientIdentitiesByValueAndType()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PatientIdentity getPatitentIdentityForPatientAndType(String patientId, String identityTypeId)
            throws LIMSRuntimeException {

        String sql = "from PatientIdentity pi where pi.patientId = :patientId and pi.identityTypeId = :typeId";

        try {
            Query<PatientIdentity> query = entityManager.unwrap(Session.class).createQuery(sql, PatientIdentity.class);
            query.setParameter("patientId", Integer.parseInt(patientId));
            query.setParameter("typeId", Integer.parseInt(identityTypeId));

            PatientIdentity pi = query.uniqueResult();
            return pi;
        } catch (HibernateException e) {
            handleException(e, "getPatitentIdentityForPatientAndType");
        }

        return null;
    }
}
