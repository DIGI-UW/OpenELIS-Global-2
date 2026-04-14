package org.openelisglobal.notebook.service;

import java.util.List;
import java.util.Map;

/**
 * Service for generating patient-centric pathology diagnostic report PDFs.
 * Aggregates data across workflow pages (Sample Creation, Gross Examination,
 * Microscopy & Diagnosis) to produce a formatted report per patient.
 */
public interface PathologyDiagnosticReportService {

    /**
     * Get all patients associated with a notebook entry, grouped by identity.
     *
     * @param entryId the notebook entry ID
     * @return list of patient summary maps with keys: patientKey, firstName,
     *         surname, nationalId, sampleCount, sampleIds, hasDiagnosis
     */
    List<Map<String, Object>> getPatientList(Integer entryId);

    /**
     * Generate a pathology diagnostic report PDF for a specific patient.
     *
     * @param entryId    the notebook entry ID
     * @param patientKey the patient grouping key (nationalId or firstName+surname)
     * @return PDF content as byte array
     * @throws Exception if PDF generation fails
     */
    byte[] generateDiagnosticReportPdf(Integer entryId, String patientKey) throws Exception;
}
