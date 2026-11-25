package org.openelisglobal.facilityregistry.service;

/**
 * A browser position was sent while the facility location is in manual mode,
 * where coordinates come only from the latitude and longitude properties.
 */
public class FacilityLocationModeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FacilityLocationModeException(String message) {
        super(message);
    }
}
