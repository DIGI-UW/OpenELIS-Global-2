package org.openelisglobal.dataexchange.fhir.exception;

public class FhirGeneralException extends Exception {

    public FhirGeneralException() {
        super();
    }

    public FhirGeneralException(String message) {
        super(message);
    }

    public FhirGeneralException(Exception e) {
        super(e);
    }
}
