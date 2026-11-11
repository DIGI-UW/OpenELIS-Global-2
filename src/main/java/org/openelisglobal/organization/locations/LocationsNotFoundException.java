package org.openelisglobal.organization.locations;

/** A record the menu was asked for that does not exist. */
public class LocationsNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LocationsNotFoundException(String message) {
        super(message);
    }
}
