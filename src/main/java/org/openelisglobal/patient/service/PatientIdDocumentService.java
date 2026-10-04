package org.openelisglobal.patient.service;

import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patient.valueholder.PatientIdDocument;
import org.springframework.security.access.prepost.PreAuthorize;

public interface PatientIdDocumentService extends BaseObjectService<PatientIdDocument, Integer> {

    @PreAuthorize("hasAuthority('PRIV_PATIENT_EDIT')")
    PatientIdDocument saveDocument(String patientId, String documentBase64, String documentCategory, String description,
            String sysUserId) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    List<PatientIdDocument> getDocumentsByPatientId(String patientId) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_PATIENT_EDIT')")
    void softDeleteDocument(Integer documentId, String sysUserId) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_PATIENT_EDIT')")
    PatientIdDocument updateDocumentCategory(Integer documentId, String documentCategory, String description,
            String sysUserId) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_PATIENT_EDIT')")
    PatientIdDocument updateDocument(Integer documentId, String documentBase64, String documentCategory,
            String description, String sysUserId) throws LIMSRuntimeException;
}
