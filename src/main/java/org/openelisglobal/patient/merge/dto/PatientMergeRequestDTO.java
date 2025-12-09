package org.openelisglobal.patient.merge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for patient merge request from REST API. Contains the two patient IDs to
 * merge, primary patient selection, reason, and confirmation flag.
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

    @NotNull(message = "Confirmation is required")
    private Boolean confirmed;
}
