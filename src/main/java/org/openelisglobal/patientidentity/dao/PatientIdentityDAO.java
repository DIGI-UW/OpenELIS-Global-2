package org.openelisglobal.patientidentity.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;

public interface PatientIdentityDAO extends BaseDAO<PatientIdentity, String> {

    // public boolean insertData(PatientIdentity patientIdentity) throws
    // LIMSRuntimeException;

    // public void updateData(PatientIdentity patientIdentity) throws
    // LIMSRuntimeException;

    // public void delete(String patientIdentityId, String activeUserId) throws
    // LIMSRuntimeException;

    public List<PatientIdentity> getPatientIdentitiesForPatient(String id) throws LIMSRuntimeException;

    public List<PatientIdentity> getPatientIdentitiesByValueAndType(String value, String identityType)
            throws LIMSRuntimeException;

    public PatientIdentity getPatitentIdentityForPatientAndType(String patientId, String identityTypeId)
            throws LIMSRuntimeException;
}
