package org.openelisglobal.fhir.search.searchparams;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.r4.model.Patient;

/**
 * Search parameters the Patient facade answers from the OpenELIS database.
 *
 * <p>
 * Only parameters with an OpenELIS counterpart are accepted: the logical id and
 * business identifiers (subject number, national id, ST number, GUID), names,
 * birth date, gender and the last-updated timestamp.
 */
public class PatientSearchParams extends BaseSearchParam {

    private StringAndListParam name;

    private StringAndListParam given;

    private StringAndListParam family;

    private DateRangeParam birthDate;

    private TokenAndListParam gender;

    private final Set<Include> revIncludes;

    public PatientSearchParams(TokenAndListParam id, TokenAndListParam identifier, StringAndListParam name,
            StringAndListParam given, StringAndListParam family, DateRangeParam birthDate, TokenAndListParam gender,
            DateRangeParam lastUpdated, SortSpec sort, Set<Include> revIncludes) {

        super(id, identifier, lastUpdated, sort);

        this.name = name;
        this.given = given;
        this.family = family;
        this.birthDate = birthDate;
        this.gender = gender;
        this.revIncludes = revIncludes == null ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(revIncludes));
    }

    @Override
    public SearchParameterMap toSearchParameterMap() {

        SearchParameterMap map = new SearchParameterMap();

        addBaseSearchParameters(map);

        if (name != null) {
            map.addParameter(Patient.SP_NAME, name);
        }
        if (given != null) {
            map.addParameter(Patient.SP_GIVEN, given);
        }
        if (family != null) {
            map.addParameter(Patient.SP_FAMILY, family);
        }
        if (birthDate != null) {
            map.addParameter(Patient.SP_BIRTHDATE, birthDate);
        }
        if (gender != null) {
            map.addParameter(Patient.SP_GENDER, gender);
        }
        if (getSort() != null) {
            map.setSortSpec(getSort());
        }

        return map;
    }

    public StringAndListParam getName() {
        return name;
    }

    public StringAndListParam getGiven() {
        return given;
    }

    public StringAndListParam getFamily() {
        return family;
    }

    public DateRangeParam getBirthDate() {
        return birthDate;
    }

    public TokenAndListParam getGender() {
        return gender;
    }

    public Set<Include> getRevIncludes() {
        return revIncludes;
    }

    /**
     * True when any of the given {@code Resource:param} values was requested via
     * {@code _revinclude}.
     */
    public boolean hasRevInclude(String... includeValues) {

        for (Include include : revIncludes) {
            for (String candidate : includeValues) {
                if (candidate != null && candidate.equals(include.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }
}
