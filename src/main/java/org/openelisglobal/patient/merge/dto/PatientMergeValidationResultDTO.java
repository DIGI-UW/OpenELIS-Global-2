package org.openelisglobal.patient.merge.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * DTO representing the result of patient merge validation. Contains validation
 * status, errors, warnings, and data summary.
 */
@Data
public class PatientMergeValidationResultDTO {

    private boolean valid;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private PatientMergeDataSummaryDTO dataSummary;

    /**
     * Creates a successful validation result with data summary.
     */
    public static PatientMergeValidationResultDTO success(PatientMergeDataSummaryDTO dataSummary) {
        PatientMergeValidationResultDTO result = new PatientMergeValidationResultDTO();
        result.setValid(true);
        result.setDataSummary(dataSummary);
        return result;
    }

    /**
     * Creates a failed validation result with error message.
     */
    public static PatientMergeValidationResultDTO failure(String error) {
        PatientMergeValidationResultDTO result = new PatientMergeValidationResultDTO();
        result.setValid(false);
        result.getErrors().add(error);
        return result;
    }

    /**
     * Adds a warning to the validation result.
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * Adds an error to the validation result.
     */
    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }
}
