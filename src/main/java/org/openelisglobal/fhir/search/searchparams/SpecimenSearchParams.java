package org.openelisglobal.fhir.search.searchparams;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.r4.model.Specimen;

/**
 * Search parameters the Specimen facade answers from the OpenELIS database:
 * logical id, identifiers (sample item uuid, lab number), accession, patient,
 * sample type, status, collection date and last-updated timestamp.
 *
 * <p>
 * {@code patient} and {@code subject} are the same lookup and arrive merged.
 * The OpenELIS status ids behind the FHIR status codes are resolved by the
 * search service before the DAO runs.
 */
public class SpecimenSearchParams extends BaseSearchParam {

    private TokenAndListParam accession;

    private ReferenceAndListParam patient;

    private TokenAndListParam type;

    private TokenAndListParam status;

    private DateRangeParam collected;

    private String canceledStatusId;

    private String disposedStatusId;

    private final Set<Include> includes;

    private final Set<Include> revIncludes;

    public SpecimenSearchParams(TokenAndListParam id, TokenAndListParam identifier, TokenAndListParam accession,
            ReferenceAndListParam patient, TokenAndListParam type, TokenAndListParam status, DateRangeParam collected,
            DateRangeParam lastUpdated, SortSpec sort, Set<Include> includes, Set<Include> revIncludes) {

        super(id, identifier, lastUpdated, sort);

        this.accession = accession;
        this.patient = patient;
        this.type = type;
        this.status = status;
        this.collected = collected;
        this.includes = includes == null ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(includes));
        this.revIncludes = revIncludes == null ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(revIncludes));
    }

    @Override
    public SearchParameterMap toSearchParameterMap() {

        SearchParameterMap map = new SearchParameterMap();
        addBaseSearchParameters(map);

        if (accession != null) {
            map.addParameter(Specimen.SP_ACCESSION, accession);
        }
        if (patient != null) {
            map.addParameter(Specimen.SP_PATIENT, patient);
        }
        if (type != null) {
            map.addParameter(Specimen.SP_TYPE, type);
        }
        if (status != null) {
            map.addParameter(Specimen.SP_STATUS, status);
        }
        if (collected != null) {
            map.addParameter(Specimen.SP_COLLECTED, collected);
        }
        if (getSort() != null) {
            map.setSortSpec(getSort());
        }
        return map;
    }

    public TokenAndListParam getAccession() {
        return accession;
    }

    public ReferenceAndListParam getPatient() {
        return patient;
    }

    public TokenAndListParam getType() {
        return type;
    }

    public TokenAndListParam getStatus() {
        return status;
    }

    public DateRangeParam getCollected() {
        return collected;
    }

    public String getCanceledStatusId() {
        return canceledStatusId;
    }

    public void setCanceledStatusId(String canceledStatusId) {
        this.canceledStatusId = canceledStatusId;
    }

    public String getDisposedStatusId() {
        return disposedStatusId;
    }

    public void setDisposedStatusId(String disposedStatusId) {
        this.disposedStatusId = disposedStatusId;
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
