package org.openelisglobal.patient.service;

import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patient.valueholder.PatientPhoto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface PatientPhotoService extends BaseObjectService<PatientPhoto, Integer> {

    @PreAuthorize("hasAuthority('PRIV_PATIENT_EDIT')")
    PatientPhoto savePhoto(String patientId, String photoBase64, String sysUserId) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getPhotoByPatientId(String patientId, boolean isThumbnail) throws LIMSRuntimeException;

}
