package org.openelisglobal.patient.service;

import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patienttype.valueholder.PatientType;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
public interface PatientTypeService extends BaseObjectService<PatientType, String> {

    List<PatientType> getAllPatientTypes() throws LIMSRuntimeException;

    List<PatientType> getPageOfPatientType(int startingRecNo) throws LIMSRuntimeException;

    void getData(PatientType patientType) throws LIMSRuntimeException;

    List<PatientType> getPatientTypes(String filter) throws LIMSRuntimeException;

    PatientType getPatientTypeByName(PatientType patientType) throws LIMSRuntimeException;

    Integer getTotalPatientTypeCount() throws LIMSRuntimeException;
}
