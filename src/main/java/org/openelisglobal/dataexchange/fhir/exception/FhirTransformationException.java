package org.openelisglobal.dataexchange.fhir.exception;

public class FhirTransformationException extends FhirGeneralException {

    public FhirTransformationException() {
        super();
    }

    public FhirTransformationException(String message) {
        super(message);
    }

    public FhirTransformationException(Exception e) {
        super(e);
    }
}
