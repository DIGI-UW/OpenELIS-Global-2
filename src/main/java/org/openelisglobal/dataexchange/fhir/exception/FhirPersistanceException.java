package org.openelisglobal.dataexchange.fhir.exception;

public class FhirPersistanceException extends FhirGeneralException {

    public FhirPersistanceException() {
        super();
    }

    public FhirPersistanceException(String message) {
        super(message);
    }

    public FhirPersistanceException(Exception e) {
        super(e);
    }
}
