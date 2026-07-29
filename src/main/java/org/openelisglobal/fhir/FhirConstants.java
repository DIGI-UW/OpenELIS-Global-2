package org.openelisglobal.fhir;

public final class FhirConstants {

    private FhirConstants() {
    }

    public static final String ID_PROPERTY = "fhirUuid";

    public static final String LAST_UPDATED_PROPERTY = "lastupdated";

    public static final String PERSON = "person";

    public static final String TELECOM = "telecom";

    public static final String ADDRESS = "address";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String CITY = "city";
    public static final String STATE = "state";
    public static final String POSTAL_CODE = "zipCode";
    public static final String COUNTRY = "country";
    public static final String EMAIL = "email";
    public static final String WORK_PHONE = "workPhone";
    public static final String HOME_PHONE = "homePhone";
    public static final String CELL_PHONE = "cellPhone";
    public static final String PRIMARY_PHONE = "primaryPhone";
    public static final String FAX = "fax";

    public static final String IDENTIFIER_SEARCH_HANDLER = ID_PROPERTY;

    public static final String FIRST_NAME_SEARCH_HANDLER = PERSON + "." + FIRST_NAME;

    public static final String LAST_NAME_SEARCH_HANDLER = PERSON + "." + LAST_NAME;

    public static final String CITY_SEARCH_HANDLER = PERSON + "." + CITY;

    public static final String STATE_SEARCH_HANDLER = PERSON + "." + STATE;

    public static final String POSTALCODE_SEARCH_HANDLER = PERSON + "." + POSTAL_CODE;

    public static final String COUNTRY_SEARCH_HANDLER = PERSON + "." + COUNTRY;

    public static final String EMAIL_SEARCH_HANDLER = PERSON + "." + EMAIL;
    public static final String WORK_PHONE_SEARCH_HANDLER = PERSON + "." + WORK_PHONE;
    public static final String HOME_PHONE_SEARCH_HANDLER = PERSON + "." + HOME_PHONE;
    public static final String CELL_PHONE_SEARCH_HANDLER = PERSON + "." + CELL_PHONE;
    public static final String PRIMARY_PHONE_SEARCH_HANDLER = PERSON + "." + PRIMARY_PHONE;
    public static final String FAX_SEARCH_HANDLER = PERSON + "." + FAX;
    public static final String NAME_SEARCH_HANDLER = "name";
    public static final String TELECOM_SEARCH_HANDLER = "telecom";
}