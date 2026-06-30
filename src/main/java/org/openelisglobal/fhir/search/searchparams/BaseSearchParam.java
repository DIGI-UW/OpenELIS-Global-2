package org.openelisglobal.fhir.search.searchparams;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.providers.FhirProviderUtils;

public abstract class BaseSearchParam implements Serializable {

    protected TokenAndListParam id;

    protected TokenAndListParam identifier;

    protected DateRangeParam lastUpdated;

    protected SortSpec sort;

    public BaseSearchParam(TokenAndListParam id, TokenAndListParam identifier, DateRangeParam lastUpdated) {
        this.id = id;
        this.identifier = identifier;
        this.lastUpdated = lastUpdated;
    }

    protected final SearchParameterMap baseSearchParameterMap() {
        return new SearchParameterMap()
                .addParameter(FhirConstants.ID_PROPERTY, FhirProviderUtils.uuidValueFromTokenAndListParam(getId()))
                .addParameter(FhirConstants.IDENTIFIER_SEARCH_HANDLER,
                        FhirProviderUtils.uuidValueFromTokenAndListParam(getIdentifier()))
                .addParameter(FhirConstants.LAST_UPDATED_PROPERTY, getLastUpdated());
    }

    public abstract SearchParameterMap toSearchParameterMap();

    public Map<String, Object> convertToFlatMap(SearchParameterMap searchMap) {
        Map<String, Object> flatMap = new LinkedHashMap<>();

        if (searchMap == null || searchMap.getParameters() == null) {
            return flatMap;
        }

        for (Map.Entry<String, List<PropParam<?>>> entry : searchMap.getParameters()) {
            String key = entry.getKey();
            List<PropParam<?>> paramList = entry.getValue();

            if (paramList != null && !paramList.isEmpty()) {
                PropParam<?> propParam = paramList.get(0);

                if (propParam != null && propParam.getParam() != null) {
                    flatMap.put(key, propParam.getParam());
                }
            }
        }

        return flatMap;
    }

    public TokenAndListParam getId() {
        return id;
    }

    public void setId(TokenAndListParam id) {
        this.id = id;
    }

    public DateRangeParam getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(DateRangeParam lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public SortSpec getSort() {
        return sort;
    }

    public void setSort(SortSpec sort) {
        this.sort = sort;
    }

    public TokenAndListParam getIdentifier() {
        return identifier;

    }

    public void setIdentifier(TokenAndListParam identifier) {
        this.identifier = identifier;
    }
}
