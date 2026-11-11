package org.openelisglobal.organization.locations;

/**
 * A change the current state does not allow: another admin saved first (FR-C5),
 * a reactivation under an inactive parent (FR-E3), or a deactivation of an area
 * that still holds active records (FR-B7).
 */
public class LocationsConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Object current;

    public LocationsConflictException(String message) {
        this(message, null);
    }

    public LocationsConflictException(String message, Object current) {
        super(message);
        this.current = current;
    }

    /** What is stored now, for a stale-save conflict; null otherwise. */
    public Object getCurrent() {
        return current;
    }
}
