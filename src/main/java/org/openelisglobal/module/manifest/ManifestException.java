package org.openelisglobal.module.manifest;

/**
 * A manifest that cannot be read at all (not JSON, or not bindable to the
 * model).
 */
public class ManifestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ManifestException(String message) {
        super(message);
    }

    public ManifestException(String message, Throwable cause) {
        super(message, cause);
    }
}
