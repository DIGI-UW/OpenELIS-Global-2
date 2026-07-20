package org.openelisglobal.fhir;

public final class FhirConstants {

    private FhirConstants() {
    }

    public static final String ID_PROPERTY = "fhirUuid";

    public static final String LAST_UPDATED_PROPERTY = "lastupdated";

    public static final String PERSON = "person";

    public static final String ADDRESS = "address";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String CITY = "city";
    public static final String STATE = "state";
    public static final String POSTAL_CODE = "zipCode";
    public static final String COUNTRY = "country";

    public static final String IDENTIFIER_SEARCH_HANDLER = ID_PROPERTY;

    public static final String FIRST_NAME_SEARCH_HANDLER = PERSON + "." + FIRST_NAME;

    public static final String LAST_NAME_SEARCH_HANDLER = PERSON + "." + LAST_NAME;

    public static final String CITY_SEARCH_HANDLER = PERSON + "." + ADDRESS + "." + CITY;

    public static final String STATE_SEARCH_HANDLER = PERSON + "." + ADDRESS + "." + STATE;

    public static final String POSTALCODE_SEARCH_HANDLER = PERSON + "." + ADDRESS + "." + POSTAL_CODE;

    public static final String COUNTRY_SEARCH_HANDLER = PERSON + "." + ADDRESS + "." + COUNTRY;
}