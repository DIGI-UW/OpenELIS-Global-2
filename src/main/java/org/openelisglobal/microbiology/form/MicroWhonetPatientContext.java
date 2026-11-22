package org.openelisglobal.microbiology.form;

import java.sql.Date;
import java.sql.Timestamp;

public record MicroWhonetPatientContext(String sampleItemId, String patientId, String nationalId, String firstName,
        String lastName, String gender, Timestamp birthDate, String accessionNumber, Date enteredDate,
        Timestamp collectionDate, String specimenTypeId, String specimenTypeLabel, String specimenTypeCode,
        Double latitude, Double longitude) {
}
