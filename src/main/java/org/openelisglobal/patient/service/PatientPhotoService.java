package org.openelisglobal.patient.service;

import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patient.valueholder.PatientPhoto;

public interface PatientPhotoService extends BaseObjectService<PatientPhoto, Integer> {

    PatientPhoto savePhoto(String patientId, String photoBase64) throws LIMSRuntimeException;

    String getPhotoByPatientId(String patientId, boolean isThumbnail) throws LIMSRuntimeException;

}
