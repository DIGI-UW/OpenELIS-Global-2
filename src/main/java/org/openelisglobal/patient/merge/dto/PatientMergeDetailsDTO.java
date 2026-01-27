package org.openelisglobal.patient.merge.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * DTO containing detailed patient information for merge preview. Includes
 * patient demographics, data summary, identifiers, and conflicting fields.
 */
@Data
public class PatientMergeDetailsDTO {

    private String patientId;
    private String firstName;
    private String lastName;
    private String gender;
    private String birthDate;
    private String nationalId;
    private String phoneNumber;
    private String email;
    private String address;

    private PatientMergeDataSummaryDTO dataSummary;
    private List<IdentifierDTO> identifiers = new ArrayList<>();
    private List<String> conflictingFields = new ArrayList<>();

    /**
     * Inner DTO for patient identifiers.
     */
    @Data
    public static class IdentifierDTO {
        private String identityType;
        private String identityValue;
        private String system;
    }
}
