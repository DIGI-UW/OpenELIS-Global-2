package org.openelisglobal.fhir.search.searchparams;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.openelisglobal.fhir.FhirConstants;

public class PractitionerSearchParams extends BaseSearchParam {

    private StringAndListParam name;

    private StringAndListParam given;

    private StringAndListParam family;

    private StringAndListParam city;

    private StringAndListParam state;

    private StringAndListParam postalCode;

    private StringAndListParam country;

    public PractitionerSearchParams(TokenAndListParam identifier, StringAndListParam name, StringAndListParam given,
            StringAndListParam family, StringAndListParam city, StringAndListParam state, StringAndListParam postalCode,
            TokenAndListParam id, DateRangeParam lastUpdated, StringAndListParam country) {

        super(id, identifier, lastUpdated);

        this.name = name;
        this.given = given;
        this.family = family;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    @Override
    public SearchParameterMap toSearchParameterMap() {
        SearchParameterMap map = new SearchParameterMap();

        // 1. Add inherited parent fields safely
        if (getId() != null) {
            map.addParameter(FhirConstants.ID_PROPERTY, getId());
        }
        if (getIdentifier() != null) {
            map.addParameter(FhirConstants.IDENTIFIER_SEARCH_HANDLER, getIdentifier());
        }
        if (getLastUpdated() != null) {
            map.addParameter(FhirConstants.LAST_UPDATED_PROPERTY, getLastUpdated());
        }

        // 2. Add resource-specific parameters as complete objects to preserve nested
        // AND/OR tokens

        if (getGiven() != null) {
            map.addParameter(FhirConstants.FIRST_NAME_SEARCH_HANDLER, getGiven());
        }
        if (getFamily() != null) {
            map.addParameter(FhirConstants.LAST_NAME_SEARCH_HANDLER, getFamily());
        }
        if (getCity() != null) {
            map.addParameter(FhirConstants.CITY_SEARCH_HANDLER, getCity());
        }
        if (getState() != null) {
            map.addParameter(FhirConstants.STATE_SEARCH_HANDLER, getState());
        }
        if (getPostalCode() != null) {
            map.addParameter(FhirConstants.POSTALCODE_SEARCH_HANDLER, getPostalCode());
        }
        if (getCountry() != null) {
            map.addParameter(FhirConstants.COUNTRY_SEARCH_HANDLER, getCountry());
        }

        return map;
    }

    public StringAndListParam getName() {
        return name;
    }

    public void setName(StringAndListParam name) {
        this.name = name;
    }

    public StringAndListParam getGiven() {
        return given;
    }

    public void setGiven(StringAndListParam given) {
        this.given = given;
    }

    public StringAndListParam getFamily() {
        return family;
    }

    public void setFamily(StringAndListParam family) {
        this.family = family;
    }

    public StringAndListParam getCity() {
        return city;
    }

    public void setCity(StringAndListParam city) {
        this.city = city;
    }

    public StringAndListParam getState() {
        return state;
    }

    public void setState(StringAndListParam state) {
        this.state = state;
    }

    public StringAndListParam getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(StringAndListParam postalCode) {
        this.postalCode = postalCode;
    }

    public StringAndListParam getCountry() {
        return country;
    }

    public void setCountry(StringAndListParam country) {
        this.country = country;
    }
}