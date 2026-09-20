package org.openelisglobal.fhir.search.searchparams;

import java.util.List;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface SearchQueryInclude<U extends IBaseResource> {

    Set<IBaseResource> getIncludedResources(List<U> resourceList, SearchParameterMap theParams);
}