package org.openelisglobal.fhir.search.searchparams;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.r4.model.Organization;

/**
 * Search parameters the Organization facade answers from the OpenELIS database:
 * logical id, business identifiers (code, short name, CLIA number, uuid), name,
 * active flag, organization type, parent organization and address parts.
 */
public class OrganizationSearchParams extends BaseSearchParam {

    private StringAndListParam name;

    private TokenAndListParam active;

    private TokenAndListParam type;

    private ReferenceAndListParam partOf;

    private StringAndListParam city;

    private StringAndListParam state;

    private final Set<Include> includes;

    private final Set<Include> revIncludes;

    public OrganizationSearchParams(TokenAndListParam id, TokenAndListParam identifier, StringAndListParam name,
            TokenAndListParam active, TokenAndListParam type, ReferenceAndListParam partOf, StringAndListParam city,
            StringAndListParam state, DateRangeParam lastUpdated, SortSpec sort, Set<Include> includes,
            Set<Include> revIncludes) {

        super(id, identifier, lastUpdated, sort);

        this.name = name;
        this.active = active;
        this.type = type;
        this.partOf = partOf;
        this.city = city;
        this.state = state;
        this.includes = includes == null ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(includes));
        this.revIncludes = revIncludes == null ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(revIncludes));
    }

    @Override
    public SearchParameterMap toSearchParameterMap() {

        SearchParameterMap map = new SearchParameterMap();
        addBaseSearchParameters(map);

        if (name != null) {
            map.addParameter(Organization.SP_NAME, name);
        }
        if (active != null) {
            map.addParameter(Organization.SP_ACTIVE, active);
        }
        if (type != null) {
            map.addParameter(Organization.SP_TYPE, type);
        }
        if (partOf != null) {
            map.addParameter(Organization.SP_PARTOF, partOf);
        }
        if (city != null) {
            map.addParameter(Organization.SP_ADDRESS_CITY, city);
        }
        if (state != null) {
            map.addParameter(Organization.SP_ADDRESS_STATE, state);
        }
        if (getSort() != null) {
            map.setSortSpec(getSort());
        }
        return map;
    }

    public StringAndListParam getName() {
        return name;
    }

    public TokenAndListParam getActive() {
        return active;
    }

    public TokenAndListParam getType() {
        return type;
    }

    public ReferenceAndListParam getPartOf() {
        return partOf;
    }

    public StringAndListParam getCity() {
        return city;
    }

    public StringAndListParam getState() {
        return state;
    }

    public boolean hasInclude(String includeValue) {
        return includes.stream().map(Include::getValue).anyMatch(includeValue::equals);
    }

    public boolean hasRevInclude(String includeValue) {
        return revIncludes.stream().map(Include::getValue).anyMatch(includeValue::equals);
    }
}
