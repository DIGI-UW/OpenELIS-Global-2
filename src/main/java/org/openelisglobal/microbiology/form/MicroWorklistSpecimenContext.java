package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public record MicroWorklistSpecimenContext(String sampleItemId, String accessionNumber, String patientDisplay,
        String specimenDisplay, Timestamp collectionDate, String specimenTypeId) {
}
