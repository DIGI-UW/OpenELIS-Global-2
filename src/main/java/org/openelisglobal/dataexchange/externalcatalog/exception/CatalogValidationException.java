package org.openelisglobal.dataexchange.externalcatalog.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when an inbound catalog request fails pre-flight validation. Carries a
 * list of human-readable error messages so the controller can return them all
 * at once in a structured 400 response.
 */
public class CatalogValidationException extends RuntimeException {

    private final List<String> errors;

    public CatalogValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = Collections.unmodifiableList(errors);
    }

    public CatalogValidationException(String singleError) {
        this(Collections.singletonList(singleError));
    }

    public List<String> getErrors() {
        return errors;
    }
}
