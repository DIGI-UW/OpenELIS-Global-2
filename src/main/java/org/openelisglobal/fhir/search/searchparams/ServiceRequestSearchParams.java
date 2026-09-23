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
import org.hl7.fhir.r4.model.ServiceRequest;

/**
 * Search parameters the ServiceRequest facade answers from the OpenELIS
 * database: logical id, identifiers (analysis uuid, lab number), patient,
 * requester, specimen, test code, status and last-updated timestamp.
 *
 * <p>
 * {@code patient} and {@code subject} arrive merged. The OpenELIS analysis
 * status ids behind each FHIR status code are supplied by the search service.
 */
public class ServiceRequestSearchParams extends BaseSearchParam {

    private ReferenceAndListParam patient;

    private ReferenceAndListParam requester;

    private ReferenceAndListParam specimen;

    private TokenAndListParam code;

    private TokenAndListParam status;

    private Map<String, List<String>> statusIdsByCode = Collections.emptyMap();

    private final Set<Include> includes;

    private final Set<Include> revIncludes;

    public ServiceRequestSearchParams(TokenAndListParam id, TokenAndListParam identifier, ReferenceAndListParam patient,
            ReferenceAndListParam requester, ReferenceAndListParam specimen, TokenAndListParam code,
            TokenAndListParam status, DateRangeParam lastUpdated, SortSpec sort, Set<Include> includes,
            Set<Include> revIncludes) {

        super(id, identifier, lastUpdated, sort);

        this.patient = patient;
        this.requester = requester;
        this.specimen = specimen;
        this.code = code;
        this.status = status;
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
            map.addParameter(ServiceRequest.SP_PATIENT, patient);
        }
        if (requester != null) {
            map.addParameter(ServiceRequest.SP_REQUESTER, requester);
        }
        if (specimen != null) {
            map.addParameter(ServiceRequest.SP_SPECIMEN, specimen);
        }
        if (code != null) {
            map.addParameter(ServiceRequest.SP_CODE, code);
        }
        if (status != null) {
            map.addParameter(ServiceRequest.SP_STATUS, status);
        }
        if (getSort() != null) {
            map.setSortSpec(getSort());
        }
        return map;
    }

    public ReferenceAndListParam getPatient() {
        return patient;
    }

    public ReferenceAndListParam getRequester() {
        return requester;
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

    /**
     * FHIR status code (lower case) to the OpenELIS analysis status ids it covers.
     */
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
