package org.openelisglobal.microbiology.dao;

import java.sql.Date;
import java.sql.Timestamp;

public record MicroWhonetContext(String caseId, String sampleItemId, String patientId, String nationalId,
        String firstName, String lastName, String gender, Timestamp birthDate, String accessionNumber, Date enteredDate,
        Timestamp collectionDate, String specimenTypeId, String specimenTypeLabel, String specimenType, Double latitude,
        Double longitude) {
}
