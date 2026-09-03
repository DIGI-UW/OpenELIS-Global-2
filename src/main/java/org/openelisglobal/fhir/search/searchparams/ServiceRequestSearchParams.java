package org.openelisglobal.fhir.search.searchparams;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;

public class ServiceRequestSearchParams extends BaseSearchParam {

    protected ServiceRequestSearchParams(TokenAndListParam id, TokenAndListParam identifier,
            DateRangeParam lastUpdated) {
        super(id, identifier, lastUpdated);

    }

    @Override
    public SearchParameterMap toSearchParameterMap() {
        SearchParameterMap map = new SearchParameterMap();
        addBaseSearchParameters(map);
        return map;
    }

}
