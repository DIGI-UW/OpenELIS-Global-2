package org.openelisglobal.patient.service;

import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patient.valueholder.PatientIdDocument;

public interface PatientIdDocumentService extends BaseObjectService<PatientIdDocument, Integer> {

    PatientIdDocument saveDocument(String patientId, String documentBase64, String documentCategory, String description)
            throws LIMSRuntimeException;

    List<PatientIdDocument> getDocumentsByPatientId(String patientId) throws LIMSRuntimeException;

    void softDeleteDocument(Integer documentId) throws LIMSRuntimeException;

    PatientIdDocument updateDocumentCategory(Integer documentId, String documentCategory, String description)
            throws LIMSRuntimeException;

    PatientIdDocument updateDocument(Integer documentId, String documentBase64, String documentCategory,
            String description) throws LIMSRuntimeException;
}
