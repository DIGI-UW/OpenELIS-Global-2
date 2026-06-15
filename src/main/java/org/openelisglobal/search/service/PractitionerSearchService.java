package org.openelisglobal.search.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.search.searchparams.PractitionerSearchParams;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PractitionerSearchService {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private FhirTransformService fhirTransformService;

    public IBundleProvider searchPractitioners(PractitionerSearchParams params) {

        Map<String, Object> flatMap = params.convertToFlatMap(params.toSearchParameterMap());

        List<Provider> providers;

        if (flatMap == null || flatMap.isEmpty()) {
            providers = providerService.getAll();
        } else {
            providers = providerService.getAllMatching(flatMap);
        }

        List<IBaseResource> resources = new ArrayList<>();

        for (Provider provider : providers) {
            resources.add(fhirTransformService.transformProviderToPractitioner(provider));
        }

        return new SimpleBundleProvider(resources);
    }
}