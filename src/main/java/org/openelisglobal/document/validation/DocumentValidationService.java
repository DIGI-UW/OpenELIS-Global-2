package org.openelisglobal.document.validation;

import java.io.InputStream;

public interface DocumentValidationService {
    /**
     * Validate file size and content type. Throws IllegalArgumentException on validation failure.
     */
    void validate(InputStream data, String contentType, long size) throws Exception;
}
