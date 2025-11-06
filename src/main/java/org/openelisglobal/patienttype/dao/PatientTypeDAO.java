/**
 * Project : LIS<br>
 * File name : PatientTypeDAO.java<br>
 * Description :
 *
 * @author TienDH
 * @date Aug 20, 2007
 */
package org.openelisglobal.patienttype.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.patienttype.valueholder.PatientType;

public interface PatientTypeDAO extends BaseDAO<PatientType, String> {

    // public boolean insertData(PatientType patientType) throws
    // LIMSRuntimeException;

    // public void deleteData(List patientType) throws LIMSRuntimeException;

    List<PatientType> getAllPatientTypes() throws LIMSRuntimeException;

    List<PatientType> getPageOfPatientType(int startingRecNo) throws LIMSRuntimeException;

    void getData(PatientType patientType) throws LIMSRuntimeException;

    // public void updateData(PatientType patientType) throws LIMSRuntimeException;

    List<PatientType> getPatientTypes(String filter) throws LIMSRuntimeException;

    PatientType getPatientTypeByName(PatientType patientType) throws LIMSRuntimeException;

    Integer getTotalPatientTypeCount() throws LIMSRuntimeException;

    boolean duplicatePatientTypeExists(PatientType patientType);
}
