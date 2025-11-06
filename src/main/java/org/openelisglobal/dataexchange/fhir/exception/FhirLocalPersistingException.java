package org.openelisglobal.dataexchange.fhir.exception;

public class FhirLocalPersistingException extends FhirPersistanceException {

    public FhirLocalPersistingException() {
        super();
    }

    public FhirLocalPersistingException(String message) {
        super(message);
    }

    public FhirLocalPersistingException(Exception e) {
        super(e);
    }
}
