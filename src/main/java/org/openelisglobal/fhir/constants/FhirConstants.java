package org.openelisglobal.fhir.constants;

public final class FhirConstants {

    public static final String ID_PROPERTY = "fhirUuid";
    public static final String IDENTIFIER_PROPERTY = "identifier";
    public static final String LAST_UPDATED_PROPERTY = "lastupdated";
    public static final String GIVEN_NAME_PROPERTY = "given";
    public static final String FAMILY_NAME_PROPERTY = "family";
    public static final String ACTIVE_PROPERTY = "active";

    public static final String ID_SEARCH_HANDLER = "fhirUuid";
    public static final String IDENTIFIER_SEARCH_HANDLER = "identifier";
    public static final String LAST_UPDATED_SEARCH_HANDLER = "lastupdated";
    public static final String GIVEN_NAME_SEARCH_HANDLER = "person.firstName";
    public static final String FAMILY_NAME_SEARCH_HANDLER = "person.lastName";

    private FhirConstants() {

    }
}