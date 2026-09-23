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
import org.hl7.fhir.r4.model.Location;

/**
 * Search parameters the Location facade answers over the storage hierarchy:
 * logical id, identifier (storage code), name, status, parent and the
 * storage-hierarchy tag (room, device, shelf, rack, box).
 */
public class LocationSearchParams extends BaseSearchParam {

    private StringAndListParam name;

    private TokenAndListParam status;

    private ReferenceAndListParam partOf;

    private TokenAndListParam tag;

    private final Set<Include> includes;

    private final Set<Include> revIncludes;

    public LocationSearchParams(TokenAndListParam id, TokenAndListParam identifier, StringAndListParam name,
            TokenAndListParam status, ReferenceAndListParam partOf, TokenAndListParam tag, DateRangeParam lastUpdated,
            SortSpec sort, Set<Include> includes, Set<Include> revIncludes) {

        super(id, identifier, lastUpdated, sort);

        this.name = name;
        this.status = status;
        this.partOf = partOf;
        this.tag = tag;
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
            map.addParameter(Location.SP_NAME, name);
        }
        if (status != null) {
            map.addParameter(Location.SP_STATUS, status);
        }
        if (partOf != null) {
            map.addParameter(Location.SP_PARTOF, partOf);
        }
        if (tag != null) {
            map.addParameter("_tag", tag);
        }
        if (getSort() != null) {
            map.setSortSpec(getSort());
        }
        return map;
    }

    public StringAndListParam getName() {
        return name;
    }

    public TokenAndListParam getStatus() {
        return status;
    }

    public ReferenceAndListParam getPartOf() {
        return partOf;
    }

    public TokenAndListParam getTag() {
        return tag;
    }

    public boolean hasInclude(String includeValue) {
        return includes.stream().map(Include::getValue).anyMatch(includeValue::equals);
    }

    public boolean hasRevInclude(String includeValue) {
        return revIncludes.stream().map(Include::getValue).anyMatch(includeValue::equals);
    }
}
