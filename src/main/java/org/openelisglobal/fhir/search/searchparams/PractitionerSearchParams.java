package org.openelisglobal.fhir.search.searchparams;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PractitionerSearchParams {

    private TokenAndListParam identifier;
    private StringAndListParam given;
    private StringAndListParam family;
    private TokenAndListParam id;
    private DateRangeParam lastUpdated;
    private Integer pageStart;
    private Integer pageSize;
    private Boolean activeOnly;

    public boolean hasIdentifier() {
        return identifier != null && !identifier.getValuesAsQueryTokens().isEmpty();
    }

    public boolean hasName() {
        return (given != null && !given.getValuesAsQueryTokens().isEmpty())
                || (family != null && !family.getValuesAsQueryTokens().isEmpty());
    }

    public boolean hasLastUpdated() {
        return lastUpdated != null && (lastUpdated.getLowerBound() != null || lastUpdated.getUpperBound() != null);
    }

    public boolean hasId() {
        return id != null && !id.getValuesAsQueryTokens().isEmpty();
    }

    public boolean isActiveOnly() {
        return activeOnly != null ? activeOnly : true;
    }
}