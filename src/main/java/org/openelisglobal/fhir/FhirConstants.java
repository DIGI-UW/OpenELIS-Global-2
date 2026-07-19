package org.openelisglobal.fhir;

public final class FhirConstants {

    private FhirConstants() {
    }

    /*
     * =========================================================================
     * Common entity properties
     * =========================================================================
     */

    /**
     * FHIR Resource id (_id)
     */
    public static final String ID_PROPERTY = "fhirUuid";

    /**
     * Entity last updated timestamp
     */
    public static final String LAST_UPDATED_PROPERTY = "lastupdated";

    /*
     * =========================================================================
     * Entity relationships
     * =========================================================================
     */

    /**
     * Provider -> Person
     */
    public static final String PERSON = "person";

    /**
     * Person -> Address
     */
    public static final String ADDRESS = "address";

    /*
     * =========================================================================
     * Person fields
     * =========================================================================
     */

    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";

    /*
     * Address fields
     * =========================================================================
     */

    public static final String CITY = "city";
    public static final String STATE = "state";
    public static final String POSTAL_CODE = "zipCode";
    public static final String COUNTRY = "country";

    /*
     * =========================================================================
     * Search property paths
     * =========================================================================
     */

    /**
     * Practitioner.identifier
     *
     * Your implementation stores identifiers as UUIDs, so this currently maps to
     * the same database property as _id.
     */
    public static final String IDENTIFIER_SEARCH_HANDLER = ID_PROPERTY;

    /**
     * Practitioner.given
     */
    public static final String FIRST_NAME_SEARCH_HANDLER = PERSON + "." + FIRST_NAME;

    /**
     * Practitioner.family
     */
    public static final String LAST_NAME_SEARCH_HANDLER = PERSON + "." + LAST_NAME;

    /**
     * Practitioner.address-city
     */
    public static final String CITY_SEARCH_HANDLER = PERSON + "." + ADDRESS + "." + CITY;

    /**
     * Practitioner.address-state
     */
    public static final String STATE_SEARCH_HANDLER = PERSON + "." + ADDRESS + "." + STATE;

    /**
     * Practitioner.address-postalCode
     */
    public static final String POSTALCODE_SEARCH_HANDLER = PERSON + "." + ADDRESS + "." + POSTAL_CODE;

    /**
     * Practitioner.address-country
     */
    public static final String COUNTRY_SEARCH_HANDLER = PERSON + "." + ADDRESS + "." + COUNTRY;
}