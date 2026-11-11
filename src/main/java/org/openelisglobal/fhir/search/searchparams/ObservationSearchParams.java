package org.openelisglobal.fhir.search.searchparams;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.Observation;

/**
 * Search parameters the Observation facade answers from the OpenELIS database:
 * logical id, patient, order ({@code based-on}), specimen, test code, status,
 * effective date and last-updated timestamp.
 *
 * <p>
 * {@code patient} and {@code subject} arrive merged. The OpenELIS analysis
 * status ids behind each FHIR status code are supplied by the search service.
 */
public class ObservationSearchParams extends BaseSearchParam {

    private ReferenceAndListParam patient;

    private ReferenceAndListParam basedOn;

    private ReferenceAndListParam specimen;

    private TokenAndListParam code;

    private TokenAndListParam status;

    private DateRangeParam date;

    private Map<String, List<String>> statusIdsByCode = Collections.emptyMap();

    private final Set<Include> includes;

    private final Set<Include> revIncludes;

    public ObservationSearchParams(TokenAndListParam id, TokenAndListParam identifier, ReferenceAndListParam patient,
            ReferenceAndListParam basedOn, ReferenceAndListParam specimen, TokenAndListParam code,
            TokenAndListParam status, DateRangeParam date, DateRangeParam lastUpdated, SortSpec sort,
            Set<Include> includes, Set<Include> revIncludes) {

        super(id, identifier, lastUpdated, sort);

        this.patient = patient;
        this.basedOn = basedOn;
        this.specimen = specimen;
        this.code = code;
        this.status = status;
        this.date = date;
        this.includes = includes == null ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(includes));
        this.revIncludes = revIncludes == null ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(revIncludes));
    }

    @Override
    public SearchParameterMap toSearchParameterMap() {

        SearchParameterMap map = new SearchParameterMap();
        addBaseSearchParameters(map);

        if (patient != null) {
            map.addParameter(Observation.SP_PATIENT, patient);
        }
        if (basedOn != null) {
            map.addParameter(Observation.SP_BASED_ON, basedOn);
        }
        if (specimen != null) {
            map.addParameter(Observation.SP_SPECIMEN, specimen);
        }
        if (code != null) {
            map.addParameter(Observation.SP_CODE, code);
        }
        if (status != null) {
            map.addParameter(Observation.SP_STATUS, status);
        }
        if (date != null) {
            map.addParameter(Observation.SP_DATE, date);
        }
        if (getSort() != null) {
            map.setSortSpec(getSort());
        }
        return map;
    }

    public ReferenceAndListParam getPatient() {
        return patient;
    }

    public ReferenceAndListParam getBasedOn() {
        return basedOn;
    }

    public ReferenceAndListParam getSpecimen() {
        return specimen;
    }

    public TokenAndListParam getCode() {
        return code;
    }

    public TokenAndListParam getStatus() {
        return status;
    }

    public DateRangeParam getDate() {
        return date;
    }

    public Map<String, List<String>> getStatusIdsByCode() {
        return statusIdsByCode;
    }

    public void setStatusIdsByCode(Map<String, List<String>> statusIdsByCode) {
        this.statusIdsByCode = statusIdsByCode == null ? Collections.emptyMap() : statusIdsByCode;
    }

    public boolean hasInclude(String... includeValues) {
        return matchesAny(includes, includeValues);
    }

    public boolean hasRevInclude(String... includeValues) {
        return matchesAny(revIncludes, includeValues);
    }

    private static boolean matchesAny(Set<Include> candidates, String... includeValues) {
        for (Include include : candidates) {
            for (String value : includeValues) {
                if (value != null && value.equals(include.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }
}
