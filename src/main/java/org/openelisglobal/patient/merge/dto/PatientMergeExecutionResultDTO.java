package org.openelisglobal.patient.merge.dto;

import lombok.Data;

/**
 * DTO representing the result of patient merge execution. Contains success
 * status, merge audit ID, message, and performance metrics.
 */
@Data
public class PatientMergeExecutionResultDTO {

    private boolean success;
    private String mergeAuditId;
    private String message;
    private String primaryPatientId;
    private String mergedPatientId;
    private Long mergeDurationMs;

    /**
     * Creates a successful execution result.
     */
    public static PatientMergeExecutionResultDTO success(String mergeAuditId, String primaryPatientId,
            String mergedPatientId, long durationMs) {
        PatientMergeExecutionResultDTO result = new PatientMergeExecutionResultDTO();
        result.setSuccess(true);
        result.setMergeAuditId(mergeAuditId);
        result.setPrimaryPatientId(primaryPatientId);
        result.setMergedPatientId(mergedPatientId);
        result.setMergeDurationMs(durationMs);
        result.setMessage("Patient merge completed successfully");
        return result;
    }

    /**
     * Creates a failed execution result.
     */
    public static PatientMergeExecutionResultDTO failure(String errorMessage) {
        PatientMergeExecutionResultDTO result = new PatientMergeExecutionResultDTO();
        result.setSuccess(false);
        result.setMessage(errorMessage);
        return result;
    }
}
