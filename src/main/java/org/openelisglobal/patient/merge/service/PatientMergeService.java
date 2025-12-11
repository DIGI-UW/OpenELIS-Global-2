package org.openelisglobal.patient.merge.service;

import org.openelisglobal.patient.merge.dto.PatientMergeDetailsDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeExecutionResultDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeRequestDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeValidationResultDTO;

/**
 * Service interface for patient merge operations. Handles validation, data
 * consolidation, FHIR synchronization, and audit trail creation.
 */
public interface PatientMergeService {

    /**
     * Validates if two patients can be merged. Checks permissions, patient
     * eligibility, circular references, and already merged status.
     *
     * @param request The merge request containing patient IDs and primary selection
     * @return Validation result with errors, warnings, and data summary
     */
    PatientMergeValidationResultDTO validateMerge(PatientMergeRequestDTO request);

    /**
     * Retrieves detailed information about a patient for merge preview. Includes
     * demographics, data counts, identifiers, and potential conflicts.
     *
     * @param patientId The patient ID to retrieve details for
     * @return Patient details with data summary
     */
    PatientMergeDetailsDTO getMergeDetails(String patientId);

    /**
     * Executes the patient merge operation. Consolidates all data, updates FHIR
     * resources, and creates audit trail. Entire operation runs in a single
     * transaction with rollback on failure.
     *
     * @param request   The merge request with confirmation flag
     * @param sysUserId The ID of the user performing the merge (for audit trail)
     * @return Execution result with success status and merge audit ID
     */
    PatientMergeExecutionResultDTO executeMerge(PatientMergeRequestDTO request, String sysUserId);
}
