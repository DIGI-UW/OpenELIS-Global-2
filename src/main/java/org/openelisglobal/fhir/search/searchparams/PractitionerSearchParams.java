package org.openelisglobal.fhir.search.searchparams;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.openelisglobal.fhir.FhirConstants;

public class PractitionerSearchParams extends BaseSearchParam {

    private StringAndListParam name;

    private StringAndListParam given;

    private StringAndListParam family;

    private StringAndListParam city;

    private StringAndListParam state;

    private StringAndListParam postalCode;

    private StringAndListParam country;

    private TokenAndListParam telecom;

    private TokenAndListParam email;

    private TokenAndListParam phone;

    private SortSpec sort;

    private final Set<Include> revIncludes;

    public PractitionerSearchParams(TokenAndListParam identifier, StringAndListParam name, StringAndListParam given,
            StringAndListParam family, StringAndListParam city, StringAndListParam state, StringAndListParam postalCode,
            StringAndListParam country, TokenAndListParam telecom, TokenAndListParam email, TokenAndListParam phone,
            TokenAndListParam id, DateRangeParam lastUpdated, SortSpec sort, Set<Include> revIncludes) {

        /*
         * This keeps compatibility with a BaseSearchParam constructor containing only
         * id, identifier and lastUpdated.
         */
        super(id, identifier, lastUpdated);

        this.name = name;
        this.given = given;
        this.family = family;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.telecom = telecom;
        this.email = email;
        this.phone = phone;
        this.sort = sort;

        this.revIncludes = revIncludes == null ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(revIncludes));
    }

    @Override
    public SearchParameterMap toSearchParameterMap() {

        SearchParameterMap map = new SearchParameterMap();

        addBaseSearchParameters(map);
        addPractitionerSearchParameters(map);

        if (sort != null) {
            map.setSortSpec(sort);
        }

        return map;
    }

    private void addPractitionerSearchParameters(SearchParameterMap map) {

        /*
         * Passing the complete AndList parameter preserves:
         *
         * repeated parameters: ?given=John&given=Paul AND semantics
         *
         * comma-separated parameters: ?given=John,Paul OR semantics
         */

        if (name != null) {
            map.addParameter(FhirConstants.NAME_SEARCH_HANDLER, name);
        }

        if (given != null) {
            map.addParameter(FhirConstants.FIRST_NAME_SEARCH_HANDLER, given);
        }

        if (family != null) {
            map.addParameter(FhirConstants.LAST_NAME_SEARCH_HANDLER, family);
        }

        if (city != null) {
            map.addParameter(FhirConstants.CITY_SEARCH_HANDLER, city);
        }

        if (state != null) {
            map.addParameter(FhirConstants.STATE_SEARCH_HANDLER, state);
        }

        if (postalCode != null) {
            map.addParameter(FhirConstants.POSTALCODE_SEARCH_HANDLER, postalCode);
        }

        if (country != null) {
            map.addParameter(FhirConstants.COUNTRY_SEARCH_HANDLER, country);
        }

        if (telecom != null) {
            map.addParameter(FhirConstants.TELECOM_SEARCH_HANDLER, telecom);
        }

        if (email != null) {
            map.addParameter(FhirConstants.EMAIL_SEARCH_HANDLER, email);
        }

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

    public TokenAndListParam getTelecom() {
        return telecom;
    }

    public void setTelecom(TokenAndListParam telecom) {

        this.telecom = telecom;
    }

    public TokenAndListParam getEmail() {
        return email;
    }

    public void setEmail(TokenAndListParam email) {

        this.email = email;
    }

    public TokenAndListParam getPhone() {
        return phone;
    }

    public void setPhone(TokenAndListParam phone) {

        this.phone = phone;
    }

    public SortSpec getSort() {
        return sort;
    }

    public void setSort(SortSpec sort) {

        this.sort = sort;
    }

    public Set<Include> getRevIncludes() {
        return revIncludes;
    }

    public boolean hasRevInclude(String includeValue) {

        if (includeValue == null || includeValue.isBlank()) {
            return false;
        }

        return revIncludes.stream().map(Include::getValue).anyMatch(includeValue::equals);
    }
}