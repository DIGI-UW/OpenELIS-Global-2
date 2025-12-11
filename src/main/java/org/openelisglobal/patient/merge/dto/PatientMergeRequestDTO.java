package org.openelisglobal.patient.merge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for patient merge request from REST API. Contains the two patient IDs to
 * merge, primary patient selection, reason, and confirmation flag.
 *
 * Design Note: The DTO includes patient1Id, patient2Id, AND primaryPatientId.
 * While this appears redundant (primaryPatientId must equal one of
 * patient1Id/patient2Id), it's a frontend-friendly design that allows the UI to
 * present both patients side-by-side and let the user select which should be
 * primary. The backend validates that primaryPatientId matches one of the
 * provided patient IDs.
 *
 * Alternative: Could simplify to just primaryPatientId + mergedPatientId, but
 * current design provides better UX for the merge workflow.
 */
@Data
public class PatientMergeRequestDTO {

    @NotBlank(message = "Patient 1 ID is required")
    private String patient1Id;

    @NotBlank(message = "Patient 2 ID is required")
    private String patient2Id;

    @NotBlank(message = "Primary patient ID is required")
    private String primaryPatientId;

    @NotBlank(message = "Merge reason is required")
    private String reason;

    /**
     * Confirmation flag ensures user has reviewed merge details before execution.
     * While this could be a frontend-only concern, including it in the API provides
     * defense-in-depth: prevents accidental merge execution if frontend validation
     * fails, and provides audit trail that user explicitly confirmed the
     * destructive operation. Backend enforces confirmed=true for /execute endpoint
     * (returns 400 if false).
     */
    @NotNull(message = "Confirmation is required")
    private Boolean confirmed;
}
