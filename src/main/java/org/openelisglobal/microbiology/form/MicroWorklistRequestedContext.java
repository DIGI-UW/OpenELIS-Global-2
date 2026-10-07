package org.openelisglobal.microbiology.form;

public record MicroWorklistRequestedContext(String caseId, String accessionNumber, String patientDisplay,
        String specimenDisplay) {
}
